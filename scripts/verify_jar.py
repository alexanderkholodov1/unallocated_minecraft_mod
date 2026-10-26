"""Fail closed on player packaging; usable without Gradle/Java."""

import hashlib
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
LICENSE = 'META-INF/licenses/Fabric-API-Apache-2.0.txt'
NOTICE = 'META-INF/NOTICE-Fabric-API.txt'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def clean_archive(jar):
    names = jar.namelist()
    require(len(names) == len(set(names)), 'Duplicate ZIP entries')
    require(jar.testzip() is None, 'Corrupt ZIP entry')
    return names


def verify(artifact, properties, policy):
    modules = policy['modules']
    expected = {f'META-INF/jars/{key}-{value["version"]}.jar': key
                for key, value in modules.items()}
    with ZipFile(artifact) as jar:
        names = clean_archive(jar)
        metadata_text = jar.read('fabric.mod.json').decode()
        require('${' not in metadata_text, 'Unexpanded metadata')
        metadata = json.loads(metadata_text)
        require(metadata['id'] == 'unallocated' and metadata['name'] == 'UNALLOCATED', 'Wrong mod')
        require(metadata['version'] == properties['version'], 'Wrong version')
        require(set(metadata['entrypoints']) == {'main', 'client'}, 'Test entrypoint leaked')
        declared = [entry['file'] for entry in metadata.get('jars', [])]
        require(len(declared) == len(set(declared)) and set(declared) == set(expected),
                'Declared nested jars differ from allowlist')
        require({name for name in names if name.lower().endswith('.jar')} == set(expected),
                'Actual nested jars differ from allowlist')
        require(not any(name.startswith('org/unallocated/smoke/') for name in names),
                'Smoke helper leaked into player jar')
        depends = {
            'fabricloader': '>=' + properties['loader_version'],
            'minecraft': '~' + properties['minecraft_version'], 'java': '>=25',
            **{key: '>=' + value['version'] for key, value in modules.items()},
        }
        require(metadata['depends'] == depends, 'Wrong player dependencies')
        require('Apache License' in jar.read(LICENSE).decode(), 'Missing Apache license')
        notice = jar.read(NOTICE).decode()
        require('FabricMC' in notice and 'Apache-2.0' in notice, 'Missing Fabric attribution')
        for path, module_id in expected.items():
            raw = jar.read(path)
            module = modules[module_id]
            # Loom packaging need not preserve Maven archive bytes; validate the contained mod.
            with ZipFile(BytesIO(raw)) as nested:
                inner_names = clean_archive(nested)
                inner = json.loads(nested.read('fabric.mod.json'))
                require(inner['id'] == module_id and inner['version'] == module['version'],
                        'Wrong nested module identity')
                require(inner['environment'] == module['environment'], 'Wrong module environment')
                require(inner['license'] == 'Apache-2.0', 'Wrong module license')
                require('Apache License' in nested.read('LICENSE-' + module_id).decode(),
                        'Module license missing')
                require(not inner.get('jars') and not any(n.lower().endswith('.jar') for n in inner_names),
                        'Unreviewed recursive nested jar')
                require(not any('gametest' in key or 'smoke' in key for key in inner.get('entrypoints', {})),
                        'Test entrypoint in nested module')
                for dependency in inner.get('depends', {}):
                    require(dependency in modules or dependency in {'minecraft', 'java', 'fabricloader'},
                            'Missing nested dependency: ' + dependency)
        for side in ('main', 'client'):
            for entrypoint in metadata['entrypoints'][side]:
                bytecode = jar.read(entrypoint.replace('.', '/') + '.class')
                require(bytecode[:4] == bytes.fromhex('cafebabe') and
                        int.from_bytes(bytecode[6:8], 'big') == 69, 'Expected Java 25 entrypoint')
    require(artifact.stat().st_size <= 2 * 1024 * 1024, 'Player jar exceeds 2 MiB packaging budget')
    return hashlib.sha256(artifact.read_bytes()).hexdigest()


def main():
    from smoke_test import properties
    props = properties()
    artifact = ROOT / 'build/libs' / f'unallocated-{props["version"]}.jar'
    policy = json.loads((ROOT / 'fabric-api-modules.json').read_text())
    digest = verify(artifact, props, policy)
    artifact.with_suffix('.jar.sha256').write_text(f'{digest}  {artifact.name}\n')
    print(f'Verified {artifact}: {artifact.stat().st_size:,} bytes; exactly {len(policy["modules"])} approved nested modules')


if __name__ == '__main__':
    main()
