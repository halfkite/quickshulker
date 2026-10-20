from __future__ import annotations

import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from build_targets import BuildTargetError, resolve_targets, validate_release_tag
from package_fabric_build import (
    ArtifactError,
    expected_filename,
    stage_build,
    verify_release_package,
)


def make_test_repo(root: Path) -> None:
    config = {
        "primaryMinecraftVersion": "26.3",
        "defaultLoader": "fabric",
        "targets": {
            "26.3-fabric": {"project": "versions/26.3-fabric", "java": 25},
            "1.21.1-fabric": {
                "project": ".",
                "java": 21,
                "maintenanceOnly": True,
            },
            "26.3-neoforge": {"project": "versions/26.3-neoforge", "java": 25},
        },
    }
    (root / "development.json").write_text(json.dumps(config), encoding="utf-8")
    projects = {
        "versions/26.3-fabric": ("26.3", 25),
        ".": ("1.21.1", 21),
    }
    for relative, (minecraft, _) in projects.items():
        project = root / relative
        project.mkdir(parents=True, exist_ok=True)
        (project / "gradle.properties").write_text(
            "\n".join(
                (
                    f"minecraft_version={minecraft}",
                    "mod_version=1.0.0",
                    "archives_base_name=quickshulkermore",
                )
            )
            + "\n",
            encoding="utf-8",
        )
        (project / "gradlew").write_text("# test wrapper\n", encoding="utf-8")


def make_fabric_jar(path: Path, version: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr(
            "fabric.mod.json",
            json.dumps({"id": "quickshulker", "version": version}),
        )


class BuildTargetTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        make_test_repo(self.root)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_all_fabric_matrix_prioritizes_primary_and_excludes_neoforge(self) -> None:
        targets = resolve_targets(self.root)
        self.assertEqual(targets[0]["target"], "26.3-fabric")
        self.assertIn("1.21.1-fabric", [target["target"] for target in targets])
        self.assertNotIn("26.3-neoforge", [target["target"] for target in targets])

    def test_manual_selection_accepts_only_exact_fabric_target_ids(self) -> None:
        targets = resolve_targets(self.root, "1.21.1-fabric")
        self.assertEqual([target["target"] for target in targets], ["1.21.1-fabric"])
        with self.assertRaises(BuildTargetError):
            resolve_targets(self.root, "26.3-neoforge")
        with self.assertRaises(BuildTargetError):
            resolve_targets(self.root, "26.3-fabric,26.3-fabric")

    def test_release_tag_must_match_mod_version_in_every_fabric_target(self) -> None:
        version, targets = validate_release_tag(self.root, "v1.0.0")
        self.assertEqual(version, "1.0.0")
        self.assertEqual(targets[0], "26.3-fabric")
        with self.assertRaises(BuildTargetError):
            validate_release_tag(self.root, "v2.0.0")


class ArtifactPackageTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        make_test_repo(self.root)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_stages_standard_name_sha_and_manifest(self) -> None:
        source = self.root / "versions/26.3-fabric/build/libs/quickshulkermore-test.jar"
        make_fabric_jar(source, "1.0.0-26.3")
        output = self.root / "staged"
        artifact = stage_build(
            self.root,
            "26.3-fabric",
            output,
            "abc123",
            workflow="Build QuickShulkerMore Fabric",
            run_id="123",
            run_attempt="1",
            build_command="./gradlew build",
        )
        self.assertEqual(
            artifact.name,
            "quickshulkermore-1.0.0-mc26.3-fabric.jar",
        )
        expected_hash = hashlib.sha256(artifact.read_bytes()).hexdigest()
        sidecar = output / f"{artifact.name}.sha256"
        self.assertTrue(sidecar.read_text(encoding="ascii").startswith(expected_hash))
        manifest = json.loads(
            (output / "build-manifest-26.3-fabric.json").read_text(encoding="utf-8")
        )
        self.assertEqual(manifest["commit"], "abc123")
        self.assertEqual(manifest["sha256"], expected_hash)

    def test_release_verifier_requires_complete_matching_artifact_set(self) -> None:
        staged = self.root / "release-assets"
        for target in ("26.3-fabric", "1.21.1-fabric"):
            targets = resolve_targets(self.root, target)
            properties_path = Path(targets[0]["project"]) / "gradle.properties"
            if not properties_path.is_absolute():
                properties_path = self.root / properties_path
            properties = {}
            for line in properties_path.read_text(encoding="utf-8").splitlines():
                if "=" in line and not line.startswith("#"):
                    key, value = line.split("=", 1)
                    properties[key.strip()] = value.strip()
            filename = expected_filename(properties)
            source = staged / filename
            make_fabric_jar(source, f"1.0.0-{properties['minecraft_version']}")
            digest = hashlib.sha256(source.read_bytes()).hexdigest()
            (staged / f"{filename}.sha256").write_text(
                f"{digest}  {filename}\n", encoding="ascii"
            )
            manifest = {
                "target": target,
                "artifact": filename,
                "sha256": digest,
                "commit": "release-sha",
            }
            (staged / f"build-manifest-{target}.json").write_text(
                json.dumps(manifest), encoding="utf-8"
            )

        release_manifest = verify_release_package(
            self.root,
            staged,
            ["26.3-fabric", "1.21.1-fabric"],
            "1.0.0",
            "release-sha",
        )
        data = json.loads(release_manifest.read_text(encoding="utf-8"))
        self.assertEqual(data["version"], "1.0.0")
        self.assertEqual(len(data["artifacts"]), 2)

    def test_release_verifier_rejects_a_corrupt_checksum(self) -> None:
        staged = self.root / "release-assets"
        target = "26.3-fabric"
        targets = resolve_targets(self.root, target)
        props_path = self.root / targets[0]["project"] / "gradle.properties"
        props = {}
        for line in props_path.read_text(encoding="utf-8").splitlines():
            if "=" in line:
                key, value = line.split("=", 1)
                props[key.strip()] = value.strip()
        filename = expected_filename(props)
        make_fabric_jar(staged / filename, "1.0.0-26.3")
        (staged / f"{filename}.sha256").write_text(f"{'0' * 64}  {filename}\n", encoding="ascii")
        (staged / f"build-manifest-{target}.json").write_text(
            json.dumps(
                {
                    "target": target,
                    "artifact": filename,
                    "sha256": "0" * 64,
                    "commit": "release-sha",
                }
            ),
            encoding="utf-8",
        )
        with self.assertRaises(ArtifactError):
            verify_release_package(
                self.root, staged, [target], "1.0.0", "release-sha"
            )


if __name__ == "__main__":
    unittest.main()
