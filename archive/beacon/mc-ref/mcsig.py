#!/usr/bin/env python3
"""mcsig — real Minecraft 26.3 / Fabric API signatures for workers who cannot run Gradle.

Minecraft 26.x ships unobfuscated, so class and member names in the official jar are the names your code uses.
Usage:
  python3 -I mcsig.py class net.minecraft.client.gui.components.ChatComponent   # members of one class
  python3 -I mcsig.py find ChatComponent                                         # classes whose name matches
  python3 -I mcsig.py member addMessage [package-prefix]                         # methods/fields named like that
Jars searched: client-26.3.jar (includes common/server classes), fabric-api-0.165.0+26.3.jar (nested modules), the
Fabric client game test and game test API modules of that release, and Fabric Loader 0.20.1.
"""
import io, re, struct, sys, zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
JARS = [HERE / 'client-26.3.jar', HERE / 'fabric-api-0.165.0+26.3.jar', HERE / 'fabric-client-gametest-api-v1-6.0.11+131f8d475d.jar',
        HERE / 'fabric-gametest-api-v1-4.0.35+3434d6d95d.jar', HERE / 'fabric-loader-0.20.1.jar']
PRIM = {'B': 'byte', 'C': 'char', 'D': 'double', 'F': 'float', 'I': 'int', 'J': 'long', 'S': 'short', 'Z': 'boolean',
        'V': 'void'}


def iter_classes():
    def walk(zf, origin):
        for name in zf.namelist():
            if name.endswith('.class') and 'module-info' not in name:
                yield name[:-6].replace('/', '.'), (zf, name, origin)
            elif name.endswith('.jar'):
                inner = zipfile.ZipFile(io.BytesIO(zf.read(name)))
                yield from walk(inner, f'{origin}!{name}')
    for jar in JARS:
        if jar.exists():
            yield from walk(zipfile.ZipFile(jar), jar.name)


def jtype(desc, i=0):
    dims = 0
    while desc[i] == '[':
        dims += 1; i += 1
    c = desc[i]
    if c == 'L':
        j = desc.index(';', i)
        t, i = desc[i + 1:j].replace('/', '.'), j + 1
    else:
        t, i = PRIM[c], i + 1
    return t + '[]' * dims, i


def method_sig(name, desc):
    args, i = [], 1
    while desc[i] != ')':
        t, i = jtype(desc, i); args.append(t)
    ret, _ = jtype(desc, i + 1)
    return f'{ret} {name}({", ".join(args)})'


def parse(data):
    pos = 10
    count = struct.unpack('>H', data[8:10])[0]
    pool = [None] * count
    k = 1
    while k < count:
        tag = data[pos]
        if tag == 1:
            ln = struct.unpack('>H', data[pos + 1:pos + 3])[0]
            pool[k] = data[pos + 3:pos + 3 + ln].decode('utf-8', 'replace'); pos += 3 + ln
        elif tag in (7, 8, 16, 19, 20):
            pool[k] = ('ref', struct.unpack('>H', data[pos + 1:pos + 3])[0]); pos += 3
        elif tag == 15:
            pos += 4
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 5
        elif tag in (5, 6):
            pos += 9; k += 1
        else:
            raise ValueError(f'bad constant tag {tag}')
        k += 1
    u2 = lambda: struct.unpack('>H', data[pos:pos + 2])[0]
    access = u2(); pos += 2
    this = pool[pool[u2()][1]]; pos += 2
    sup_i = u2(); pos += 2
    sup = pool[pool[sup_i][1]] if sup_i else None
    n = u2(); pos += 2
    ifaces = []
    for _ in range(n):
        ifaces.append(pool[pool[u2()][1]]); pos += 2

    def members():
        nonlocal pos
        out = []
        n = u2(); pos += 2
        for _ in range(n):
            acc, ni, di = struct.unpack('>HHH', data[pos:pos + 6]); pos += 6
            ac = u2(); pos += 2
            for _ in range(ac):
                ln = struct.unpack('>I', data[pos + 2:pos + 6])[0]; pos += 6 + ln
            out.append((acc, pool[ni], pool[di]))
        return out
    fields = members()
    methods = members()
    return access, this, sup, ifaces, fields, methods


def mods(acc):
    names = [(1, 'public'), (4, 'protected'), (2, 'private'), (8, 'static'), (16, 'final'), (1024, 'abstract')]
    return ' '.join(n for bit, n in names if acc & bit)


def show(entry, qual):
    zf, name, origin = entry
    access, this, sup, ifaces, fields, methods = parse(zf.read(name))
    print(f'{mods(access)} class {qual}  [{origin}]')
    if sup: print(f'  extends {sup.replace("/", ".")}')
    for i in ifaces: print(f'  implements {i.replace("/", ".")}')
    for acc, n, d in fields:
        if acc & 0x1000: continue
        print(f'  field  {mods(acc)} {jtype(d)[0]} {n}')
    for acc, n, d in methods:
        if acc & 0x1000 or n == '<clinit>': continue
        print(f'  method {mods(acc)} {method_sig(n, d)}')


def main(argv):
    if len(argv) < 2 or argv[0] not in ('class', 'find', 'member'):
        print(__doc__); return 2
    mode, q = argv[0], argv[1]
    hits = 0
    for qual, entry in iter_classes():
        if mode == 'class' and qual == q:
            show(entry, qual); hits += 1
        elif mode == 'find' and re.search(q, qual.rsplit('.', 1)[-1]):
            print(qual); hits += 1
        elif mode == 'member':
            prefix = argv[2] if len(argv) > 2 else ''
            if not qual.startswith(prefix): continue
            try:
                _, _, _, _, fields, methods = parse(entry[0].read(entry[1]))
            except Exception:
                continue
            for acc, n, d in methods:
                if n == q and not acc & 0x1000:
                    print(f'{qual}: {mods(acc)} {method_sig(n, d)}'); hits += 1
            for acc, n, d in fields:
                if n == q: print(f'{qual}: field {jtype(d)[0]} {n}'); hits += 1
    if not hits: print('no match'); return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
