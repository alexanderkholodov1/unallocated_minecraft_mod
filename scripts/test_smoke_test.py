"""Exercise watchdog failure paths with Python subprocesses, never Minecraft/Java."""

from unittest.mock import patch
from contextlib import redirect_stderr, redirect_stdout
import io
import os
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest

from smoke_test import ROOT, monitor, verify_screenshot, server_evidence, API_POLICY, API_VERSION, api_version_argument, profile_name, expects_rejection, verify_interface_screenshots, client_phases


COMMON = r'UNALLOCATED initialized \(common\)\.'
DONE = r'Done \([^\r\n]*\)!'


class JavaHomeTest(unittest.TestCase):
    def setUp(self):
        (ROOT / '.tmp').mkdir(exist_ok=True)

    @staticmethod
    def fake_jdk(directory, version='25.0.5', body='raise AssertionError("Java must not run during validation")\n'):
        home = directory / 'selected jdk'
        (home / 'bin').mkdir(parents=True)
        (home / 'release').write_text(f'JAVA_VERSION="{version}"\n')
        java = home / 'bin/java'
        java.write_text(f'#!{sys.executable}\n' + body)
        java.chmod(0o755)
        return home

    def test_requires_java_home_and_java_25_without_starting_java(self):
        import smoke_test
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(RuntimeError, 'JAVA_HOME is required'):
                smoke_test.java_executable()
        for version in ('21.0.9', '26', '1.8.0_472', 'unknown'):
            with self.subTest(version=version), TemporaryDirectory(dir=ROOT / '.tmp') as temp:
                home = self.fake_jdk(Path(temp), version)
                with patch.dict(os.environ, {'JAVA_HOME': str(home)}):
                    with self.assertRaisesRegex(RuntimeError, 'JAVA_HOME must point to Java 25'):
                        smoke_test.java_executable()
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            home = self.fake_jdk(Path(temp))
            with patch.dict(os.environ, {'JAVA_HOME': str(home)}):
                self.assertEqual(smoke_test.java_executable(), str(home / 'bin/java'))
                (home / 'bin/java').unlink()
                with self.assertRaisesRegex(RuntimeError, 'no executable Java 25 launcher'):
                    smoke_test.java_executable()
                (home / 'release').unlink()
                with self.assertRaisesRegex(RuntimeError, 'cannot read'):
                    smoke_test.java_executable()

    def test_server_launches_java_home_java_even_when_path_contains_older_java(self):
        import smoke_test
        from zipfile import ZipFile
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            root = Path(temp)
            called, wrong = root / 'selected-called', root / 'path-called'
            # These executables are Python stand-ins: no Java or game process runs locally.
            body = (
                'from pathlib import Path\nimport sys\n'
                f'Path({str(called)!r}).write_text("selected")\n'
                'assert "-jar" in sys.argv\n'
                'Path("logs").mkdir()\n'
                f'Path("logs/latest.log").write_text({WatchdogTest.REJECTION!r})\n'
                'sys.exit(1)\n'
            )
            home = self.fake_jdk(root, body=body)
            path_bin = root / 'path-bin'
            path_bin.mkdir()
            path_java = path_bin / 'java'
            path_java.write_text(f'#!{sys.executable}\nfrom pathlib import Path\nPath({str(wrong)!r}).touch()\nraise SystemExit(99)\n')
            path_java.chmod(0o755)
            (root / 'build/libs').mkdir(parents=True)
            (root / 'build/libs/unallocated-test.jar').write_bytes(b'player fixture')
            (root / 'build/smoke/full-api').mkdir(parents=True)
            (root / 'build/smoke/full-api/fabric-api-0.153.1+26.3.jar').write_bytes(b'api fixture')
            downloads = root / '.tmp/smoke-downloads/26.3-0.20.1-1.1.2'
            downloads.mkdir(parents=True)
            with ZipFile(downloads / 'fabric-server-launch.jar', 'w'):
                pass
            props = {'version': 'test', 'minecraft_version': '26.3', 'loader_version': '0.20.1'}
            with patch.dict(os.environ, {'JAVA_HOME': str(home), 'PATH': str(path_bin)}), \
                    patch.object(smoke_test, 'ROOT', root), patch.object(smoke_test, 'SMOKE', root / '.tmp/smoke'), \
                    patch.object(smoke_test, 'properties', return_value=props), \
                    patch.object(smoke_test, 'urlopen', side_effect=AssertionError('Unexpected download')), \
                    redirect_stdout(io.StringIO()):
                smoke_test.server('0.153.1+26.3')
            self.assertTrue(called.is_file())
            self.assertFalse(wrong.exists(), 'Smoke must never launch PATH java')

    def test_gradle_daemon_uses_the_validated_java_home(self):
        import smoke_test
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            root = Path(temp)
            home = self.fake_jdk(root)
            gradle = root / 'gradle'
            gradle.write_text(f'#!{sys.executable}\nimport sys\nassert sys.argv[1] == {"-Dorg.gradle.java.home=" + str(home)!r}\n')
            gradle.chmod(0o755)
            with patch.dict(os.environ, {'JAVA_HOME': str(home)}), redirect_stdout(io.StringIO()):
                monitor([str(gradle), '--daemon'], root, root / 'console.log', (), timeout=2)


class WatchdogTest(unittest.TestCase):
    def run_fixture(self, body, required=(COMMON, DONE), **kwargs):
        (ROOT / '.tmp').mkdir(exist_ok=True)
        with TemporaryDirectory(dir=ROOT / '.tmp', prefix='smoke-watchdog-') as temp:
            directory = Path(temp)
            game_directory = directory / 'game' if kwargs.pop('separate_game_dir', False) else directory
            (game_directory / 'logs').mkdir(parents=True)
            code = (
                "from pathlib import Path\nimport sys,time\n"
                f"log=Path({str(game_directory / 'logs/latest.log')!r})\n" + body
            )
            with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
                monitor(
                    [sys.executable, '-c', code], directory, directory / 'console.log',
                    required, timeout=kwargs.pop('timeout', 2), **kwargs,
                    game_directory=game_directory,
                )

    def test_server_requires_stop_and_clean_exit(self):
        self.run_fixture(
            "log.write_text('UNALLOCATED initialized (common).\\nDone (0.1s)!\\n')\n"
            "assert sys.stdin.readline().strip()=='stop'\n"
            "log.write_text(log.read_text()+'Stopping server\\n')\n",
            stop_when_ready=True,
        )

    def test_console_commands_run_before_stop_and_their_evidence_is_required(self):
        self.run_fixture(
            "log.write_text('UNALLOCATED initialized (common).\\nDone (0.1s)!\\n')\n"
            "assert sys.stdin.readline().strip()=='ubait selftest'\n"
            "log.write_text(log.read_text()+'UNALLOCATED bait selftest passed\\n')\n"
            "assert sys.stdin.readline().strip()=='stop'\n"
            "log.write_text(log.read_text()+'Stopping server\\n')\n",
            required=(COMMON, DONE, 'UNALLOCATED bait selftest passed'),
            stop_when_ready=True, startup_required=(COMMON, DONE),
            command_steps=[('ubait selftest', ['UNALLOCATED bait selftest passed'])],
        )

    def test_failed_selftest_fails_without_waiting_for_the_deadline(self):
        with self.assertRaisesRegex(RuntimeError, 'self-test failed: .*no spyglass'):
            self.run_fixture(
                "log.write_text('UNALLOCATED initialized (common).\\nDone (0.1s)!\\n')\n"
                "sys.stdin.readline()\n"
                "log.write_text(log.read_text()+'UNALLOCATED bait selftest FAILED: no spyglass\\n')\n"
                "time.sleep(20)\n",
                required=(COMMON, DONE, 'UNALLOCATED bait selftest passed'), timeout=5,
                stop_when_ready=True, startup_required=(COMMON, DONE),
            command_steps=[('ubait selftest', ['UNALLOCATED bait selftest passed'])],
            )

    def test_missing_common_or_done_fails_even_on_exit_zero(self):
        for content in ['Done (0.1s)!', 'UNALLOCATED initialized (common).']:
            with self.subTest(content=content), self.assertRaisesRegex(RuntimeError, 'Missing required'):
                self.run_fixture(f'log.write_text({content!r})\n')

    def test_ready_then_early_exit_is_not_clean_shutdown(self):
        with self.assertRaisesRegex(RuntimeError, 'stop command'):
            self.run_fixture(
                "log.write_text('UNALLOCATED initialized (common).\\nDone (0.1s)!\\n')\n",
                stop_when_ready=True,
            )

    def test_server_requires_loaded_bundled_modules(self):
        with self.assertRaisesRegex(RuntimeError, 'Missing required'):
            self.run_fixture(
                "log.write_text('UNALLOCATED initialized (common).\\nDone (0.1s)!\\n')\n",
                required=server_evidence(),
            )
        loaded = 'UNALLOCATED initialized (common).\nDone (0.1s)!\n' + '\n'.join(
            f"- {key} {value['version']}" for key, value in API_POLICY['modules'].items()
            if value['environment'] != 'client'
        )
        self.run_fixture(f'log.write_text({loaded!r})\n', required=server_evidence())
        with self.assertRaisesRegex(RuntimeError, 'Missing required'):
            self.run_fixture(f'log.write_text({loaded!r})\n', required=server_evidence(True))
        loaded += f'\n- fabric-api {API_VERSION}\n'
        self.run_fixture(f'log.write_text({loaded!r})\n', required=server_evidence(True))

    def test_each_external_api_version_requires_its_own_loader_evidence(self):
        loaded = 'UNALLOCATED initialized (common).\nDone (0.1s)!\n' + '\n'.join(
            f"- {key} {value['version']}" for key, value in API_POLICY['modules'].items()
            if value['environment'] != 'client'
        )
        for version in API_POLICY['smoke_api_versions']:
            with self.subTest(version=version):
                self.assertEqual(api_version_argument(version), version)
                self.assertEqual(profile_name('server', version), 'server-api-' + version)
                with self.assertRaisesRegex(RuntimeError, 'Missing required'):
                    self.run_fixture(f'log.write_text({loaded!r})\n', required=server_evidence(version))
                expected = loaded + f'\n- fabric-api {version}\n'
                self.run_fixture(f'log.write_text({expected!r})\n', required=server_evidence(version))
        with self.assertRaisesRegex(ValueError, 'Unreviewed'):
            api_version_argument('../unreviewed')
        self.assertIsNone(api_version_argument('none'))

    REJECTION = (
        "Incompatible mods found!\n"
        "Mod 'UNALLOCATED' (unallocated) 0.1.6 is incompatible with any version "
        "before 0.161.0 of mod 'Fabric API' (fabric-api), yet a conflicting version is present: "
        "0.153.1+26.3!\nRemove one of the mods or check for updates.\n"
    )

    def test_external_api_rejection_boundary(self):
        self.assertTrue(expects_rejection('0.153.1+26.3'))
        self.assertTrue(expects_rejection('0.160.7+26.3'))
        self.assertFalse(expects_rejection('0.161.0+26.3'))
        self.assertFalse(expects_rejection('0.165.0+26.3'))
        self.assertFalse(expects_rejection(None))

    def test_loader_rejection_requires_nonzero_exit_and_specific_message(self):
        self.run_fixture(f'print({self.REJECTION!r})\nsys.exit(1)\n',
                         required=(), expected_rejection='0.153.1+26.3')
        with self.assertRaisesRegex(RuntimeError, 'exited successfully'):
            self.run_fixture(f'print({self.REJECTION!r})\n',
                             required=(), expected_rejection='0.153.1+26.3')
        for message in ['Unrelated startup failure',
                        self.REJECTION.replace('(unallocated)', '(other-mod)'),
                        self.REJECTION.replace('0.153.1+26.3', '0.150.0+26.3'),
                        self.REJECTION.replace('0.161.0', '0.165.0')]:
            with self.subTest(message=message), self.assertRaisesRegex(RuntimeError, 'specific Loader'):
                self.run_fixture(f'print({message!r})\nsys.exit(1)\n',
                                 required=(), expected_rejection='0.153.1+26.3')

    def test_dependency_rejection_never_accepts_game_initialization_or_crash(self):
        for extra in ['UNALLOCATED initialized (common).', 'Done (0.1s)!',
                      'MixinApplyError: fabric-entity-events-v1',
                      'java.lang.ClassNotFoundException: net.minecraft.world.item.HoeItem']:
            with self.subTest(extra=extra), self.assertRaises(RuntimeError):
                message = self.REJECTION + extra
                self.run_fixture(f'print({message!r})\nsys.exit(1)\n',
                                 required=(), expected_rejection='0.153.1+26.3')

    def test_nonzero_exit_fails(self):
        with self.assertRaises(RuntimeError):
            self.run_fixture("sys.exit(9)\n")

    def test_crash_report_fails_even_with_exit_zero(self):
        with self.assertRaisesRegex(RuntimeError, 'crash'):
            self.run_fixture(
                "Path('crash-reports').mkdir()\n"
                "Path('crash-reports/crash-test.txt').write_text('crashed')\n",
                required=(),
            )

    def test_fatal_stderr_fails(self):
        with self.assertRaisesRegex(RuntimeError, 'crash'):
            self.run_fixture("print('Exception in thread main',file=sys.stderr)\n", required=())

    def test_map_command_exception_fails_before_waiting_for_success_timeout(self):
        with self.assertRaisesRegex(RuntimeError, 'Game crash detected'):
            self.run_fixture(
                "log.write_text('UNALLOCATED map command failed operation=build all\\nIllegalStateException: fixture\\n')\n"
                "time.sleep(20)\n",
                required=('UNALLOCATED map built section=all',), timeout=1,
            )

    def test_story_validation_marker_is_fatal(self):
        with self.assertRaisesRegex(RuntimeError, 'crash'):
            self.run_fixture("log.write_text('UNALLOCATED story invalid story.json#/format: [bad-value] fixture')\n", required=())

    def test_ci_entrypoint_requests_all_three_client_phases(self):
        import smoke_test
        with patch.object(sys, 'argv', ['smoke_test.py', 'client']), patch.dict(smoke_test.os.environ, {'CI': 'true'}), patch.object(smoke_test, 'client') as client:
            smoke_test.main()
            client.assert_called_once_with(None, include_story=True)

    def test_story_phase_requires_both_endings_and_every_capture(self):
        phases = client_phases()
        self.assertEqual(list(phases), ['first', 'restart', 'story'])
        story = phases['story']
        self.assertIn('story-example-sign', story['screenshots'])
        self.assertIn('story-example-end', story['screenshots'])
        for letter in ('a', 'b'):
            self.assertIn('unallocated-story-farewell-' + letter, story['screenshots'])
        for index, ending in enumerate(('EXAMPLE_A', 'EXAMPLE_B')):
            self.assertTrue(any(f'run={index} ending={ending} complete' in line for line in story['evidence']))

    def test_asynchronous_audio_failure_fails(self):
        with self.assertRaisesRegex(RuntimeError, 'crash'):
            self.run_fixture("log.write_text('Failed to load sound unallocated:audio.breath')\n", required=())

    def test_hanging_process_is_killed_on_timeout(self):
        with self.assertRaisesRegex(RuntimeError, 'Timed out'):
            self.run_fixture('time.sleep(20)\n', timeout=0.3)

    def test_client_reads_game_directory_separately_from_launcher(self):
        self.run_fixture(
            "log.write_text('UNALLOCATED initialized (client).\\nTITLE_READY\\n')\n",
            required=(r'UNALLOCATED initialized \(client\)\.', 'TITLE_READY'),
            separate_game_dir=True,
        )

    def test_console_messages_cannot_replace_game_log_evidence(self):
        with self.assertRaisesRegex(RuntimeError, 'Missing required'):
            self.run_fixture("print('UNALLOCATED initialized (common).\\nDone (0.1s)!')\n")

    def test_title_marker_cannot_replace_client_entrypoint(self):
        with self.assertRaisesRegex(RuntimeError, 'Missing required'):
            self.run_fixture(
                "log.write_text('TITLE_READY')\n",
                required=(r'UNALLOCATED initialized \(client\)\.', 'TITLE_READY'),
                separate_game_dir=True,
            )

    def test_numbered_fabric_screenshot_is_accepted(self):
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            directory = Path(temp)
            (directory / '0000_unallocated-title.png').write_bytes(b'\x89PNG\r\n\x1a\n')
            verify_screenshot(directory)

    def test_title_capture_cannot_replace_ending_captures(self):
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            directory = Path(temp)
            (directory / 'unallocated-title.png').write_bytes(b'\x89PNG\r\n\x1a\n')
            with self.assertRaisesRegex(RuntimeError, 'unallocated-ending-disconnect'):
                verify_screenshot(directory, 'unallocated-ending-disconnect')
            (directory / '0001_unallocated-ending-disconnect.png').write_bytes(b'\x89PNG\r\n\x1a\n')
            verify_screenshot(directory, 'unallocated-ending-disconnect')

    def test_missing_or_invalid_screenshot_fails(self):
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            directory = Path(temp)
            with self.assertRaisesRegex(RuntimeError, 'screenshot'):
                verify_screenshot(directory)
            (directory / 'unallocated-title.png').write_text('not an image')
            with self.assertRaisesRegex(RuntimeError, 'screenshot'):
                verify_screenshot(directory)

    def test_title_screenshot_cannot_replace_map_tour(self):
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            directory = Path(temp)
            (directory / 'unallocated-title.png').write_bytes(b'\x89PNG\r\n\x1a\n')
            with self.assertRaisesRegex(RuntimeError, 'map-lobby-spawn'):
                verify_screenshot(directory, ('unallocated-title', 'map-lobby-spawn'))

    def test_core_commands_wait_for_fresh_game_log_evidence(self):
        self.run_fixture(
            "log.write_text('UNALLOCATED initialized (common).\\nDone (0.1s)!\\n')\n"
            "assert sys.stdin.readline().strip()=='unallocated arm'\n"
            "log.write_text(log.read_text()+'UNALLOCATED session armed\\n')\n"
            "assert sys.stdin.readline().strip()=='unallocated act LOBBY'\n"
            "log.write_text(log.read_text()+'UNALLOCATED act=LOBBY enter t=0\\n')\n"
            "assert sys.stdin.readline().strip()=='unallocated status'\n"
            "log.write_text(log.read_text()+'UNALLOCATED status act=LOBBY\\n')\n"
            "assert sys.stdin.readline().strip()=='stop'\n"
            "log.write_text(log.read_text()+'Stopping server\\n')\n",
            required=(COMMON, DONE, r'UNALLOCATED session armed', r'UNALLOCATED act=LOBBY enter t=0', r'UNALLOCATED status act=LOBBY'),
            startup_required=(COMMON, DONE), stop_when_ready=True,
            command_steps=[('unallocated arm', [r'UNALLOCATED session armed']), ('unallocated act LOBBY', [r'UNALLOCATED act=LOBBY enter t=0']),
                           ('unallocated status', [r'UNALLOCATED status act=LOBBY'])],
        )

    def test_old_marker_cannot_acknowledge_command(self):
        with self.assertRaisesRegex(RuntimeError, 'Timed out'):
            self.run_fixture(
                "log.write_text('UNALLOCATED initialized (common).\\nDone (0.1s)!\\nACT_ENTER\\n')\n"
                "assert sys.stdin.readline().strip()=='unallocated act LOBBY'\n"
                "time.sleep(20)\n",
                required=(COMMON, DONE, 'ACT_ENTER'), startup_required=(COMMON, DONE),
                stop_when_ready=True, command_steps=[('unallocated act LOBBY', ['ACT_ENTER'])], timeout=0.6,
            )

    def test_interface_requires_each_effect_screenshot(self):
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            directory = Path(temp)
            for effect in ('splash', 'loading', 'f3', 'advancement', 'sign', 'death'):
                with self.assertRaisesRegex(RuntimeError, 'interface screenshot'):
                    verify_interface_screenshots(directory)
                (directory / f'0001_unallocated-interface-{effect}.png').write_bytes(b'\x89PNG\r\n\x1a\n')
            verify_interface_screenshots(directory)

    def test_title_only_marker_cannot_replace_effect_checks(self):
        with self.assertRaisesRegex(RuntimeError, 'Missing required'):
            self.run_fixture("log.write_text('TITLE_READY')\n", required=('TITLE_READY', 'INTERFACE_RESTORED'))


if __name__ == '__main__':
    unittest.main()
