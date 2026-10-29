"""Synthetic release artifacts for scripts/release_bundle.py, without Java."""

import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / ".tmp" / "release-bundle-tests"
SCRIPT = ROOT / "scripts" / "release_bundle.py"
PNG = b"\x89PNG\r\n\x1a\n" + b"synthetic screenshot"


class ReleaseBundleTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        shutil.rmtree(BASE, ignore_errors=True)
        BASE.mkdir(parents=True)

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(BASE, ignore_errors=True)

    def setUp(self):
        self.temp = TemporaryDirectory(dir=BASE)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.artifact = self.root / "artifact"
        self.artifact.mkdir()
        self.out = self.root / "dist"
        self.artifact_dir, self.release = self.make_artifact()
        self.design = self.root / "design.json"
        self.design.write_text(json.dumps({
            "DESCRIPTION": "Descripción breve de prueba.",
            "WARNINGS": "Advertencia adicional de prueba.",
            "TEAM": "Equipo de prueba.",
            "CREDITS": "Créditos externos de prueba.",
        }), encoding="utf-8")
        self.known_issues = self.root / "known-issues.txt"
        self.known_issues.write_text("Error conocido de prueba.", encoding="utf-8")

    def make_artifact(self, video=True):
        jar_name = "unallocated-0.2.0.jar"
        jar = self.artifact / jar_name
        jar.write_bytes(b"synthetic player jar")
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        (self.artifact / (jar_name + ".sha256")).write_text(
            f"{digest}  {jar_name}\n", encoding="utf-8")
        world_zip = self.artifact / "UNALLOCATED-world.zip"
        with ZipFile(world_zip, "w", ZIP_DEFLATED) as archive:
            archive.writestr("UNALLOCATED/level.dat", b"level")
        screenshots = self.artifact / "screenshots"
        screenshots.mkdir()
        (screenshots / "unallocated-title-button.png").write_bytes(PNG)
        (screenshots / "story-01.png").write_bytes(PNG)
        if video:
            video_dir = self.artifact / "video"
            video_dir.mkdir()
            (video_dir / "gameplay.mp4").write_bytes(b"synthetic video")
        release = {
            "version": "0.2.0",
            "minecraft": "26.3",
            "loader": "0.20.1",
            "fabric_api_bundled": "0.165.0+26.3",
            "jar": jar_name,
            "sha256": digest,
            "world_zip": "UNALLOCATED-world.zip",
            "commit": "0123456789abcdef",
            "run": "1234567890",
        }
        (self.artifact / "release.json").write_text(
            json.dumps(release), encoding="utf-8")
        return self.artifact, release

    def run_bundle(self, *extra):
        return subprocess.run(
            [sys.executable, str(SCRIPT), str(self.artifact_dir),
             "--out", str(self.out), *map(str, extra)],
            capture_output=True, text=True, cwd=ROOT)

    def test_complete_artifact_produces_bundle(self):
        result = self.run_bundle("--design", self.design,
                                 "--known-issues", self.known_issues)
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertTrue((self.out / "unallocated-0.2.0.jar").is_file())
        self.assertTrue((self.out / "unallocated-0.2.0.jar.sha256").is_file())
        self.assertTrue((self.out / "UNALLOCATED-world.zip").is_file())
        self.assertTrue((self.out / "screenshots" / "story-01.png").is_file())
        self.assertTrue((self.out / "gameplay.mp4").is_file())
        page = (self.out / "itch-page.md").read_text(encoding="utf-8")
        self.assertNotIn("{{", page)
        self.assertIn("26.3", page)
        self.assertIn("unallocated-0.2.0.jar", page)
        self.assertIn("Descripción breve de prueba.", page)

    def test_missing_design_leaves_tokens_and_exits_two(self):
        result = self.run_bundle("--known-issues", self.known_issues)
        self.assertEqual(result.returncode, 2, result.stderr + result.stdout)
        self.assertIn("DESIGN:DESCRIPTION", result.stdout)
        self.assertIn("DESIGN:TEAM", result.stdout)
        page = (self.out / "itch-page.md").read_text(encoding="utf-8")
        self.assertIn("{{DESIGN:DESCRIPTION}}", page)

    def test_hash_mismatch_exits_one(self):
        release = dict(self.release, sha256="0" * 64)
        (self.artifact_dir / "release.json").write_text(
            json.dumps(release), encoding="utf-8")
        result = self.run_bundle("--design", self.design,
                                 "--known-issues", self.known_issues)
        self.assertEqual(result.returncode, 1, result.stderr + result.stdout)
        self.assertIn("SHA-256", result.stderr)

    def test_missing_jar_exits_one(self):
        (self.artifact_dir / self.release["jar"]).unlink()
        result = self.run_bundle("--design", self.design,
                                 "--known-issues", self.known_issues)
        self.assertEqual(result.returncode, 1, result.stderr + result.stdout)
        self.assertIn("missing JAR", result.stderr)

    def test_invalid_png_exits_one(self):
        (self.artifact_dir / "screenshots" / "story-01.png").write_bytes(
            b"not a png")
        result = self.run_bundle("--design", self.design,
                                 "--known-issues", self.known_issues)
        self.assertEqual(result.returncode, 1, result.stderr + result.stdout)
        self.assertIn("invalid PNG", result.stderr)

    def test_missing_video_still_succeeds(self):
        shutil.rmtree(self.artifact_dir / "video")
        result = self.run_bundle("--design", self.design,
                                 "--known-issues", self.known_issues)
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertFalse((self.out / "gameplay.mp4").exists())


if __name__ == "__main__":
    unittest.main()
