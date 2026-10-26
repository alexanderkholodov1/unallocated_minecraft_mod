"""Adversarial archive fixtures for the release gate, without Java."""

from copy import deepcopy
import hashlib
from io import BytesIO
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import BadZipFile, ZipFile, ZIP_DEFLATED, ZIP_STORED

from verify_jar import ROOT, LICENSE, NOTICE, verify
from smoke_test import verify_mod_directory


def archive(entries):
    data = BytesIO()
    with ZipFile(data, 'w', compression=ZIP_DEFLATED) as jar:
        for key, value in entries.items():
            jar.writestr(key, value)
    return data.getvalue()


class PackagingTest(unittest.TestCase):
    def setUp(self):
        (ROOT / '.tmp').mkdir(exist_ok=True)
        self.temp = TemporaryDirectory(dir=ROOT / '.tmp')
        self.addCleanup(self.temp.cleanup)
        self.artifact = Path(self.temp.name) / 'player.jar'
        self.props = {'version': 'test', 'loader_version': '0.20.1', 'minecraft_version': '26.3'}
        self.policy = json.loads((ROOT / 'fabric-api-modules.json').read_text())
        self.included_hashes = {}
        self.entries = {LICENSE: 'Apache License', NOTICE: 'FabricMC Apache-2.0'}
        self.meta = {'id': 'unallocated', 'name': 'UNALLOCATED', 'version': 'test',
                     'entrypoints': {'main': ['example.Common'], 'client': ['example.Client']},
                     'depends': {'fabricloader': '>=0.20.1', 'minecraft': '26.3', 'java': '>=25'},
                     'jars': [],
                     'breaks': {'fabric-api': '<' + self.policy['minimum_external_api_version']}}
        for module_id, module in self.policy['modules'].items():
            self.set_module(module_id, {'schemaVersion': 1, 'id': module_id,
                'version': module['version'], 'environment': module['environment'],
                'license': 'Apache-2.0', 'depends': {'fabricloader': '>=0.19.3'}})
            self.meta['depends'][module_id] = '>=' + module['version']
            self.meta['jars'].append({'file': self.module_path(module_id)})
        for side in ('Common', 'Client'):
            self.entries[f'example/{side}.class'] = bytes.fromhex('cafebabe00000045')

    def module_path(self, module_id):
        return f'META-INF/jars/{module_id}-{self.policy["modules"][module_id]["version"]}.jar'

    def set_module(self, module_id, metadata, extras=None):
        raw = archive({'fabric.mod.json': json.dumps(metadata),
                       'LICENSE-' + module_id: 'Apache License', **(extras or {})})
        self.entries[self.module_path(module_id)] = raw
        self.included_hashes[self.module_path(module_id)] = hashlib.sha256(raw).hexdigest()

    def check(self):
        self.entries['fabric.mod.json'] = json.dumps(self.meta)
        self.artifact.write_bytes(archive(self.entries))
        return verify(self.artifact, self.props, self.policy, self.included_hashes)

    def test_packaged_mixin_config_cannot_be_left_unregistered(self):
        self.entries['forgotten.mixins.json'] = '{}'
        with self.assertRaisesRegex(ValueError, 'Unregistered or missing mixin'):
            self.check()

    def test_mixin_registration_cannot_be_duplicated(self):
        self.meta['mixins'] = ['duplicate.mixins.json', 'duplicate.mixins.json']
        with self.assertRaisesRegex(ValueError, 'Duplicate mixin registration'):
            self.check()

    def test_approved_subset_passes(self):
        self.assertEqual(self.check(), hashlib.sha256(self.artifact.read_bytes()).hexdigest())

    def test_missing_extra_or_undeclared_jar_fails(self):
        original = deepcopy(self.entries)
        for mutation in ('missing', 'extra', 'undeclared'):
            with self.subTest(mutation=mutation):
                self.entries = deepcopy(original)
                if mutation == 'missing':
                    del self.entries[self.meta['jars'][0]['file']]
                elif mutation == 'extra':
                    self.entries['META-INF/jars/fabric-api.jar'] = b'bad'
                else:
                    self.meta['jars'].pop()
                with self.assertRaisesRegex(ValueError, 'allowlist'):
                    self.check()

    def test_registry_sync_cannot_be_omitted_from_player(self):
        module_id = 'fabric-registry-sync-v0'
        self.assertIn(module_id, self.policy['modules'])
        original = deepcopy(self.entries)
        del self.entries[self.module_path(module_id)]
        with self.assertRaisesRegex(ValueError, 'allowlist'):
            self.check()
        self.entries = original
        del self.meta['depends'][module_id]
        with self.assertRaisesRegex(ValueError, 'Wrong player dependencies'):
            self.check()

    def test_repackaged_resolved_input_passes_but_unrecorded_changes_fail(self):
        path = self.meta['jars'][0]['file']
        original = self.entries[path]
        data = BytesIO()
        with ZipFile(BytesIO(original)) as source, ZipFile(data, 'w', ZIP_STORED) as target:
            # Different compression, entry ordering and JSON serialization must be accepted.
            for name in reversed(source.namelist()):
                content = source.read(name)
                if name == 'fabric.mod.json':
                    content = json.dumps(json.loads(content), indent=4, sort_keys=True).encode()
                target.writestr(name, content)
        self.entries[path] = data.getvalue()
        self.assertNotEqual(original, self.entries[path])
        with self.assertRaisesRegex(ValueError, 'differs from Gradle include input'):
            self.check()
        self.included_hashes[path] = hashlib.sha256(self.entries[path]).hexdigest()
        self.assertEqual(self.check(), hashlib.sha256(self.artifact.read_bytes()).hexdigest())

    def test_invalid_nested_archive_fails(self):
        self.entries[self.meta['jars'][0]['file']] = b'not a ZIP archive'
        self.included_hashes[self.meta['jars'][0]['file']] = hashlib.sha256(b'not a ZIP archive').hexdigest()
        with self.assertRaises(BadZipFile):
            self.check()

    def test_wrong_identity_missing_dependency_and_recursive_jar_fail(self):
        module_id = 'fabric-api-base'
        good = {'id': module_id, 'version': self.policy['modules'][module_id]['version'],
                'environment': '*', 'license': 'Apache-2.0'}
        for changes, extras, error in [({'id': 'fabric-api'}, {}, 'identity'),
                ({'version': '999.0.0'}, {}, 'identity'),
                ({'depends': {'fabric-rendering-v1': '*'}}, {}, 'Missing nested dependency'),
                ({}, {'hidden.jar': b'bad'}, 'recursive')]:
            with self.subTest(error=error):
                self.set_module(module_id, {**good, **changes}, extras)
                with self.assertRaisesRegex(ValueError, error):
                    self.check()

    def test_content_change_with_metadata_intact_fails(self):
        module_id = 'fabric-api-base'
        path = self.module_path(module_id)
        before = self.included_hashes[path]
        with ZipFile(BytesIO(self.entries[path])) as source:
            metadata = json.loads(source.read('fabric.mod.json'))
        self.set_module(module_id, metadata, {'net/fabricmc/Altered.class': b'changed code'})
        self.included_hashes[path] = before
        with self.assertRaisesRegex(ValueError, 'differs from Gradle include input'):
            self.check()

    def test_external_api_breaks_boundary_is_required(self):
        self.meta['breaks'] = {'fabric-api': '<0.165.0'}
        with self.assertRaisesRegex(ValueError, 'compatibility boundary'):
            self.check()

    def test_recorded_input_set_must_match_allowlist(self):
        self.included_hashes['META-INF/jars/unreviewed.jar'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'Recorded include files'):
            self.check()

    def test_dependency_ranges_and_breaks_are_enforced(self):
        module_id = 'fabric-command-api-v2'
        metadata = {'id': module_id, 'version': self.policy['modules'][module_id]['version'],
                    'environment': '*', 'license': 'Apache-2.0'}
        for changes, error in [
                ({'depends': {'fabric-api-base': '>=999.0.0'}}, 'Unsatisfied nested dependency'),
                ({'breaks': {'fabric-api-base': '<999.0.0'}}, 'breaks bundled dependency'),
                ({'depends': {'fabric-api-base': '^2.0.0'}}, 'Unsupported bundled dependency predicate')]:
            with self.subTest(changes=changes):
                self.set_module(module_id, {**metadata, **changes})
                with self.assertRaisesRegex(ValueError, error):
                    self.check()
        self.set_module(module_id, {**metadata,
            'depends': {'fabric-api-base': ['>=999.0.0', '>=2.0.0 <3.0.0']},
            'breaks': {'fabric-api-base': '<2.0.0'}})
        self.check()

    def test_test_entrypoint_classes_and_missing_credits_fail(self):
        original = deepcopy(self.entries)
        for failure in ('entrypoint', 'class', 'license', 'notice', 'dependency'):
            with self.subTest(failure=failure):
                self.entries = deepcopy(original)
                meta = deepcopy(self.meta)
                if failure == 'entrypoint':
                    self.meta['entrypoints']['fabric-client-gametest'] = ['Test']
                elif failure == 'class':
                    self.entries['org/unallocated/smoke/Test.class'] = b'bad'
                elif failure == 'license':
                    del self.entries[LICENSE]
                elif failure == 'notice':
                    del self.entries[NOTICE]
                else:
                    self.meta['depends']['fabric-api'] = '*'
                with self.assertRaises((ValueError, KeyError)):
                    self.check()
                self.meta = meta

    def test_registered_mixin_package_rejects_logic_classes(self):
        from test_mixin_packages import class_file
        self.meta['mixins'] = [{'config': 'feature.mixins.json', 'environment': 'client'}]
        self.entries['feature.mixins.json'] = json.dumps({'package': 'example.mixin', 'client': ['FeatureMixin']})
        self.entries['example/mixin/FeatureMixin.class'] = class_file('example/mixin/FeatureMixin', mixin=True)
        self.check()
        self.entries['example/mixin/Helper.class'] = class_file('example/mixin/Helper')
        with self.assertRaisesRegex(ValueError, 'non-mixin class'):
            self.check()

    def test_clean_mod_directory_rejects_external_api(self):
        directory = Path(self.temp.name)
        (directory / 'player.jar').touch()
        verify_mod_directory(directory, {'player.jar'})
        (directory / 'fabric-api.jar').touch()
        with self.assertRaisesRegex(RuntimeError, 'clean installation'):
            verify_mod_directory(directory, {'player.jar'})


if __name__ == '__main__':
    unittest.main()
