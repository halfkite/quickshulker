#!/usr/bin/env python3
"""Stage and verify installable Fabric JARs for CI and GitHub Releases."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any

from build_targets import BuildTargetError, read_properties, resolve_targets


class ArtifactError(ValueError):
    pass


def load_target(repo_root: Path, target_key: str) -> tuple[dict[str, Any], dict[str, str]]:
    matches = [item for item in resolve_targets(repo_root, grouped=False) if item["target"] == target_key]
    if not matches:
        raise ArtifactError(f"Unknown Fabric target: {target_key}")
    target = matches[0]
    project = Path(target["project"])
    if not project.is_absolute():
        project = repo_root / project
    target["project_path"] = project.resolve()
    properties = read_properties(project / "gradle.properties")
    properties["artifact_mc_version"] = target["artifact_minecraft"]
    properties["minecraft_dependency"] = target["minecraft_dependency"]
    return target, properties


def installable_jar(path: Path) -> bool:
    lowered = path.name.lower()
    return path.is_file() and path.suffix.lower() == ".jar" and not any(
        lowered.endswith(suffix)
        for suffix in ("-sources.jar", "-dev.jar", "-dev-shadow.jar", "-javadoc.jar", "-tests.jar")
    )


def validate_fabric_jar(path: Path, properties: dict[str, str] | None = None) -> None:
    try:
        with zipfile.ZipFile(path) as archive:
            metadata = json.loads(archive.read("fabric.mod.json").decode("utf-8"))
    except (OSError, KeyError, zipfile.BadZipFile, json.JSONDecodeError) as exc:
        raise ArtifactError(f"Not a valid Fabric mod JAR ({path.name}): {exc}") from exc
    if not isinstance(metadata, dict):
        raise ArtifactError(f"JAR {path.name} has invalid mod metadata")
    if metadata.get("id") != "quickshulker":
        raise ArtifactError(
            f"JAR {path.name} has Fabric mod id {metadata.get('id')!r}, expected 'quickshulker'"
        )
    if properties is not None:
        expected_version = f"{properties['mod_version']}-{properties['minecraft_version']}"
        if metadata.get("version") != expected_version:
            raise ArtifactError(f"JAR {path.name} has version {metadata.get('version')!r}, expected {expected_version!r}")
        predicate = properties["minecraft_dependency"]
        dependencies = metadata.get("depends")
        if not isinstance(dependencies, dict) or dependencies.get("minecraft") != predicate:
            raise ArtifactError(f"JAR {path.name} must declare Minecraft {predicate!r}")


def expected_filename(properties: dict[str, str], loader: str = "fabric") -> str:
    required = ("archives_base_name", "mod_version", "minecraft_version")
    missing = [key for key in required if not properties.get(key)]
    if missing:
        raise ArtifactError(f"gradle.properties is missing: {', '.join(missing)}")
    return (
        f"{properties['archives_base_name']}-{properties['mod_version']}-"
        f"mc{properties.get('artifact_mc_version', properties['minecraft_version'])}-{loader}.jar"
    )


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def stage_build(
    repo_root: Path,
    target_key: str,
    output_dir: Path,
    commit: str,
    workflow: str = "",
    run_id: str = "",
    run_attempt: str = "",
    build_command: str = "",
) -> Path:
    target, properties = load_target(repo_root, target_key)
    build_dir = target["project_path"] / "build" / "libs"
    candidates = sorted(path for path in build_dir.glob("*.jar") if installable_jar(path))
    if len(candidates) != 1:
        raise ArtifactError(
            f"Expected exactly one installable JAR for {target_key}, found {len(candidates)}"
        )
    source = candidates[0]
    validate_fabric_jar(source, properties)

    output_dir.mkdir(parents=True, exist_ok=True)
    filename = expected_filename(properties)
    destination = output_dir / filename
    if source.resolve() != destination.resolve():
        shutil.copy2(source, destination)
    digest = sha256_file(destination)
    (output_dir / f"{filename}.sha256").write_text(
        f"{digest}  {filename}\n", encoding="ascii"
    )
    manifest = {
        "mod": properties["archives_base_name"],
        "target": target_key,
        "minecraft_version": properties["minecraft_version"],
        "minecraft_versions": target["minecraft_versions"],
        "minecraft_dependency": target["minecraft_dependency"],
        "loader": "fabric",
        "mod_version": properties["mod_version"],
        "artifact": filename,
        "sha256": digest,
        "commit": commit,
        "build_command": build_command,
        "workflow": workflow,
        "run_id": run_id,
        "run_attempt": run_attempt,
    }
    manifest_path = output_dir / f"build-manifest-{target_key}.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return destination


def verify_release_package(
    repo_root: Path,
    artifact_dir: Path,
    target_keys: list[str],
    version: str,
    commit: str,
) -> Path:
    if not target_keys:
        raise ArtifactError("Release target list is empty")
    expected_jars: set[str] = set()
    entries: list[dict[str, Any]] = []
    for key in target_keys:
        target, properties = load_target(repo_root, key)
        if properties.get("mod_version") != version:
            raise ArtifactError(
                f"{key} has mod_version={properties.get('mod_version')!r}, expected {version!r}"
            )
        filename = expected_filename(properties)
        if filename in expected_jars:
            raise ArtifactError(f"Duplicate release artifact filename: {filename}")
        expected_jars.add(filename)

        jar_path = artifact_dir / filename
        checksum_path = artifact_dir / f"{filename}.sha256"
        manifest_path = artifact_dir / f"build-manifest-{key}.json"
        for required in (jar_path, checksum_path, manifest_path):
            if not required.is_file():
                raise ArtifactError(f"Missing release artifact: {required.name}")
        validate_fabric_jar(jar_path, properties)

        digest = sha256_file(jar_path)
        checksum_parts = checksum_path.read_text(encoding="ascii").strip().split()
        if len(checksum_parts) != 2 or checksum_parts[0].lower() != digest:
            raise ArtifactError(f"SHA-256 sidecar does not match {filename}")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if manifest.get("artifact") != filename or manifest.get("sha256") != digest:
            raise ArtifactError(f"Build manifest does not match {filename}")
        if manifest.get("target") != key or manifest.get("commit") != commit:
            raise ArtifactError(f"Build manifest target or commit mismatch for {key}")
        entries.append(
            {
                "target": key,
                "minecraft_version": properties["minecraft_version"],
                "minecraft_versions": target["minecraft_versions"],
                "loader": "fabric",
                "artifact": filename,
                "sha256": digest,
            }
        )

    actual_jars = {path.name for path in artifact_dir.glob("*.jar")}
    if actual_jars != expected_jars:
        raise ArtifactError(
            f"Release JAR set mismatch; missing={sorted(expected_jars - actual_jars)}, "
            f"unexpected={sorted(actual_jars - expected_jars)}"
        )

    release_manifest = {
        "mod": "quickshulkermore",
        "version": version,
        "commit": commit,
        "artifacts": entries,
    }
    manifest_path = artifact_dir / "release-manifest.json"
    manifest_path.write_text(
        json.dumps(release_manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest_path


def verify_runtime_report(repo_root: Path, target_key: str, jar: Path, log: Path, world_report: Path) -> dict[str, Any]:
    anchor = resolve_targets(repo_root, target_key)[0]
    _, properties = load_target(repo_root, anchor["target"])
    validate_fabric_jar(jar, properties)
    digest = sha256_file(jar)
    output = log.read_text(encoding="utf-8-sig")
    if f"COMPATIBILITY_SHA256={digest}" not in output:
        raise ArtifactError("Runtime report did not verify the shared artifact SHA-256")
    if output.count("external artifact verified:") < 2 or "shared JAR client smoke passed:" not in output:
        raise ArtifactError("Missing external-artifact verification for server, world, or client tests")
    try:
        report = ET.parse(world_report).getroot()
    except ET.ParseError as exc:
        raise ArtifactError(f"Invalid world test report: {exc}") from exc
    cases = report.findall(".//testcase")
    if not cases or report.findall(".//failure") or report.findall(".//error"):
        raise ArtifactError("World test report is empty or contains failures")
    return {"target": target_key, "artifact_target": anchor["target"], "sha256": digest,
            "world_tests": len(cases), "client_smoke": "passed"}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    stage = subparsers.add_parser("stage", help="stage one successful Fabric build")
    stage.add_argument("--repo-root", type=Path, default=Path.cwd())
    stage.add_argument("--target", required=True)
    stage.add_argument("--output-dir", type=Path, required=True)
    stage.add_argument("--commit", required=True)
    stage.add_argument("--workflow", default="")
    stage.add_argument("--run-id", default="")
    stage.add_argument("--run-attempt", default="")
    stage.add_argument("--build-command", default="")

    verify = subparsers.add_parser("verify-release", help="validate all staged release artifacts")
    verify.add_argument("--repo-root", type=Path, default=Path.cwd())
    verify.add_argument("--artifact-dir", type=Path, required=True)
    verify.add_argument("--targets-json", required=True)
    verify.add_argument("--version", required=True)
    verify.add_argument("--commit", required=True)

    runtime = subparsers.add_parser("verify-runtime", help="require evidence that the shared binary passed every runtime phase")
    runtime.add_argument("--repo-root", type=Path, default=Path.cwd())
    runtime.add_argument("--target", required=True)
    runtime.add_argument("--jar", type=Path, required=True)
    runtime.add_argument("--log", type=Path, required=True)
    runtime.add_argument("--world-report", type=Path, required=True)

    args = parser.parse_args(argv)
    try:
        if args.command == "stage":
            artifact = stage_build(
                args.repo_root.resolve(),
                args.target,
                args.output_dir.resolve(),
                args.commit,
                args.workflow,
                args.run_id,
                args.run_attempt,
                args.build_command,
            )
            print(artifact)
        elif args.command == "verify-runtime":
            result = verify_runtime_report(args.repo_root, args.target, args.jar, args.log, args.world_report)
            print(json.dumps(result, ensure_ascii=False))
        else:
            targets = json.loads(args.targets_json)
            if not isinstance(targets, list) or not all(isinstance(item, str) for item in targets):
                raise ArtifactError("--targets-json must be a JSON array of target IDs")
            manifest = verify_release_package(
                args.repo_root.resolve(),
                args.artifact_dir.resolve(),
                targets,
                args.version,
                args.commit,
            )
            print(manifest)
    except (ArtifactError, BuildTargetError, OSError, json.JSONDecodeError) as exc:
        print(f"Artifact error: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
