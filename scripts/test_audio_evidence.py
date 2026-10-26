"""Fail-closed CI evidence for the real client audio test; no Java on this host."""
import re
import unittest
from smoke_test import audio_evidence


class AudioEvidenceTest(unittest.TestCase):
    def test_requires_every_event_and_cleanup_marker(self):
        log = 'UNALLOCATED audio: registered 7 sound events; loud=screamer.\n'
        layers = {
            'ambient': ('ambient_drone', 'ambient_cave'),
            'threads': ('thread_string', 'thread_chime'),
            'muffle': ('breath',),
            'screamer': ('screamer_impact', 'screamer_crack'),
        }
        for cue, events in layers.items():
            log += f'UNALLOCATED smoke: audio cue={cue} registered, resolved and active.\n'
            for event in events:
                log += (f'UNALLOCATED audio: play cue={cue} event=unallocated:audio.{event} '
                        f'loud={str(cue == "screamer").lower()} result=STARTED\n')
        log += 'UNALLOCATED smoke: audio attenuation, expiry, replacement, stop, disconnect and options verified.\n'
        patterns = audio_evidence()
        self.assertTrue(all(re.search(pattern, log) for pattern in patterns))
        for line in log.splitlines(keepends=True):
            with self.subTest(line=line):
                self.assertFalse(all(re.search(pattern, log.replace(line, '')) for pattern in patterns))
        for wrong in ('NOT_STARTED', 'STARTED_SILENTLY'):
            with self.subTest(wrong=wrong):
                self.assertFalse(all(re.search(pattern, log.replace('result=STARTED', 'result=' + wrong))
                                     for pattern in patterns))
