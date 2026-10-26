"""Prove the two-process watchdog requires fresh contract evidence, without invoking Java."""

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

import smoke_test


class ContractSmokeTest(unittest.TestCase):
    def run_client(self, *, missing=None, rewrite=False):
        (smoke_test.ROOT / '.tmp').mkdir(exist_ok=True)
        with TemporaryDirectory(dir=smoke_test.ROOT / '.tmp') as temp:
            smoke = Path(temp)
            directory = smoke / 'client'
            calls = []

            def fake_monitor(command, cwd, console, required, **kwargs):
                phase = command[-1].split('=')[1]
                calls.append(phase)
                self.assertEqual(kwargs['game_directory'], directory)
                self.assertTrue(any('contract ' in evidence for evidence in required))
                if phase == 'restart':
                    self.assertFalse((directory / 'logs/latest.log').exists())
                    self.assertTrue((directory / 'logs-first/latest.log').exists())
                    self.assertTrue((directory / 'unallocated/contract.properties').is_file())
                (directory / 'logs').mkdir(exist_ok=True)
                (directory / 'logs/latest.log').write_text(phase)
                receipt = directory / 'unallocated/contract.properties'
                receipt.parent.mkdir(exist_ok=True)
                if missing != 'receipt':
                    receipt.write_bytes(b'rewritten' if phase == 'restart' and rewrite else b'original')
                for name in smoke_test.client_phases()[phase]['screenshots']:
                    if missing != name:
                        (directory / f'0000_{name}.png').write_bytes(b'\x89PNG\r\n\x1a\n')
                # Loom's run task installs exactly the player JAR into the shared game directory.
                (directory / 'mods').mkdir(exist_ok=True)
                (directory / 'mods' / f"unallocated-{smoke_test.properties()['version']}.jar").touch()

            with patch.object(smoke_test, 'SMOKE', smoke), patch.object(smoke_test, 'monitor', fake_monitor):
                smoke_test.client()
            self.assertEqual(calls, ['first', 'restart'])

    def test_same_directory_two_processes_preserve_receipt_and_archive_first_log(self):
        self.run_client()

    def test_second_process_must_not_rewrite_receipt(self):
        with self.assertRaisesRegex(RuntimeError, 'rewrote'):
            self.run_client(rewrite=True)

    def test_first_screenshot_cannot_substitute_for_restart_screenshot(self):
        with self.assertRaisesRegex(RuntimeError, 'screenshot'):
            self.run_client(missing='unallocated-title-restart')

    def test_contract_and_data_list_need_screenshots(self):
        for missing in ('unallocated-contract', 'unallocated-contract-uses'):
            with self.subTest(missing=missing), self.assertRaisesRegex(RuntimeError, 'contract screenshot'):
                self.run_client(missing=missing)

    def test_missing_receipt_fails(self):
        with self.assertRaises(FileNotFoundError):
            self.run_client(missing='receipt')


if __name__ == '__main__':
    unittest.main()
