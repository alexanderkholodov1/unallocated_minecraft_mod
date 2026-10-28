"""Tests for story validator (scripts/story_check.py) and fixtures."""

import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
from story_check import check, Finding

REPO_ROOT = Path(__file__).resolve().parent.parent
FIXTURES_DIR = REPO_ROOT / "src/test/resources/story-fixtures"
VALID_EXAMPLE_DIR = FIXTURES_DIR / "valid/example"
INVALID_DIR = FIXTURES_DIR / "invalid"

ALL_ERROR_RULES = {
    "unknown-key",
    "bad-id",
    "duplicate-id",
    "missing-chapter-file",
    "chapter-id-mismatch",
    "too-many-chapters",
    "missing-language",
    "unknown-placeholder",
    "unknown-anchor",
    "unknown-zone",
    "unknown-beat-ref",
    "unknown-ending",
    "ending-unreachable",
    "flag-never-set",
    "bad-command",
    "unknown-function",
    "bad-time",
    "unknown-trigger",
    "unknown-action",
    "chat-without-input",
    "duplicate-capture",
    "autoplay-ending-coverage",
    "feature-off",
    "bad-value",
}

OUTPUT_LINE_RE = re.compile(r"^.+#[^:]*: (error|warning): \[[a-z0-9_-]+\] .+$")
SUMMARY_LINE_RE = re.compile(r"^story_check: (\d+) errors, (\d+) warnings$")


class TestStoryCheck(unittest.TestCase):
    def test_valid_example_yields_zero_errors(self):
        findings = check(VALID_EXAMPLE_DIR)
        errors = [f for f in findings if f.level == "error"]
        self.assertEqual(errors, [], f"Expected zero errors, got: {errors}")
        warnings = [f for f in findings if f.level == "warning"]
        self.assertTrue(
            any(f.rule == "session-length" for f in warnings),
            "Expected session-length warning on example fixture",
        )

    def test_invalid_fixtures_match_expectations(self):
        rule_dirs = sorted([d for d in INVALID_DIR.iterdir() if d.is_dir()])
        self.assertGreater(len(rule_dirs), 0, "No invalid fixture directories found")

        for rule_dir in rule_dirs:
            with self.subTest(rule=rule_dir.name):
                expect_file = rule_dir / "EXPECT.txt"
                self.assertTrue(expect_file.is_file(), f"Missing EXPECT.txt in {rule_dir}")
                expect_line = expect_file.read_text().strip()
                parts = expect_line.split()
                self.assertEqual(len(parts), 2, f"Invalid EXPECT.txt line '{expect_line}' in {rule_dir}")
                target_loc, expected_rule = parts

                findings = check(rule_dir)
                errors = [f for f in findings if f.level == "error"]

                matched = False
                for err in errors:
                    loc = f"{err.file}#{err.pointer}"
                    if loc == target_loc and err.rule == expected_rule:
                        matched = True
                        break

                if not matched:
                    actual = [f"{e.file}#{e.pointer} [{e.rule}] {e.message}" for e in errors]
                    self.fail(
                        f"Fixture '{rule_dir.name}' did not match expected finding '{expect_line}'. "
                        f"Actual errors: {actual}"
                    )

    def test_every_brief_error_rule_has_an_invalid_fixture(self):
        fixture_rule_names = {d.name for d in INVALID_DIR.iterdir() if d.is_dir()}
        missing = ALL_ERROR_RULES - fixture_rule_names
        self.assertEqual(missing, set(), f"Missing invalid fixtures for rules: {missing}")

    def test_duplicate_keys_nonfinite_numbers_and_boolean_times_fail(self):
        base = REPO_ROOT / '.tmp/story-check-tests'
        base.mkdir(parents=True, exist_ok=True)
        with TemporaryDirectory(dir=base) as temp:
            target = Path(temp) / 'story'
            shutil.copytree(VALID_EXAMPLE_DIR, target)
            story = target / 'story.json'
            original = story.read_text()
            story.write_text(original.replace('"format": 1', '"format": 1, "format": 1'))
            self.assertTrue(any(f.rule == 'bad-json' for f in check(target)))
            story.write_text(original.replace('"format": 1', '"format": NaN'))
            self.assertTrue(any(f.rule == 'bad-json' for f in check(target)))
            story.write_text(original)
            chapter = target / 'chapters/first.json'
            data = json.loads(chapter.read_text())
            data['on_enter'][0]['at'] = True
            chapter.write_text(json.dumps(data))
            self.assertTrue(any(f.rule == 'bad-value' and f.pointer == '/on_enter/0/at' for f in check(target)))

    def test_warnings_inline(self):
        tmp_base = REPO_ROOT / ".tmp/story-check-tests"
        tmp_base.mkdir(parents=True, exist_ok=True)
        try:
            with TemporaryDirectory(dir=tmp_base, prefix="warn-") as temp_dir:
                tpath = Path(temp_dir)
                (tpath / "chapters").mkdir(exist_ok=True)

                # Base story.json (valid, 700 budget when chapters sum to 700)
                base_story = {
                    "format": 1,
                    "languages": ["es", "en"],
                    "default_language": "es",
                    "language_select": "client",
                    "chapters": ["one"],
                    "endings": {
                        "END": {
                            "title": {"es": "T", "en": "T"},
                            "lines": [{"es": "L", "en": "L"}],
                            "splash": {"es": "S", "en": "S"},
                            "quote_agreement": False,
                        }
                    },
                    "fallback_ending": "END",
                    "autoplay": {
                        "time_scale": 0.25,
                        "runs": [{"ending": "END"}],
                    },
                }

                # 1. session-length warning (< 600)
                ch_under = {
                    "id": "one",
                    "budget_seconds": 300,
                    "checkpoint": "lobby.spawn",
                    "beats": [],
                }
                (tpath / "story.json").write_text(json.dumps(base_story))
                (tpath / "chapters/one.json").write_text(json.dumps(ch_under))
                findings = check(tpath)
                self.assertTrue(any(f.rule == "session-length" for f in findings if f.level == "warning"))

                # 2. session-length no warning (within 600-900)
                ch_ok = {
                    "id": "one",
                    "budget_seconds": 700,
                    "checkpoint": "lobby.spawn",
                    "beats": [],
                }
                (tpath / "chapters/one.json").write_text(json.dumps(ch_ok))
                findings = check(tpath)
                self.assertFalse(any(f.rule == "session-length" for f in findings if f.level == "warning"))

                # 3. session-length warning (> 900)
                ch_over = {
                    "id": "one",
                    "budget_seconds": 1000,
                    "checkpoint": "lobby.spawn",
                    "beats": [],
                }
                (tpath / "chapters/one.json").write_text(json.dumps(ch_over))
                findings = check(tpath)
                self.assertTrue(any(f.rule == "session-length" for f in findings if f.level == "warning"))

                # 4. screamer-count warning (> 2 screamers)
                ch_screamers = {
                    "id": "one",
                    "budget_seconds": 700,
                    "checkpoint": "lobby.spawn",
                    "on_enter": [
                        {"at": 0, "do": "audio", "cue": "screamer"},
                        {"at": 1, "do": "audio", "cue": "screamer"},
                        {"at": 2, "do": "audio", "cue": "screamer"},
                    ],
                    "beats": [],
                }
                (tpath / "chapters/one.json").write_text(json.dumps(ch_screamers))
                findings = check(tpath)
                self.assertTrue(any(f.rule == "screamer-count" for f in findings if f.level == "warning"))

                # screamer-count no warning (<= 2)
                ch_two_screamers = {
                    "id": "one",
                    "budget_seconds": 700,
                    "checkpoint": "lobby.spawn",
                    "on_enter": [
                        {"at": 0, "do": "audio", "cue": "screamer"},
                        {"at": 1, "do": "audio", "cue": "screamer"},
                    ],
                    "beats": [],
                }
                (tpath / "chapters/one.json").write_text(json.dumps(ch_two_screamers))
                findings = check(tpath)
                self.assertFalse(any(f.rule == "screamer-count" for f in findings if f.level == "warning"))

                # 5. long-title warning (> 40 chars)
                ch_long_title = {
                    "id": "one",
                    "budget_seconds": 700,
                    "checkpoint": "lobby.spawn",
                    "on_enter": [
                        {
                            "at": 0,
                            "do": "title",
                            "title": {
                                "es": "Esta es una cadena de titulo sumamente larga que sobrepasa los cuarenta caracteres",
                                "en": "Short title",
                            },
                        }
                    ],
                    "beats": [],
                }
                (tpath / "chapters/one.json").write_text(json.dumps(ch_long_title))
                findings = check(tpath)
                self.assertTrue(any(f.rule == "long-title" for f in findings if f.level == "warning"))

                # long-title no warning (<= 40 chars)
                ch_short_title = {
                    "id": "one",
                    "budget_seconds": 700,
                    "checkpoint": "lobby.spawn",
                    "on_enter": [
                        {
                            "at": 0,
                            "do": "title",
                            "title": {
                                "es": "Titulo corto",
                                "en": "Short title",
                            },
                        }
                    ],
                    "beats": [],
                }
                (tpath / "chapters/one.json").write_text(json.dumps(ch_short_title))
                findings = check(tpath)
                self.assertFalse(any(f.rule == "long-title" for f in findings if f.level == "warning"))
        finally:
            if tmp_base.exists():
                shutil.rmtree(tmp_base, ignore_errors=True)

    def test_malformed_json_yields_bad_json_at_root(self):
        tmp_base = REPO_ROOT / ".tmp/story-check-tests"
        tmp_base.mkdir(parents=True, exist_ok=True)
        try:
            with TemporaryDirectory(dir=tmp_base, prefix="bad-json-") as temp_dir:
                tpath = Path(temp_dir)
                (tpath / "story.json").write_text("{ format: 1, ")
                findings = check(tpath)
                errors = [f for f in findings if f.level == "error"]
                self.assertEqual(len(errors), 1)
                self.assertEqual(errors[0].file, "story.json")
                self.assertEqual(errors[0].pointer, "")
                self.assertEqual(errors[0].rule, "bad-json")
        finally:
            if tmp_base.exists():
                shutil.rmtree(tmp_base, ignore_errors=True)

    def test_shipped_story_if_present(self):
        shipped_story = REPO_ROOT / "src/main/resources/data/unallocated/story/story.json"
        if shipped_story.exists():
            findings = check(shipped_story.parent)
            errors = [f for f in findings if f.level == "error"]
            self.assertEqual(errors, [], f"Shipped story has errors: {errors}")

    def test_cli_execution(self):
        script_path = REPO_ROOT / "scripts/story_check.py"

        # Valid example fixture: exit code 0
        proc = subprocess.run(
            [sys.executable, str(script_path), str(VALID_EXAMPLE_DIR)],
            capture_output=True,
            text=True,
        )
        self.assertEqual(proc.returncode, 0, f"Expected 0, got {proc.returncode}. Output: {proc.stdout}\n{proc.stderr}")
        lines = [line.strip() for line in proc.stdout.strip().splitlines() if line.strip()]
        self.assertGreater(len(lines), 0)
        self.assertTrue(SUMMARY_LINE_RE.match(lines[-1]), f"Last line was not summary: '{lines[-1]}'")
        for line in lines[:-1]:
            self.assertTrue(OUTPUT_LINE_RE.match(line), f"Line did not match output format: '{line}'")

        # Invalid fixture: exit code 1
        invalid_fix = INVALID_DIR / "unknown-action"
        proc_bad = subprocess.run(
            [sys.executable, str(script_path), str(invalid_fix)],
            capture_output=True,
            text=True,
        )
        self.assertEqual(proc_bad.returncode, 1, f"Expected 1, got {proc_bad.returncode}. Output: {proc_bad.stdout}")
        bad_lines = [line.strip() for line in proc_bad.stdout.strip().splitlines() if line.strip()]
        self.assertGreater(len(bad_lines), 0)
        self.assertTrue(SUMMARY_LINE_RE.match(bad_lines[-1]), f"Last line was not summary: '{bad_lines[-1]}'")
        for line in bad_lines[:-1]:
            self.assertTrue(OUTPUT_LINE_RE.match(line), f"Line did not match output format: '{line}'")


if __name__ == "__main__":
    unittest.main()
