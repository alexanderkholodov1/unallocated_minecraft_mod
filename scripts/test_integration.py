"""Structural integration checks: reject silent JSON overwrites and unwired production mixins."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate JSON key: ' + key)
        result[key] = value
    return result


class IntegrationTest(unittest.TestCase):
    def test_source_json_cannot_silently_overwrite_fields(self):
        for path in [ROOT / 'fabric-api-modules.json', *(ROOT / 'src').rglob('*.json')]:
            with self.subTest(path=path.relative_to(ROOT)):
                json.loads(path.read_text(), object_pairs_hook=unique_object)

    def test_every_production_mixin_configuration_is_registered_once(self):
        metadata = json.loads((ROOT / 'src/main/resources/fabric.mod.json').read_text())
        declared = [entry if isinstance(entry, str) else entry['config'] for entry in metadata['mixins']]
        available = {path.name for side in ('main', 'client')
                     for path in (ROOT / f'src/{side}/resources').rglob('*.mixins.json')}
        self.assertEqual(len(declared), len(set(declared)))
        self.assertEqual(set(declared), available)
        for entry in metadata['mixins']:
            name = entry if isinstance(entry, str) else entry['config']
            if (ROOT / 'src/client/resources' / name).exists():
                self.assertIsInstance(entry, dict)
                self.assertEqual(entry['environment'], 'client')

    def test_client_helper_has_one_orchestrator_and_keeps_contract_observer(self):
        helper = json.loads((ROOT / 'src/clientSmoke/resources/fabric.mod.json').read_text())
        self.assertEqual(helper['entrypoints']['fabric-client-gametest'],
                         ['org.unallocated.smoke.TitleScreenSmokeTest'])
        self.assertIn('org.unallocated.smoke.ContractSmokeObserver', helper['entrypoints']['client'])


if __name__ == '__main__':
    unittest.main()
