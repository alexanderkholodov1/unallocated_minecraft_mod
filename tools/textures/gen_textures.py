"""Generate every texture UNALLOCATED ships, from scratch and deterministically.

Run: python tools/textures/gen_textures.py
Writes PNGs (and animation .mcmeta files) under src/main/resources/assets/unallocated/textures/.
The Reclaimer's UV regions mirror ReclaimerModel.createLayer(); keep both in sync.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'src/main/resources/assets/unallocated/textures'

# Pale, bruised and grey skins: the Reclaimer is made of what it has data of.
SKINS = [(214, 196, 178), (188, 160, 136), (152, 118, 96), (112, 82, 66), (205, 200, 196), (136, 112, 118), (170, 150, 128)]


def clamp(v):
    return max(0, min(255, int(v)))


def box_faces(u, v, w, h, d):
    """Vanilla box UV layout: name -> (x, y, width, height)."""
    return {
        'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h),
    }


def skin_field(img, rng, region, seeds=None):
    x0, y0, x1, y1 = region
    pts = seeds or [(rng.uniform(x0, x1), rng.uniform(y0, y1), rng.choice(SKINS)) for _ in range(max(3, (x1 - x0) * (y1 - y0) // 90))]
    px = img.load()
    owner = {}
    for y in range(y0, y1):
        for x in range(x0, x1):
            best = min(range(len(pts)), key=lambda i: (pts[i][0] - x) ** 2 + (pts[i][1] - y) ** 2 * 1.4)
            owner[(x, y)] = best
            r, g, b = pts[best][2]
            n = rng.uniform(-12, 12)
            mottled = 10 * math.sin(x * 0.9 + y * 0.4) * math.sin(y * 1.3 - x * 0.2)
            px[x, y] = (clamp(r + n + mottled), clamp(g + n + mottled * 0.8), clamp(b + n + mottled * 0.7), 255)
    # Seams between victims' skin, with stitches across them.
    for (x, y), o in owner.items():
        right, below = owner.get((x + 1, y)), owner.get((x, y + 1))
        if (right is not None and right != o) or (below is not None and below != o):
            px[x, y] = (58, 22, 24, 255)
            if (x + y) % 3 == 0:
                for dx, dy in ((-1, -1), (1, 1), (-1, 1), (1, -1)):
                    if (x + dx, y + dy) in owner:
                        px[x + dx, y + dy] = (36, 14, 16, 255)


def veins(img, rng, region, count):
    x0, y0, x1, y1 = region
    px = img.load()
    for _ in range(count):
        x, y = rng.uniform(x0, x1), rng.uniform(y0, y1)
        a = rng.uniform(0, math.tau)
        for _ in range(rng.randint(4, 12)):
            if x0 <= x < x1 and y0 <= y < y1:
                r, g, b, _a = px[int(x), int(y)]
                px[int(x), int(y)] = (clamp(r * 0.55 + 30), clamp(g * 0.45), clamp(b * 0.55 + 25), 255)
            a += rng.uniform(-0.7, 0.7)
            x += math.cos(a)
            y += math.sin(a)


def glitch(img, rng, region, bands):
    x0, y0, x1, y1 = region
    px = img.load()
    for _ in range(bands):
        y = rng.randrange(y0, y1)
        shift = rng.randint(-6, 6)
        tint = rng.choice([(0.6, 1.25, 1.3), (1.3, 0.6, 1.25), (0.5, 0.5, 0.5)])
        row = [px[x, y] for x in range(x0, x1)]
        for i, x in enumerate(range(x0, x1)):
            r, g, b, a = row[(i - shift) % len(row)]
            px[x, y] = (clamp(r * tint[0]), clamp(g * tint[1]), clamp(b * tint[2]), a)


def bone(img, rng, region, dark=False):
    x0, y0, x1, y1 = region
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            base = (96, 84, 80) if dark else (168, 150, 132)
            n = rng.uniform(-14, 14)
            stripe = -30 if (x * 7 + y * 3) % 11 == 0 else 0
            knuckle = -26 if (x - x0) % 16 in (0, 15) else 0
            px[x, y] = (clamp(base[0] + n + stripe + knuckle), clamp(base[1] + n + stripe + knuckle), clamp(base[2] + n + stripe + knuckle), 255)


def face(img, eyes, rect, rng, skin, kind):
    """Paint a face in the north face rect; matching glow pixels go to the eyes layer."""
    x, y, w, h = rect
    px, ex = img.load(), eyes.load()
    for j in range(h):
        for i in range(w):
            n = rng.uniform(-8, 8)
            px[x + i, y + j] = (clamp(skin[0] + n), clamp(skin[1] + n), clamp(skin[2] + n), 255)
    ey = y + h // 3
    if kind == 'reclaimer':
        # Hollow sockets with pinpoint lights, a vertical split mouth with teeth.
        for cx in (x + 2, x + w - 4):
            for dx in range(2):
                for dy in range(3):
                    px[cx + dx, ey + dy] = (8, 4, 6, 255)
            ex[cx + 1, ey + 1] = (255, 240, 235, 255)
            ex[cx, ey + 1] = (190, 20, 30, 255)
        mx = x + w // 2
        for dy in range(ey + 3, y + h):
            px[mx, dy] = (20, 6, 8, 255)
            if dy % 2 == 0:
                px[mx - 1, dy] = (226, 214, 190, 255)
                px[mx + 1, dy] = (226, 214, 190, 255)
        for dx in range(-2, 3):
            px[mx + dx, y + h - 1] = (40, 10, 12, 255)
    else:
        hair = rng.choice([(40, 28, 20), (20, 16, 14), (92, 60, 30), (180, 160, 120)])
        for i in range(w):
            px[x + i, y] = (*hair, 255)
            if rng.random() < 0.6:
                px[x + i, y + 1] = (*hair, 255)
        for cx in (x + 1, x + w - 2):
            if kind == 'blind':
                px[cx, ey] = (30, 10, 12, 255)
            else:
                px[cx, ey] = (250, 250, 250, 255)
                px[cx + (1 if cx == x + 1 else -1), ey] = (24, 32, 60, 255)
                ex[cx, ey] = (120, 10, 16, 255)
        for i in range(1, w - 1):
            px[x + i, y + h - 2] = (70, 30, 30, 255)


def reclaimer():
    rng = random.Random(4114)
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    eyes = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    skin_field(img, rng, (0, 0, 64, 56))         # torso (0,0) 56x26 and abdomen (0,28) 60x28
    skin_field(img, rng, (64, 0, 128, 56))       # neck, head, jaw, victim faces
    bone(img, rng, (0, 60, 40, 72))              # upper and lower legs
    bone(img, rng, (0, 74, 40, 80))              # upper arms
    bone(img, rng, (0, 82, 60, 90), dark=True)   # forearms and hands
    bone(img, rng, (0, 104, 36, 122))            # spine ridge
    veins(img, rng, (0, 0, 64, 56), 70)
    veins(img, rng, (64, 0, 128, 56), 30)
    glitch(img, rng, (0, 0, 128, 90), 14)
    head = box_faces(64, 12, 9, 10, 9)['north']
    face(img, eyes, head, rng, (220, 210, 200), 'reclaimer')
    for (u, v, size), kind in zip(((64, 32, 6), (88, 32, 6), (100, 12, 5)), ('open', 'blind', 'open')):
        rect = box_faces(u, v, size, size, size)['north']
        face(img, eyes, rect, rng, rng.choice(SKINS), kind)
    # Faint red threads running down its back, lit like its eyes.
    ex = eyes.load()
    for _ in range(6):
        x, y = rng.randrange(10, 46), 0
        while y < 26:
            ex[x, y] = (110, 8, 14, 255)
            y += 1
            x = max(0, min(55, x + rng.choice((-1, 0, 0, 1))))
    out = OUT / 'entity'
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / 'reclaimer.png')
    eyes.save(out / 'reclaimer_eyes.png')


def thread():
    """16x16 frames; row 7 is the strand, a pulse travels along it."""
    frames = 8
    img = Image.new('RGBA', (16, 16 * frames), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(7331)
    fiber = [rng.uniform(0.75, 1.0) for _ in range(16)]
    for f in range(frames):
        for x in range(16):
            pulse = max(0.0, 1.0 - abs(((x - f * 2) % 16) - 0) / 3.0)
            k = fiber[x]
            base = (int(110 * k + 120 * pulse), int(14 * k + 30 * pulse), int(22 * k + 30 * pulse))
            px[x, f * 16 + 7] = (*base, 255)
            px[x, f * 16 + 8] = (int(base[0] * 0.6), int(base[1] * 0.6), int(base[2] * 0.6), 255)
    out = OUT / 'block'
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / 'thread.png')
    (out / 'thread.png.mcmeta').write_text(json.dumps({'animation': {'frametime': 2, 'interpolate': True}}) + '\n')


def web():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(4412)
    for _ in range(11):
        x, y = rng.uniform(0, 16), rng.uniform(0, 16)
        a = rng.uniform(0, math.tau)
        for _ in range(26):
            if 0 <= x < 16 and 0 <= y < 16:
                dark = rng.random() < 0.35
                px[int(x), int(y)] = (40, 8, 12, 255) if dark else (132, 24, 34, 255)
            a += rng.uniform(-0.25, 0.25)
            x += math.cos(a) * 0.8
            y += math.sin(a) * 0.8
    img.save(OUT / 'block' / 'web.png')


def rot():
    frames = 4
    img = Image.new('RGBA', (16, 16 * frames), (0, 0, 0, 255))
    px = img.load()
    rng = random.Random(512)
    spots = [(rng.randrange(16), rng.randrange(16), rng.uniform(1.0, 2.2)) for _ in range(5)]
    base = [[rng.uniform(-12, 12) for _ in range(16)] for _ in range(16)]
    for f in range(frames):
        swell = 0.6 + 0.4 * math.sin(f / frames * math.tau)
        for y in range(16):
            for x in range(16):
                n = base[y][x]
                r, g, b = 70 + n, 44 + n * 0.7, 52 + n * 0.8
                for sx, sy, rad in spots:
                    dd = math.hypot(x - sx, y - sy)
                    if dd < rad * swell:
                        r, g, b = 128 + n, 96 + n, 70 + n
                    elif dd < rad * swell + 0.9:
                        r, g, b = 44, 20, 26
                px[x, f * 16 + y] = (clamp(r), clamp(g), clamp(b), 255)
    img.save(OUT / 'block' / 'rot.png')
    (OUT / 'block' / 'rot.png.mcmeta').write_text(json.dumps({'animation': {'frametime': 10, 'interpolate': True}}) + '\n')


def thread_entity():
    """64x16 texture of the Puppet-only thread entity: a dark fibrous strand (rows 0-3) and a brighter knot (rows 4-10)."""
    img = Image.new('RGBA', (64, 16), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(2077)
    for y in range(0, 4):
        for x in range(64):
            k = rng.uniform(0.6, 1.0)
            px[x, y] = (int(150 * k), int(18 * k), int(28 * k), 255)
            if rng.random() < 0.12:
                px[x, y] = (230, 70, 80, 255)
    for y in range(4, 11):
        for x in range(0, 16):
            k = rng.uniform(0.75, 1.0)
            px[x, y] = (int(220 * k), int(40 * k), int(50 * k), 255)
    out = OUT / 'entity'
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / 'thread.png')


if __name__ == '__main__':
    reclaimer()
    thread()
    web()
    rot()
    thread_entity()
    print('textures written to', OUT)
