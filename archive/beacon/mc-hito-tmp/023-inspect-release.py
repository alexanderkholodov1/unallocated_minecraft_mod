"""Inspect downloaded CI release bytes without Java or game execution."""
import hashlib
import json
from pathlib import Path
import struct
import sys
from zipfile import ZipFile
sys.path.insert(0, str(Path.cwd() / 'scripts'))
from author_template import TEMPLATE_ENTRY, read_world_zip
root = Path(sys.argv[1])
manifest_path = next(root.rglob('release.json'))
root = manifest_path.parent
manifest = json.loads(manifest_path.read_text())
jar = root / manifest['jar']; world = root / manifest['world_zip']
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
assert digest == manifest['sha256']
assert jar.with_suffix('.jar.sha256').read_text() == f'{digest}  {jar.name}\n'
with ZipFile(jar) as archive:
    assert archive.namelist().count(TEMPLATE_ENTRY) == 1
    assert archive.read(TEMPLATE_ENTRY) == world.read_bytes()
    assert json.loads(archive.read('fabric.mod.json'))['version'] == manifest['version']
    shipped = json.loads(archive.read('data/unallocated/story/story.json'))
world_files = read_world_zip(world.read_bytes())
pngs = list((root / 'screenshots').glob('*.png'))
assert pngs and all(p.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') for p in pngs)
assert (root / 'screenshots/unallocated-title-button.png').is_file()
video = root / 'video/gameplay.mp4'
result = {'manifest': manifest, 'jar_bytes': jar.stat().st_size, 'template_bytes': world.stat().st_size,
          'world_files': len(world_files), 'screenshots': len(pngs), 'shipped_runs': shipped['autoplay']['runs']}
if video.is_file():
    raw = video.read_bytes(); assert len(raw) <= 25_000_000
    atoms = []; pos = 0
    while pos < len(raw):
        size, name = struct.unpack('>I4s', raw[pos:pos+8]); header = 8
        if size == 1: size = struct.unpack('>Q', raw[pos+8:pos+16])[0]; header = 16
        if size == 0: size = len(raw) - pos
        assert size >= header and pos + size <= len(raw)
        atoms.append(name.decode('ascii')); pos += size
    assert pos == len(raw) and {'ftyp', 'moov', 'mdat'}.issubset(atoms)
    assert b'avc1' in raw and b'avcC' in raw
    result.update(video_bytes=len(raw), mp4_atoms=atoms, codec='H.264/AVC')
else: result['video_bytes'] = None
print(json.dumps(result, indent=2))
