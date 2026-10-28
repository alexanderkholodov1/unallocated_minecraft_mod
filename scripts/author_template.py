"""CI-only release world authoring, deterministic export and final player JAR injection."""
import gzip
import hashlib
from io import BytesIO
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import struct
import sys
from urllib.request import Request, urlopen
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
TEMPLATE_ENTRY = 'unallocated/template/world.zip'
WORLD_ZIP = ROOT / 'build/template/UNALLOCATED-world.zip'
EXCLUDED = {'playerdata', 'stats', 'advancements', 'logs'}


def nbt_read(raw):
    """Preserve NBT types while inspecting/editing level.dat without Java or third party tools."""
    stream = BytesIO(gzip.decompress(raw))
    def unpack(fmt): return struct.unpack('>' + fmt, stream.read(struct.calcsize('>' + fmt)))[0]
    def string(): return stream.read(unpack('H')).decode('utf-8')
    def payload(kind):
        if kind in range(1, 7): return unpack({1:'b', 2:'h', 3:'i', 4:'q', 5:'f', 6:'d'}[kind])
        if kind == 7: return stream.read(unpack('i'))
        if kind == 8: return string()
        if kind == 9:
            child, length = unpack('B'), unpack('i')
            return child, [payload(child) for _ in range(length)]
        if kind == 10:
            result = {}
            while (child := unpack('B')):
                name = string(); result[name] = (child, payload(child))
            return result
        if kind in (11, 12): return [unpack('i' if kind == 11 else 'q') for _ in range(unpack('i'))]
        raise ValueError('Unknown NBT type')
    if unpack('B') != 10: raise ValueError('NBT root must be compound')
    name = string(); data = payload(10)
    if stream.read(): raise ValueError('Trailing NBT data')
    return name, data


def nbt_write(name, data):
    stream = BytesIO()
    def pack(fmt, value): stream.write(struct.pack('>' + fmt, value))
    def string(value):
        raw = value.encode('utf-8'); pack('H', len(raw)); stream.write(raw)
    def payload(kind, value):
        if kind in range(1, 7): pack({1:'b', 2:'h', 3:'i', 4:'q', 5:'f', 6:'d'}[kind], value)
        elif kind == 7: pack('i', len(value)); stream.write(value)
        elif kind == 8: string(value)
        elif kind == 9:
            child, values = value; pack('B', child); pack('i', len(values))
            for item in values: payload(child, item)
        elif kind == 10:
            for key, (child, item) in value.items():
                pack('B', child); string(key); payload(child, item)
            pack('B', 0)
        elif kind in (11, 12):
            pack('i', len(value))
            for item in value: pack('i' if kind == 11 else 'q', item)
        else: raise ValueError('Unknown NBT type')
    pack('B', 10); string(name); payload(10, data)
    return gzip.compress(stream.getvalue(), mtime=0)


def value(data, key): return data[key][1]


def validate_world(entries):
    if 'UNALLOCATED/level.dat' not in entries: raise ValueError('Template missing UNALLOCATED/level.dat')
    sessions = [name for name in entries if name.endswith('/unallocated_session.dat')]
    if len(sessions) != 1: raise ValueError('Template needs exactly one session file')
    _, saved = nbt_read(entries[sessions[0]])
    state = value(saved, 'data')
    if not value(state, 'armed') or value(state, 'started') or value(state, 'finished'):
        raise ValueError('Template session must be armed and unstarted, unfinished')
    _, saved = nbt_read(entries['UNALLOCATED/level.dat']); level = value(saved, 'Data')
    if value(level, 'GameType') != 2 or value(level, 'allowCommands') != 0:
        raise ValueError('Template requires Adventure and cheats off')
    if value(level, 'LevelName') != 'UNALLOCATED': raise ValueError('Wrong template level name')


def read_world_zip(raw):
    with ZipFile(BytesIO(raw)) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)): raise ValueError('Duplicate template ZIP entries')
        if archive.testzip() is not None: raise ValueError('Corrupt template ZIP')
        entries = {}
        for info in archive.infolist():
            name = info.filename; path = PurePosixPath(name)
            if (not name.startswith('UNALLOCATED/') or '..' in path.parts or '\\' in name
                    or (info.external_attr >> 16) & 0o170000 == 0o120000):
                raise ValueError('Unsafe template ZIP path')
            if info.is_dir(): continue
            if path.name == 'session.lock' or EXCLUDED.intersection(path.parts):
                raise ValueError('Template contains excluded player/lock/log data')
            entries[name] = archive.read(info)
    validate_world(entries)
    return entries


def deterministic_zip(entries):
    output = BytesIO()
    with ZipFile(output, 'w', compression=ZIP_DEFLATED, compresslevel=9) as archive:
        for name, raw in sorted(entries.items()):
            info = ZipInfo(name, (1980, 1, 1, 0, 0, 0)); info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, raw, compresslevel=9)
    return output.getvalue()


def export_world(world):
    entries = {}
    for path in sorted(world.rglob('*')):
        relative = path.relative_to(world)
        if path.is_symlink(): raise ValueError('Symlink in authored world')
        if (not path.is_file() or path.name in {'session.lock', 'level.dat_old'}
                or EXCLUDED.intersection(relative.parts)): continue
        entries['UNALLOCATED/' + relative.as_posix()] = path.read_bytes()
    # Dedicated console permissions are not judge permissions. Enforce the integrated save explicitly.
    name, saved = nbt_read(entries['UNALLOCATED/level.dat']); data = value(saved, 'Data')
    data['allowCommands'] = (1, 0); data['GameType'] = (3, 2); data['LevelName'] = (8, 'UNALLOCATED')
    data.pop('Player', None)
    entries['UNALLOCATED/level.dat'] = nbt_write(name, saved)
    validate_world(entries)
    return deterministic_zip(entries)


def inject_template(artifact, raw):
    read_world_zip(raw)
    with ZipFile(artifact) as source:
        entries = {info.filename: source.read(info) for info in source.infolist()
                   if info.filename != TEMPLATE_ENTRY}
    entries[TEMPLATE_ENTRY] = raw
    artifact.write_bytes(deterministic_zip(entries))


def author():
    from smoke_test import SMOKE, properties, java_executable, monitor, server_evidence, verify_mod_directory
    props = properties(); artifact = ROOT / 'build/libs' / f'unallocated-{props["version"]}.jar'
    directory = SMOKE / 'template'; directory.mkdir(parents=True, exist_ok=False)
    (directory / 'mods').mkdir(); shutil.copyfile(artifact, directory / 'mods' / artifact.name)
    verify_mod_directory(directory / 'mods', {artifact.name})
    (directory / 'eula.txt').write_text('eula=true\n')
    (directory / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\nlevel-name=UNALLOCATED\n'
        'level-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:air","height":1}],'
        '"biome":"unallocated:lobby","structure_overrides":[]}\ngenerate-structures=false\n'
        'gamemode=adventure\nforce-gamemode=true\ndifficulty=normal\nview-distance=2\nsimulation-distance=2\n'
        'max-players=1\npause-when-empty-seconds=-1\n')
    override = ROOT / 'template/override/world.zip'
    if override.is_file():
        entries = read_world_zip(override.read_bytes())
        for name, raw in entries.items():
            target = directory / name; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
    downloads = ROOT / '.tmp/smoke-downloads' / f'{props["minecraft_version"]}-{props["loader_version"]}-1.1.2'
    downloads.mkdir(parents=True, exist_ok=True); (directory / '.fabric').mkdir()
    for relative in ('.fabric/server', 'libraries', 'versions'):
        shared = downloads / relative; shared.mkdir(parents=True, exist_ok=True)
        (directory / relative).symlink_to(shared, target_is_directory=True)
    launcher = downloads / 'fabric-server-launch.jar'
    if not launcher.is_file():
        url = f'https://meta.fabricmc.net/v2/versions/loader/{props["minecraft_version"]}/{props["loader_version"]}/1.1.2/server/jar'
        with urlopen(Request(url, headers={'User-Agent':'UNALLOCATED-CI'}), timeout=60) as response:
            launcher.write_bytes(response.read())
    temp = directory / 'temp'; temp.mkdir()
    steps = [] if override.is_file() else [
        ('unallocated build all', [r'UNALLOCATED map built section=all', r'UNALLOCATED map verified section=all']),
        ('unallocated story build', [r'UNALLOCATED story build ok'])]
    steps += [('unallocated story check', [r'UNALLOCATED story check ok']),
              ('unallocated arm', [r'UNALLOCATED session armed']),
              ('save-all flush', [r'Saved the game'])]
    monitor([java_executable(), '-Xms256M', '-Xmx1G', '-Djava.awt.headless=true',
             f'-Djava.io.tmpdir={temp}', '-jar', str(launcher), 'nogui'], directory,
            SMOKE / 'template-console.log', server_evidence() + [pattern for _, patterns in steps for pattern in patterns],
            timeout=240, stop_when_ready=True, startup_required=server_evidence(), command_steps=steps)
    raw = export_world(directory / 'UNALLOCATED')
    WORLD_ZIP.parent.mkdir(parents=True, exist_ok=True); WORLD_ZIP.write_bytes(raw)
    inject_template(artifact, raw)
    print(f'UNALLOCATED template bytes={len(raw)} jar bytes={artifact.stat().st_size} sha256={hashlib.sha256(artifact.read_bytes()).hexdigest()}')


if __name__ == '__main__':
    if os.environ.get('CI') != 'true': sys.exit('Template authoring runs only in CI')
    author()
