# Original "fallen soul" player skins (64x64, standard layout), degraded: desaturated, glitched, hollow eyes, threads.
import random, os
from PIL import Image
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "unallocated", "textures", "entity", "soul")
os.makedirs(OUT, exist_ok=True)
SOULS = [  # skin, hair, shirt, pants, shoes
    ((198, 150, 120), (60, 40, 25), (40, 120, 70), (40, 50, 90), (40, 30, 25)),
    ((120, 80, 55), (20, 15, 10), (170, 60, 50), (60, 60, 60), (20, 20, 20)),
    ((230, 190, 160), (200, 160, 70), (60, 90, 160), (90, 70, 50), (60, 40, 30)),
    ((160, 110, 80), (90, 30, 20), (230, 200, 60), (30, 30, 40), (80, 80, 80)),
    ((210, 170, 140), (30, 30, 30), (120, 60, 140), (50, 80, 60), (30, 30, 30)),
]
def rect(px, x0, y0, w, h, c):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            px[x, y] = c + (255,)
def degrade(c, rnd, amount=0.65):
    g = sum(c) / 3
    c = [int(v * (1 - amount) + g * amount) for v in c]
    n = rnd.randint(-18, 18)
    return tuple(max(0, min(255, v + n - 25)) for v in c)
for i, (skin, hair, shirt, pants, shoes) in enumerate(SOULS, 1):
    rnd = random.Random(4114 + i)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); px = img.load()
    # head (all faces skin, top/back hair)
    rect(px, 0, 0, 32, 16, skin)
    rect(px, 8, 0, 8, 8, hair); rect(px, 24, 8, 8, 8, hair)
    for x0 in (0, 16): rect(px, x0, 8, 8, 3, hair)
    rect(px, 8, 8, 8, 2, hair)
    # hollow eyes, open mouth
    for ex in (9, 13): rect(px, ex, 11, 2, 2, (8, 8, 8))
    px[9, 12] = (230, 230, 230, 255); px[14, 12] = (230, 230, 230, 255)
    rect(px, 11, 14, 2, 1, (20, 5, 5))
    # body, arms, legs (base layer, both sides of the 64x64 layout)
    rect(px, 16, 16, 24, 16, shirt)
    rect(px, 40, 16, 16, 16, shirt); rect(px, 40, 26, 16, 6, skin)
    rect(px, 32, 48, 16, 16, shirt); rect(px, 32, 58, 16, 6, skin)
    rect(px, 0, 16, 16, 16, pants); rect(px, 0, 28, 16, 4, shoes)
    rect(px, 16, 48, 16, 16, pants); rect(px, 16, 60, 16, 4, shoes)
    # degrade every opaque pixel
    for x in range(64):
        for y in range(64):
            if y >= 32 and y < 48: continue
            if x >= 32 and y < 16: continue
            if y >= 48 and x >= 48: continue
            if y >= 48 and x < 16: continue
            r, g, b, a = px[x, y]
            if a: px[x, y] = degrade((r, g, b), rnd) + (255,)
    # glitch: shifted rows
    for _ in range(6):
        y = rnd.randrange(0, 64); s = rnd.randint(-3, 3)
        row = [px[x, y] for x in range(64)]
        for x in range(64):
            src = row[(x - s) % 64]
            if px[x, y][3] and src[3]: px[x, y] = src
    # rot patches and thread marks (dark red)
    for _ in range(40):
        x, y = rnd.randrange(0, 64), rnd.randrange(16, 32) if rnd.random() < .6 else rnd.randrange(0, 16)
        if px[x, y][3]: px[x, y] = (rnd.randint(25, 45), 8, 10, 255)
    for x0 in (20, 44):
        for y in range(16, 32):
            if px[x0, y][3] and rnd.random() < .8: px[x0, y] = (138, 16, 32, 255)
    img.save(os.path.join(OUT, f"soul_{i}.png"))
print("ok")
