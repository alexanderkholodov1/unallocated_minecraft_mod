"""Exercise watchdog failure paths with Python subprocesses, never Minecraft/Java."""

from contextlib import redirect_stderr, redirect_stdout
import io
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest

from smoke_test import ROOT, monitor, verify_screenshot, server_evidence, API_POLICY, API_VERSION


COMMON = r'UNALLOCATED initialized \(common\)\.'
DONE = r'Done \([^\r\n]*\)!'


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
            "assert sys.stdin.readline().strip()=='unallocated act LOBBY'\n"
            "log.write_text(log.read_text()+'UNALLOCATED act=LOBBY enter t=0\\n')\n"
            "assert sys.stdin.readline().strip()=='unallocated status'\n"
            "log.write_text(log.read_text()+'UNALLOCATED status act=LOBBY\\n')\n"
            "assert sys.stdin.readline().strip()=='stop'\n"
            "log.write_text(log.read_text()+'Stopping server\\n')\n",
            required=(COMMON, DONE, r'UNALLOCATED act=LOBBY enter t=0', r'UNALLOCATED status act=LOBBY'),
            startup_required=(COMMON, DONE), stop_when_ready=True,
            command_steps=[('unallocated act LOBBY', [r'UNALLOCATED act=LOBBY enter t=0']),
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


if __name__ == '__main__':
    unittest.main()
