"""Release-world checks and deterministic packaging, without starting Java."""
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile
from author_template import (ROOT, TEMPLATE_ENTRY, SESSION_ENTRY, MAP_RULES, deterministic_zip, export_world, inject_template,
                             nbt_read, nbt_write, read_world_zip, validate_world)


def world_entries():
    return {
        'UNALLOCATED/level.dat': nbt_write('', {'Data': (10, {
            'GameType': (3, 2), 'allowCommands': (1, 0), 'LevelName': (8, 'UNALLOCATED')})}),
        SESSION_ENTRY: nbt_write('', {'data': (10, {
            'armed': (1, 1), 'started': (1, 0), 'finished': (1, 0)})}),
        'UNALLOCATED/data/minecraft/game_rules.dat': nbt_write('', {'data': (10, MAP_RULES.copy())}),
    }


class AuthorTemplateTest(unittest.TestCase):
    def test_nbt_roundtrip_preserves_every_type(self):
        data = {str(k): (k, v) for k, v in [(1, 1), (2, 300), (3, 42), (4, 2**40),
            (5, .25), (6, .125), (7, b'bytes'), (8, 'Spanish: sesión'), (9, (8, ['a', 'b'])),
            (10, {'nested': (3, 9)}), (11, [1, -2]), (12, [2**40, -3])]}
        self.assertEqual(nbt_read(nbt_write('root', data)), ('root', data))

    def test_zip_is_sorted_and_reproducible_with_fixed_timestamps(self):
        entries = world_entries()
        raw = deterministic_zip(entries)
        self.assertEqual(raw, deterministic_zip(dict(reversed(list(entries.items())))))
        self.assertEqual(read_world_zip(raw), entries)
        with ZipFile(BytesIO(raw)) as archive:
            self.assertEqual(archive.namelist(), sorted(entries))
            self.assertTrue(all(info.date_time == (1980, 1, 1, 0, 0, 0) for info in archive.infolist()))

    def test_unarmed_started_finished_and_cheats_are_rejected(self):
        for field, number in [('armed', 0), ('started', 1), ('finished', 1)]:
            entries = world_entries(); path = SESSION_ENTRY
            name, root = nbt_read(entries[path]); root['data'][1][field] = (1, number)
            entries[path] = nbt_write(name, root)
            with self.assertRaisesRegex(ValueError, 'armed and unstarted'): validate_world(entries)
        entries = world_entries(); name, root = nbt_read(entries['UNALLOCATED/level.dat'])
        root['Data'][1]['allowCommands'] = (1, 1)
        entries['UNALLOCATED/level.dat'] = nbt_write(name, root)
        with self.assertRaisesRegex(ValueError, 'cheats off'): validate_world(entries)

    def test_dimension_session_path_and_map_rules_are_required(self):
        entries = world_entries()
        entries['UNALLOCATED/data/unallocated/unallocated_session.dat'] = entries.pop(SESSION_ENTRY)
        with self.assertRaisesRegex(ValueError, 'Overworld'): validate_world(entries)
        entries = world_entries(); path = 'UNALLOCATED/data/minecraft/game_rules.dat'
        name, root = nbt_read(entries[path]); root['data'][1]['minecraft:spawn_mobs'] = (1, 1)
        entries[path] = nbt_write(name, root)
        with self.assertRaisesRegex(ValueError, 'map rules'): validate_world(entries)

    def test_missing_and_unsafe_archive_paths_are_rejected(self):
        for extra in ['UNALLOCATED/../escape', '/absolute', 'OTHER/level.dat',
                      'UNALLOCATED/session.lock', 'UNALLOCATED/playerdata/player.dat']:
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                read_world_zip(deterministic_zip({**world_entries(), extra: b'bad'}))
        with self.assertRaisesRegex(ValueError, 'session file'):
            validate_world({'UNALLOCATED/level.dat': world_entries()['UNALLOCATED/level.dat']})

    def test_export_removes_private_files_enforces_cheats_and_injects_identical_zip(self):
        (ROOT / '.tmp').mkdir(exist_ok=True)
        with TemporaryDirectory(dir=ROOT / '.tmp') as temp:
            directory = Path(temp)
            for name, raw in world_entries().items():
                target = directory / name; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
            world = directory / 'UNALLOCATED'
            for name in ['session.lock', 'playerdata/a.dat', 'stats/a.json', 'advancements/a.json', 'logs/latest.log']:
                target = world / name; target.parent.mkdir(parents=True, exist_ok=True); target.write_text('private')
            name, root = nbt_read((world / 'level.dat').read_bytes())
            root['Data'][1]['allowCommands'] = (1, 1); root['Data'][1]['Player'] = (10, {'secret': (8, 'private')})
            (world / 'level.dat').write_bytes(nbt_write(name, root))
            raw = export_world(world); self.assertEqual(set(read_world_zip(raw)), set(world_entries()))
            self.assertNotIn('Player', nbt_read(read_world_zip(raw)['UNALLOCATED/level.dat'])[1]['Data'][1])
            jar = directory / 'player.jar'; jar.write_bytes(deterministic_zip({'other.txt': b'player'}))
            inject_template(jar, raw); inject_template(jar, raw)
            with ZipFile(jar) as archive:
                self.assertEqual(archive.namelist().count(TEMPLATE_ENTRY), 1)
                self.assertEqual(archive.read(TEMPLATE_ENTRY), raw)
                self.assertEqual(archive.read('other.txt'), b'player')


if __name__ == '__main__': unittest.main()
