# Original pixel-art cover for the itch.io page (630x500): drawn at 126x100, scaled x5 with nearest neighbour.
import random, os
from PIL import Image, ImageDraw, ImageFont
W, H, S = 126, 100, 5
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "dist", "itch")
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(4114)
img = Image.new("RGB", (W, H)); px = img.load()
# sky: black at the top, bleeding to dark red at the horizon
for y in range(H):
    t = min(1, y / 72)
    for x in range(W):
        r = int(6 + 44 * t ** 2.2); g = int(4 + 3 * t); b = int(9 + 4 * t)
        n = rnd.randint(-2, 2)
        px[x, y] = (max(0, r + n), max(0, g), max(0, b + n))
d = ImageDraw.Draw(img)
# the giant in the sky (it is not on this plane: dark, huge, faint outline)
cx = 63
body = (12, 8, 14); rim = (70, 10, 18)
def block(x0, y0, x1, y1, c): d.rectangle([x0, y0, x1, y1], fill=c)
def outlined(x0, y0, x1, y1):
    block(x0 - 1, y0 - 1, x1 + 1, y1 + 1, rim); block(x0, y0, x1, y1, body)
parts = [(cx - 7, 6, cx + 6, 17), (cx - 9, 18, cx + 8, 40), (cx - 14, 19, cx - 10, 52), (cx + 9, 19, cx + 13, 52),
         (cx - 15, 52, cx - 11, 56), (cx + 10, 52, cx + 14, 56), (cx - 7, 41, cx - 3, 62), (cx + 2, 41, cx + 6, 62), (cx - 2, 17, cx + 1, 18)]
for x0, y0, x1, y1 in parts: block(x0 - 1, y0 - 1, x1 + 1, y1 + 1, rim)
for x0, y0, x1, y1 in parts: block(x0, y0, x1, y1, body)
block(cx - 9, 19, cx + 8, 20, body)
for ex in (cx - 4, cx + 2):                # eyes
    block(ex, 11, ex + 1, 12, (255, 40, 40))
    px[ex, 10] = (120, 10, 20)
# its threads: from its hands and body down into the world
threads = [(cx - 13, 56, 18, 76), (cx + 12, 56, 104, 77), (cx - 2, 40, 44, 79), (cx + 1, 40, 86, 80),
           (cx - 13, 56, 6, 82), (cx + 12, 56, 122, 81), (cx, 62, 63, 86)]
for x0, y0, x1, y1 in threads:
    steps = max(abs(x1 - x0), abs(y1 - y0)) * 2
    for i in range(steps + 1):
        t = i / steps; sag = 3 * (1 - (2 * t - 1) ** 2)
        x = round(x0 + (x1 - x0) * t); y = round(y0 + (y1 - y0) * t + sag)
        if 0 <= x < W and 0 <= y < H: px[x, y] = (150, 18, 32) if i % 3 else (200, 30, 45)
# blocky ground: hills, grass edge, dirt
heights = []
h = 80
for x in range(W):
    if x % 6 == 0: h = max(76, min(86, h + rnd.choice([-1, 0, 0, 1])))
    heights.append(h)
for x in range(W):
    top = heights[x]
    for y in range(top, H):
        if y == top: c = (22, 38, 18)
        elif y < top + 2: c = (16, 26, 13)
        else: c = (14, 10, 9) if (x // 4 + y // 4) % 2 else (17, 12, 10)
        px[x, y] = c
# the house with a lit window (someone forgot the light on)
hx, hy = 14, heights[14] - 9
block(hx, hy, hx + 13, hy + 8, (24, 16, 12))
for i in range(8): block(hx - 1 + i, hy - 1 - i // 2, hx + 14 - i, hy - 1 - i // 2, (30, 14, 14))
block(hx + 3, hy + 3, hx + 5, hy + 5, (255, 190, 90)); block(hx + 9, hy + 4, hx + 10, hy + 8, (10, 7, 6))
for i in range(1, 7):
    for j in range(-i, i + 1):
        x, y = hx + 4 + j, hy + 4
        if 0 <= x < W and rnd.random() < 0.25: px[x, min(H - 1, y + rnd.randint(-2, 2))] = (60, 40, 20)
# the player, from behind, small, looking up
p0 = 61; ground = heights[p0]
block(p0, ground - 8, p0 + 3, ground - 6, (40, 28, 20))       # head
block(p0, ground - 5, p0 + 3, ground - 3, (30, 90, 110))       # shirt
block(p0 - 1, ground - 5, p0 - 1, ground - 3, (30, 90, 110)); block(p0 + 4, ground - 5, p0 + 4, ground - 3, (30, 90, 110))
block(p0, ground - 2, p0 + 3, ground - 1, (30, 30, 70))         # legs
img = img.resize((W * S, H * S), Image.NEAREST)
# title, pixel font: drawn small and scaled up so it stays blocky
def pixel_text(text, size, scale, color, y):
    font = ImageFont.truetype(r"C:\Windows\Fonts\consolab.ttf", size)
    l, t, r, b = font.getbbox(text)
    small = Image.new("RGBA", (r - l + 4, b - t + 4), (0, 0, 0, 0))
    ImageDraw.Draw(small).text((2 - l, 2 - t), text, font=font, fill=color)
    shadow = Image.new("RGBA", small.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).text((2 - l, 2 - t), text, font=font, fill=(0, 0, 0, 230))
    big = small.resize((small.width * scale, small.height * scale), Image.NEAREST)
    sh = shadow.resize(big.size, Image.NEAREST)
    x = (img.width - big.width) // 2
    img.paste(sh, (x + scale, y + scale), sh); img.paste(big, (x, y), big)
pixel_text("UNALLOCATED", 14, 4, (232, 226, 214, 255), 408)
pixel_text("a Minecraft horror mod", 8, 3, (200, 45, 55, 255), 460)
img.save(os.path.join(OUT, "cover-630x500.png"))
print(img.size)
