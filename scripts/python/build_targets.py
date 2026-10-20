#!/usr/bin/env python3
"""Resolve the documented Fabric build matrix from development.json."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


class BuildTargetError(ValueError):
    pass


def read_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith(("#", "!")) or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def compatibility_groups(config: dict[str, Any], available: list[str]) -> dict[str, tuple[str, list[str]]]:
    """Map exact runtime targets to one validated, contiguous publication group."""
    result: dict[str, tuple[str, list[str]]] = {}
    groups = config.get("fabricCompatibilityGroups", [])
    if not isinstance(groups, list):
        raise BuildTargetError("fabricCompatibilityGroups must be an array")
    for group in groups:
        if not isinstance(group, dict):
            raise BuildTargetError("Each compatibility group must be an object")
        anchor = group.get("buildTarget")
        games = group.get("minecraftVersions")
        if anchor not in available or not isinstance(games, list) or len(games) < 2:
            raise BuildTargetError(f"Invalid compatibility group: {group!r}")
        if not all(isinstance(game, str) and re.fullmatch(r"\d+\.\d+(?:\.\d+)?", game) for game in games):
            raise BuildTargetError(f"Invalid compatibility versions: {games!r}")
        parts = [tuple(map(int, game.split("."))) for game in games]
        patches = [part[2] if len(part) == 3 else 0 for part in parts]
        if (any(part[:2] != parts[0][:2] for part in parts)
                or any(patch != patches[0] + index for index, patch in enumerate(patches))):
            raise BuildTargetError(f"Compatibility versions must be contiguous and ordered: {games!r}")
        members = [f"{game}-fabric" for game in games]
        if anchor not in members or any(member not in available for member in members):
            raise BuildTargetError(f"Compatibility group contains missing targets: {group!r}")
        for member in members:
            if member in result:
                raise BuildTargetError(f"Overlapping compatibility groups for {member}")
            result[member] = (anchor, games)
    return result


def _load_targets(repo_root: Path) -> tuple[dict[str, Any], list[str]]:
    root = repo_root.resolve()
    try:
        config = json.loads((root / "development.json").read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise BuildTargetError(f"Cannot read development.json: {exc}") from exc

    targets = config.get("targets")
    if not isinstance(targets, dict):
        raise BuildTargetError("development.json must contain a targets object")

    loader = config.get("defaultLoader", "fabric")
    if loader != "fabric":
        raise BuildTargetError(f"This workflow supports Fabric only, found defaultLoader={loader!r}")

    primary = config.get("primaryMinecraftVersion")
    primary_key = f"{primary}-{loader}"
    fabric_keys = [key for key in targets if key.endswith(f"-{loader}")]
    if primary_key not in fabric_keys:
        raise BuildTargetError(f"Primary Fabric target {primary_key!r} is missing")
    ordered_keys = [primary_key, *(key for key in fabric_keys if key != primary_key)]

    for key in ordered_keys:
        entry = targets[key]
        if not isinstance(entry, dict):
            raise BuildTargetError(f"Target {key!r} must be an object")
        project_value = entry.get("project")
        java = entry.get("java")
        if not isinstance(project_value, str) or not project_value:
            raise BuildTargetError(f"Target {key!r} has no project path")
        if not isinstance(java, int) or java < 21:
            raise BuildTargetError(f"Target {key!r} has an invalid Java version")

        project = (root / project_value).resolve()
        try:
            project.relative_to(root)
        except ValueError as exc:
            raise BuildTargetError(f"Target {key!r} escapes the repository root") from exc
        properties_path = project / "gradle.properties"
        if not properties_path.is_file():
            raise BuildTargetError(f"Target {key!r} is missing gradle.properties")
        if not ((project / "gradlew").is_file() or (project / "gradlew.bat").is_file()):
            raise BuildTargetError(f"Target {key!r} is missing its Gradle wrapper")

        properties = read_properties(properties_path)
        minecraft = properties.get("minecraft_version")
        if not minecraft or key != f"{minecraft}-{loader}":
            raise BuildTargetError(
                f"Target key {key!r} does not match minecraft_version={minecraft!r}"
            )

    compatibility_groups(config, ordered_keys)
    return config, ordered_keys


def resolve_targets(repo_root: Path, selector: str = "all-fabric", *, grouped: bool = True) -> list[dict[str, Any]]:
    config, available = _load_targets(repo_root)
    by_key = config["targets"]
    groups = compatibility_groups(config, available)
    if selector.strip() in {"all", "all-fabric"}:
        selected = available
    else:
        requested = [part.strip() for part in selector.split(",")]
        if not requested or any(not part for part in requested):
            raise BuildTargetError("Target selection contains an empty entry")
        if len(requested) != len(set(requested)):
            raise BuildTargetError("Target selection contains duplicates")
        invalid = [key for key in requested if key not in available]
        if invalid:
            raise BuildTargetError(
                "Only exact Fabric target IDs are accepted; unsupported target(s): "
                + ", ".join(invalid)
            )
        selected = requested

    if grouped:
        selected = list(dict.fromkeys(groups.get(key, (key, []))[0] for key in selected))
    resolved: list[dict[str, Any]] = []
    for key in selected:
        entry = by_key[key]
        project = Path(entry["project"]).as_posix()
        games = groups.get(key, (key, [key[:-len("-fabric")]]))[1]
        label = games[0] if len(games) == 1 else f"{games[0]}-{games[-1]}"
        predicate = games[0] if len(games) == 1 else f">={games[0]} <={games[-1]}"
        resolved.append(
            {
                "target": key,
                "minecraft": key[: -len("-fabric")],
                "loader": "fabric",
                "project": project,
                "java": entry["java"],
                "artifact_minecraft": label,
                "minecraft_dependency": predicate,
                "minecraft_versions": games,
                "build_tasks": "assemble validateAccessWidener printArtifactName" if len(games) > 1 else "build printArtifactName",
            }
        )
    return resolved


def resolve_compatibility_targets(repo_root: Path, selector: str = "all-fabric") -> list[dict[str, Any]]:
    runtimes = {item["target"]: item for item in resolve_targets(repo_root, grouped=False)}
    tests = []
    for artifact in resolve_targets(repo_root, selector):
        if len(artifact["minecraft_versions"]) == 1:
            continue
        for game in artifact["minecraft_versions"]:
            runtime = dict(runtimes[f"{game}-fabric"])
            runtime["artifact_target"] = artifact["target"]
            tests.append(runtime)
    return tests


def validate_release_tag(repo_root: Path, tag: str) -> tuple[str, list[str]]:
    normalized = tag[1:] if tag.startswith("v") else tag
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?", normalized):
        raise BuildTargetError(f"Release tag {tag!r} is not a supported version tag")

    targets = resolve_targets(repo_root, "all-fabric", grouped=False)
    mismatches: list[str] = []
    root = repo_root.resolve()
    for target in targets:
        project = Path(target["project"])
        if not project.is_absolute():
            project = root / project
        version = read_properties(project / "gradle.properties").get("mod_version")
        if version != normalized:
            mismatches.append(f"{target['target']} uses {version!r}")
    if mismatches:
        raise BuildTargetError(
            f"Release tag {tag!r} must match mod_version {normalized!r} in every Fabric target: "
            + "; ".join(mismatches)
        )
    return normalized, [target["target"] for target in resolve_targets(repo_root)]


def write_outputs(output_path: str | None, values: dict[str, str]) -> None:
    lines = [f"{key}={value}" for key, value in values.items()]
    if output_path:
        with open(output_path, "a", encoding="utf-8", newline="\n") as output:
            output.write("\n".join(lines) + "\n")
    else:
        print("\n".join(lines))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    matrix = subparsers.add_parser("matrix", help="create a GitHub Actions build matrix")
    matrix.add_argument("--repo-root", type=Path, default=Path.cwd())
    matrix.add_argument("--targets", default="all-fabric")
    matrix.add_argument("--github-output")

    compatibility = subparsers.add_parser("compatibility-matrix", help="test each grouped runtime using one shared binary")
    compatibility.add_argument("--repo-root", type=Path, default=Path.cwd())
    compatibility.add_argument("--targets", default="all-fabric")
    compatibility.add_argument("--github-output")

    release = subparsers.add_parser("validate-release", help="validate a release tag")
    release.add_argument("--repo-root", type=Path, default=Path.cwd())
    release.add_argument("--tag", required=True)
    release.add_argument("--github-output")

    args = parser.parse_args(argv)
    try:
        if args.command in {"matrix", "compatibility-matrix"}:
            targets = (resolve_targets if args.command == "matrix" else resolve_compatibility_targets)(args.repo_root, args.targets)
            matrix_value = json.dumps({"include": targets}, separators=(",", ":"))
            targets_value = json.dumps([item["target"] for item in targets], separators=(",", ":"))
            outputs = ({"compatibility_matrix": matrix_value, "has_compatibility": str(bool(targets)).lower()}
                       if args.command == "compatibility-matrix" else {"matrix": matrix_value, "targets": targets_value})
            write_outputs(args.github_output, outputs)
        else:
            version, targets = validate_release_tag(args.repo_root, args.tag)
            write_outputs(
                args.github_output,
                {
                    "tag": args.tag,
                    "version": version,
                    "targets": json.dumps(targets, separators=(",", ":")),
                },
            )
    except BuildTargetError as exc:
        print(f"Build target error: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
