"""Reject ordinary classes in every mixin namespace registered by the player JAR."""

import argparse
import json
from zipfile import ZipFile

MIXIN = 'Lorg/spongepowered/asm/mixin/Mixin;'


class ClassReader:
    def __init__(self, data):
        self.data = data
        self.position = 0

    def take(self, length):
        end = self.position + length
        if end > len(self.data):
            raise ValueError('Truncated class file')
        result = self.data[self.position:end]
        self.position = end
        return result

    def number(self, length):
        return int.from_bytes(self.take(length), 'big')

    def attributes(self, pool):
        for _ in range(self.number(2)):
            name = pool[self.number(2)]
            yield name, self.take(self.number(4))


def class_annotations(data):
    """Read class annotations without loading a mixin class or requiring Java/dependencies."""
    reader = ClassReader(data)
    if reader.take(4) != b'\xca\xfe\xba\xbe':
        raise ValueError('Invalid class file')
    reader.take(4)  # minor/major version
    pool = [None] * reader.number(2)
    index = 1
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4,
             12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < len(pool):
        tag = reader.number(1)
        if tag == 1:
            # Names used here are ASCII; other JVM modified-UTF8 strings can be ignored.
            pool[index] = reader.take(reader.number(2)).decode('utf-8', errors='replace')
        elif tag in sizes:
            reader.take(sizes[tag])
        else:
            raise ValueError(f'Unknown constant-pool tag {tag}')
        index += 2 if tag in (5, 6) else 1
    reader.take(6)  # access flags, this class, superclass
    reader.take(2 * reader.number(2))  # interfaces
    for _ in range(2):  # fields, methods
        for _ in range(reader.number(2)):
            reader.take(6)
            list(reader.attributes(pool))

    def annotation(source):
        descriptor = pool[source.number(2)]
        for _ in range(source.number(2)):
            source.take(2)  # element name
            element(source)
        return descriptor

    def element(source):
        tag = chr(source.number(1))
        if tag in 'BCDFIJSZsc':
            source.take(2)
        elif tag == 'e':
            source.take(4)
        elif tag == '@':
            annotation(source)
        elif tag == '[':
            for _ in range(source.number(2)):
                element(source)
        else:
            raise ValueError(f'Unknown annotation element tag {tag}')

    result = set()
    for name, value in reader.attributes(pool):
        if name in ('RuntimeInvisibleAnnotations', 'RuntimeVisibleAnnotations'):
            source = ClassReader(value)
            for _ in range(source.number(2)):
                result.add(annotation(source))
    return result


def verify_mixin_packages(jar):
    metadata = json.loads(jar.read('fabric.mod.json'))
    classes = [name for name in jar.namelist() if name.endswith('.class')]
    for registration in metadata.get('mixins', []):
        config_name = registration if isinstance(registration, str) else registration['config']
        config = json.loads(jar.read(config_name))
        package = config.get('package')
        if not isinstance(package, str) or not package.strip('.'):
            raise ValueError(f'{config_name}: expected a dedicated mixin package')
        prefix = package.rstrip('.').replace('.', '/') + '/'
        for section in ('mixins', 'client', 'server'):
            for name in config.get(section, []):
                expected = prefix + name.replace('.', '/') + '.class'
                if expected not in classes:
                    raise ValueError(f'{config_name}: missing mixin class {expected}')
        for name in classes:
            if name.startswith(prefix) and MIXIN not in class_annotations(jar.read(name)):
                raise ValueError(f'{config_name}: non-mixin class {name} is inside mixin package {package}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar')
    args = parser.parse_args()
    with ZipFile(args.jar) as artifact:
        verify_mixin_packages(artifact)
