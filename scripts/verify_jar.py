"""Fail closed on player packaging; usable without Gradle/Java."""

import hashlib
from io import BytesIO
import json
import re
from pathlib import Path
from zipfile import ZipFile

from mixin_packages import verify_mixin_packages

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


def stable_version(value):
    # Bundled Fabric modules currently use stable numeric versions plus build metadata.
    match = re.fullmatch(r'(\d+(?:\.\d+)*)(?:\+[\w.-]+)?', value)
    require(match is not None, 'Unsupported bundled version: ' + value)
    parts = tuple(int(part) for part in match[1].split('.'))
    return parts + (0,) * max(0, 4 - len(parts))


def matches(version, predicate):
    if isinstance(predicate, list):
        return any([matches(version, branch) for branch in predicate])
    require(isinstance(predicate, str) and bool(predicate.strip()), 'Invalid version predicate')
    actual = stable_version(version)
    for term in predicate.split():
        if term == '*':
            continue
        match = re.fullmatch(r'(>=|<=|>|<|=)?(\d+(?:\.\d+)*(?:\+[\w.-]+)?)', term)
        require(match is not None, 'Unsupported bundled dependency predicate: ' + term)
        wanted = stable_version(match[2])
        comparison = (actual > wanted) - (actual < wanted)
        if not {'>=': comparison >= 0, '<=': comparison <= 0, '>': comparison > 0,
                '<': comparison < 0, '=': comparison == 0}[match[1] or '=']:
            return False
    return True


def verify(artifact, properties, policy, included_hashes):
    modules = policy['modules']
    expected = {f'META-INF/jars/{key}-{value["version"]}.jar': key
                for key, value in modules.items()}
    require(set(included_hashes) == set(expected), 'Recorded include files differ from allowlist')
    from author_template import TEMPLATE_ENTRY, read_world_zip
    with ZipFile(artifact) as jar:
        names = clean_archive(jar)
        require(names.count(TEMPLATE_ENTRY) == 1, 'Expected exactly one bundled world template')
        read_world_zip(jar.read(TEMPLATE_ENTRY))
        metadata_text = jar.read('fabric.mod.json').decode()
        require('${' not in metadata_text, 'Unexpanded metadata')
        metadata = json.loads(metadata_text)
        declared_mixins = [entry if isinstance(entry, str) else entry['config']
                           for entry in metadata.get('mixins', [])]
        require(len(declared_mixins) == len(set(declared_mixins)), 'Duplicate mixin registration')
        require(set(declared_mixins) == {name for name in names if name.endswith('.mixins.json')},
                'Unregistered or missing mixin configuration')
        verify_mixin_packages(jar)
        require(metadata['id'] == 'unallocated' and metadata['name'] == 'UNALLOCATED', 'Wrong mod')
        require(metadata['version'] == properties['version'], 'Wrong version')
        require(set(metadata['entrypoints']) == {'main', 'client'}, 'Test entrypoint leaked')
        declared = [entry['file'] for entry in metadata.get('jars', [])]
        require(len(declared) == len(set(declared)) and set(declared) == set(expected),
                'Declared nested jars differ from allowlist')
        require({name for name in names if name.lower().endswith('.jar')} == set(expected),
                'Actual nested jars differ from allowlist')
        require(not any(name.startswith('org/unallocated/smoke/')
                        or re.match(r'org/unallocated/client/mixin/[^/]*smoke/', name)
                        or name == 'unallocated-interface-smoke.mixins.json' for name in names),
                'Smoke helper leaked into player jar')
        depends = {
            'fabricloader': '>=' + properties['loader_version'],
            'minecraft': properties['minecraft_version'], 'java': '>=25',
            **{key: '>=' + value['version'] for key, value in modules.items()},
        }
        require(metadata['depends'] == depends, 'Wrong player dependencies')
        require(metadata.get('breaks') == {'fabric-api': '<' + policy['minimum_external_api_version']},
                'Wrong external Fabric API compatibility boundary')
        require('Apache License' in jar.read(LICENSE).decode(), 'Missing Apache license')
        notice = jar.read(NOTICE).decode()
        require('FabricMC' in notice and 'Apache-2.0' in notice, 'Missing Fabric attribution')
        for path, module_id in expected.items():
            raw = jar.read(path)
            module = modules[module_id]
            require(hashlib.sha256(raw).hexdigest() == included_hashes[path],
                    'Nested content differs from Gradle include input: ' + path)
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
                for dependency, predicate in inner.get('depends', {}).items():
                    require(dependency in modules or dependency in {'minecraft', 'java', 'fabricloader'},
                            'Missing nested dependency: ' + dependency)
                    if dependency in modules:
                        require(matches(modules[dependency]['version'], predicate),
                                'Unsatisfied nested dependency: ' + dependency)
                for incompatible, predicate in inner.get('breaks', {}).items():
                    if incompatible in modules:
                        require(not matches(modules[incompatible]['version'], predicate),
                                'Nested module breaks bundled dependency: ' + incompatible)
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
    included_hashes = json.loads((ROOT / 'build/reports/included-jars.json').read_text())
    digest = verify(artifact, props, policy, included_hashes)
    artifact.with_suffix('.jar.sha256').write_text(f'{digest}  {artifact.name}\n')
    print(f'Verified {artifact}: {artifact.stat().st_size:,} bytes; exactly {len(policy["modules"])} approved nested modules')


if __name__ == '__main__':
    main()
