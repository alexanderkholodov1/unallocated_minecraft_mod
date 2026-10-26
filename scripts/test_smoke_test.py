"""Exercise watchdog failure paths with Python subprocesses, never Minecraft/Java."""

from contextlib import redirect_stderr, redirect_stdout
import io
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest

from smoke_test import ROOT, monitor


COMMON = r'UNALLOCATED initialized \(common\)\.'
DONE = r'Done \([^\r\n]*\)!'


class WatchdogTest(unittest.TestCase):
    def run_fixture(self, body, required=(COMMON, DONE), **kwargs):
        (ROOT / '.tmp').mkdir(exist_ok=True)
        with TemporaryDirectory(dir=ROOT / '.tmp', prefix='smoke-watchdog-') as temp:
            directory = Path(temp)
            code = (
                "from pathlib import Path\nimport sys,time\n"
                "Path('logs').mkdir()\nlog=Path('logs/latest.log')\n" + body
            )
            with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
                monitor(
                    [sys.executable, '-c', code], directory, directory / 'console.log',
                    required, timeout=kwargs.pop('timeout', 2), **kwargs,
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

    def test_hanging_process_is_killed_on_timeout(self):
        with self.assertRaisesRegex(RuntimeError, 'Timed out'):
            self.run_fixture('time.sleep(20)\n', timeout=0.3)


if __name__ == '__main__':
    unittest.main()
