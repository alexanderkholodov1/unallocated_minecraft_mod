"""Check package isolation using small class-file fixtures; no Java/game process is invoked."""

import io
import json
import struct
import unittest
from zipfile import ZipFile

from mixin_packages import MIXIN, class_annotations, verify_mixin_packages


def class_file(name, *, mixin=False, visible=False, values=False):
    def utf8(value):
        encoded = value.encode('utf-8')
        return b'\x01' + struct.pack('>H', len(encoded)) + encoded

    # Include the descriptor even in ordinary classes to rule out a plain byte-string search.
    pool = (utf8(name) + b'\x07\x00\x01' + utf8('java/lang/Object') + b'\x07\x00\x03'
            + utf8('RuntimeVisibleAnnotations' if visible else 'RuntimeInvisibleAnnotations') + utf8(MIXIN)
            + utf8('value') + utf8('Lnet/minecraft/client/gui/Gui;') + utf8('remap') + b'\x03\x00\x00\x00\x01')
    header = b'\xca\xfe\xba\xbe' + struct.pack('>HHH', 0, 69, 11) + pool
    body = struct.pack('>HHHHHHH', 0x21, 2, 4, 0, 0, 0, int(mixin))
    annotation = struct.pack('>HHH', 1, 6, 2 if values else 0)
    if values:
        # @Mixin(value = {Gui.class}, remap = true): class array and boolean constant.
        annotation += b'\x00\x07[\x00\x01c\x00\x08\x00\x09Z\x00\x0a'
    attribute = struct.pack('>HI', 5, len(annotation)) + annotation if mixin else b''
    return header + body + attribute


class MixinPackagesTest(unittest.TestCase):
    def check_jar(self, packages, classes, *, object_registration=True):
        buffer = io.BytesIO()
        with ZipFile(buffer, 'w') as jar:
            registrations = []
            for index, (package, names) in enumerate(packages):
                config = f'feature-{index}.mixins.json'
                registrations.append({'config': config, 'environment': 'client'} if object_registration else config)
                jar.writestr(config, json.dumps({'package': package, 'client': names}))
            jar.writestr('fabric.mod.json', json.dumps({'mixins': registrations}))
            for name, is_mixin in classes.items():
                jar.writestr(name.replace('.', '/') + '.class', class_file(name, mixin=is_mixin))
        with ZipFile(buffer) as jar:
            verify_mixin_packages(jar)

    def test_separate_mixin_package_accepts_logic_classes_outside_it(self):
        self.check_jar([('org.example.mixin.contract', ['ContractGuiMixin'])], {
            'org.example.mixin.contract.ContractGuiMixin': True,
            'org.example.contract.ContractClient': False,
        })

    def test_original_crash_layout_is_rejected_even_with_descriptor_in_constant_pool(self):
        with self.assertRaisesRegex(ValueError, 'non-mixin class.*ContractClient'):
            self.check_jar([('org.example.contract', ['ContractGuiMixin'])], {
                'org.example.contract.ContractGuiMixin': True,
                'org.example.contract.ContractClient': False,
            })

    def test_subpackages_cannot_hide_an_ordinary_class(self):
        with self.assertRaisesRegex(ValueError, 'non-mixin class.*Helper'):
            self.check_jar([('org.example.mixin', ['ContractMixin'])], {
                'org.example.mixin.ContractMixin': True,
                'org.example.mixin.internal.Helper': False,
            })

    def test_similarly_named_package_is_outside_the_namespace(self):
        self.check_jar([('org.example.mixin', ['ContractMixin'])], {
            'org.example.mixin.ContractMixin': True,
            'org.example.mixins.Helper': False,
        })

    def test_all_merged_configs_are_checked_including_string_registrations(self):
        with self.assertRaisesRegex(ValueError, 'feature-1.*non-mixin class'):
            self.check_jar([('org.example.contract.mixin', ['ContractMixin']),
                            ('org.example.interface.mixin', ['InterfaceMixin'])], {
                'org.example.contract.mixin.ContractMixin': True,
                'org.example.interface.mixin.InterfaceMixin': True,
                'org.example.interface.mixin.OptionsService': False,
            }, object_registration=False)

    def test_missing_registered_class_fails_before_startup(self):
        with self.assertRaisesRegex(ValueError, 'missing mixin class'):
            self.check_jar([('org.example.mixin', ['Typo'])], {})

    def test_class_level_annotations_are_read_in_both_retention_attributes(self):
        for visible in (False, True):
            with self.subTest(visible=visible):
                self.assertEqual(class_annotations(class_file('Example', mixin=True, visible=visible)), {MIXIN})
        self.assertEqual(class_annotations(class_file('Example')), set())

    def test_mixin_target_array_and_remap_flag_are_consumed(self):
        self.assertEqual(class_annotations(class_file('Example', mixin=True, values=True)), {MIXIN})

    def test_invalid_or_truncated_bytecode_fails_closed(self):
        for data in (b'not a class', class_file('Example', mixin=True)[:-1]):
            with self.subTest(data=data), self.assertRaises(ValueError):
                class_annotations(data)


if __name__ == '__main__':
    unittest.main()
