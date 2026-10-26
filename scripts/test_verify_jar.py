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
        self.entries = {LICENSE: 'Apache License', NOTICE: 'FabricMC Apache-2.0'}
        self.meta = {'id': 'unallocated', 'name': 'UNALLOCATED', 'version': 'test',
                     'entrypoints': {'main': ['example.Common'], 'client': ['example.Client']},
                     'depends': {'fabricloader': '>=0.20.1', 'minecraft': '~26.3', 'java': '>=25'},
                     'jars': []}
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

    def check(self):
        self.entries['fabric.mod.json'] = json.dumps(self.meta)
        self.artifact.write_bytes(archive(self.entries))
        return verify(self.artifact, self.props, self.policy)

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

    def test_repackaged_module_with_same_identity_passes(self):
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
        self.assertEqual(self.check(), hashlib.sha256(self.artifact.read_bytes()).hexdigest())

    def test_invalid_nested_archive_fails(self):
        self.entries[self.meta['jars'][0]['file']] = b'not a ZIP archive'
        with self.assertRaises(BadZipFile):
            self.check()

    def test_wrong_identity_missing_dependency_and_recursive_jar_fail(self):
        module_id = 'fabric-api-base'
        good = {'id': module_id, 'version': self.policy['modules'][module_id]['version'],
                'environment': '*', 'license': 'Apache-2.0'}
        for changes, extras, error in [({'id': 'fabric-api'}, {}, 'identity'),
                ({'version': '999.0.0'}, {}, 'identity'),
                ({'depends': {'fabric-registry-sync-v0': '*'}}, {}, 'Missing nested dependency'),
                ({}, {'hidden.jar': b'bad'}, 'recursive')]:
            with self.subTest(error=error):
                self.set_module(module_id, {**good, **changes}, extras)
                with self.assertRaisesRegex(ValueError, error):
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
