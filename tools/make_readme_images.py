#!/usr/bin/env python3
"""Build README images from demo-app screenshots (fictional data from the "demo" build type).

Usage: python tools/make_readme_images.py <screenshot_dir>
Expects PNG screenshots named like 01_dashboard.png … (1200x2670 phone captures).
Writes docs/images/*.png (status bar cropped, rounded phone-style corners, resized).
"""
import os, sys
from PIL import Image, ImageDraw, ImageFilter

SRC = sys.argv[1]
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'docs', 'images')
os.makedirs(OUT, exist_ok=True)
NAVY = (7, 17, 31)

def phone(name, width=540):
    im = Image.open(os.path.join(SRC, name)).convert('RGB')
    w, h = im.size
    im = im.crop((0, int(h * 0.036), w, h - int(h * 0.022)))  # drop status bar (notification icons) and gesture bar
    im = im.resize((width, int(im.height * width / w)), Image.LANCZOS)
    mask = Image.new('L', im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius=int(width * 0.07), fill=255)
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out

def save(img, name):
    path = os.path.join(OUT, name)
    img.save(path, optimize=True)
    print(f'{name}: {img.size[0]}x{img.size[1]}, {os.path.getsize(path) // 1024} KB')

# gallery images
shots = {
    '01_dashboard.png': 'dashboard.png', '02_dash2.png': 'dashboard-shortcuts.png', '04b_cards.png': 'cards.png',
    '05_records.png': 'records.png', '06_budgets.png': 'budgets.png', '07_bills.png': 'bills.png',
    '08_analytics.png': 'analytics.png', '09_debts.png': 'debts.png',
}
for src, dst in shots.items():
    if os.path.exists(os.path.join(SRC, src)):
        save(phone(src, 360), dst)

# hero banner: three phones on navy with a soft glow
W, H = 1600, 900
banner = Image.new('RGBA', (W, H), NAVY + (255,))
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow)
gd.ellipse((W * 0.18, -H * 0.3, W * 0.82, H * 0.9), fill=(26, 77, 156, 150))
gd.ellipse((W * 0.55, H * 0.45, W * 1.05, H * 1.25), fill=(245, 160, 32, 60))
banner = Image.alpha_composite(banner, glow.filter(ImageFilter.GaussianBlur(120)))
specs = [('04b_cards.png', 330, -7, (230, 150)), ('06_budgets.png', 330, 7, (1040, 150)), ('01_dashboard.png', 380, 0, (610, 70))]
for name, width, angle, (x, y) in specs:
    p = phone(name, width)
    shadow = Image.new('RGBA', (p.width + 120, p.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((60, 70, p.width + 60, p.height + 60), radius=40, fill=(0, 0, 0, 170))
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    if angle:
        p = p.rotate(angle, resample=Image.BICUBIC, expand=True)
        shadow = shadow.rotate(angle, resample=Image.BICUBIC, expand=True)
    banner.alpha_composite(shadow, (max(0, x - 60), max(0, y - 40)))
    banner.alpha_composite(p, (x, y))
save(banner.convert('RGB'), 'banner.png')
