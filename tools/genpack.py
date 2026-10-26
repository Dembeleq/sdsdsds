#!/usr/bin/env python3
"""ChicagoRP kaynak paketi uretici: 3D arac modelleri, telefon, uygulama ikonlari, 3D tabela yazilari.
Calistir:  python3 genpack.py   ->  ../../ChicagoRP-ResourcePack/ + ChicagoRP-ResourcePack.zip + sha1 + signs-def.yml
Hicbir mod gerekmez: 1.21.4 'item_model' bileseni + duz kaynak paketi (ItemsAdder'a da aktarilabilir)."""
import json, os, struct, zlib, hashlib, zipfile, shutil, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'ChicagoRP-ResourcePack')
NS = 'chicagorp'
shutil.rmtree(OUT, ignore_errors=True)
for d in ('items', 'models/item', 'textures/item'):
    os.makedirs(f'{OUT}/assets/{NS}/{d}', exist_ok=True)

PAL = [(22,22,26),(62,64,70),(135,139,146),(246,246,246),(205,30,36),(30,85,205),(250,205,30),(240,130,20),
       (40,165,62),(135,52,195),(38,58,92),(255,240,170),(205,210,220),(120,15,22),(112,72,42),(150,232,42)]
BLACK, DGRAY, GRAY, WHITE, RED, BLUE, YELLOW, ORANGE, GREEN, PURPLE, GLASS, LIGHT, CHROME, DRED, BROWN, LIME = range(16)

def write_png(path, w, h, rows):
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    raw = b''.join(b'\x00' + bytes(v for px in row for v in px) for row in rows)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    open(path, 'wb').write(data)

def wj(rel, obj):
    p = f'{OUT}/{rel}'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), separators=(',', ':'))

json.dump({"pack": {"pack_format": 46, "description": "ChicagoRP 3D modelleri"}}, open(f'{OUT}/pack.mcmeta', 'w'))

# ---- palet dokusu (64x64, her renk 4px'lik dikey seritler)
rows = [[PAL[x // 4] + (255,) for x in range(64)] for _ in range(64)]
write_png(f'{OUT}/assets/{NS}/textures/item/palette.png', 64, 64, rows)

def r3(v): return round(v, 3)
def el(x1, y1, z1, x2, y2, z2, c):
    f = {"uv": [c + 0.3, 2, c + 0.7, 6], "texture": "#0"}
    return {"from": [r3(x1), r3(y1), r3(z1)], "to": [r3(x2), r3(y2), r3(z2)],
            "faces": {k: dict(f) for k in ("north", "east", "south", "west", "up", "down")}}

def save_model(name, elements, display=None):
    m = {"textures": {"0": f"{NS}:item/palette", "particle": f"{NS}:item/palette"}, "elements": elements}
    if display: m["display"] = display
    wj(f'assets/{NS}/models/item/{name}.json', m)
    wj(f'assets/{NS}/items/{name}.json', {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})

# ================= ARACLAR (on = +z) =================
def car(p):
    L, W, h1, h2 = p['L'], p['W'], p['h1'], p['h2']
    cx = cz = 8.0
    y0, gc = 8.0, 2.0
    x1, x2, z1, z2 = cx - W / 2, cx + W / 2, cz - L / 2, cz + L / 2
    yb = y0 + gc; yt = yb + h1; ytop = yt + h2
    body = p['body']; lower = p.get('lower')
    E = []
    if lower is not None:
        E.append(el(x1, yb, z1, x2, yb + h1 * 0.45, z2, lower))
        E.append(el(x1, yb + h1 * 0.45, z1, x2, yt, z2, body))
    else:
        E.append(el(x1, yb, z1, x2, yt, z2, body))
    zs, ze = z1 + p['cs'], z1 + p['ce']
    E.append(el(x1 + 1.2, yt, zs, x2 - 1.2, ytop, ze, GLASS))
    E.append(el(x1 + 0.9, ytop, zs - 0.3, x2 - 0.9, ytop + 0.8, ze + 0.3, body))
    for px in (x1 + 0.9, x2 - 1.9):
        for pz in (zs - 0.3, ze - 0.7):
            E.append(el(px, yt, pz, px + 1.0, ytop, pz + 1.0, body))
    # kaput egimi / bagaj
    E.append(el(x1 + 0.4, yt, ze + 0.3, x2 - 0.4, yt + 0.8, z2 - 0.3, body))
    wd, dia = 2.4, p.get('wheel', 4.6)
    for wx0, wx1, hubx in ((x1 - 0.5, x1 + 1.9, x1 - 0.7), (x2 - 1.9, x2 + 0.5, x2 + 0.5)):
        for zc in (z1 + L * 0.2, z2 - L * 0.2):
            E.append(el(wx0, y0, zc - dia / 2, wx1, y0 + dia, zc + dia / 2, BLACK))
            E.append(el(hubx, y0 + dia / 2 - 1.1, zc - 1.1, hubx + 0.2, y0 + dia / 2 + 1.1, zc + 1.1, CHROME))
    for sx in (x1 + 0.9, x2 - 3.4):
        E.append(el(sx, yt - 2.2, z2 - 0.05, sx + 2.5, yt - 0.7, z2 + 0.4, LIGHT))
        E.append(el(sx, yt - 2.2, z1 - 0.4, sx + 2.5, yt - 0.7, z1 + 0.05, RED))
    E.append(el(cx - 3, yb + 1.0, z2 - 0.05, cx + 3, yt - 2.6, z2 + 0.3, DGRAY))
    E.append(el(x1 + 0.1, yb - 0.4, z2 - 0.3, x2 - 0.1, yb + 1.2, z2 + 0.8, p.get('bump', DGRAY)))
    E.append(el(x1 + 0.1, yb - 0.4, z1 - 0.8, x2 - 0.1, yb + 1.2, z1 + 0.3, p.get('bump', DGRAY)))
    for mx in (x1 - 0.9, x2 + 0.1):
        E.append(el(mx, yt + 0.2, zs + 0.6, mx + 0.8, yt + 1.4, zs + 1.6, body))
    if p.get('sign'):
        E.append(el(cx - 2.5, ytop + 0.8, cz - 1.6, cx + 2.5, ytop + 2.4, cz + 1.6, YELLOW))
        E.append(el(cx - 2.5, ytop + 1.4, cz - 1.7, cx + 2.5, ytop + 1.8, cz + 1.7, BLACK))
    if p.get('rails'):
        for rx in (x1 + 1.3, x2 - 2.0):
            E.append(el(rx, ytop + 0.8, zs, rx + 0.7, ytop + 1.5, ze, CHROME))
    if p.get('spoiler'):
        sh = p['spoiler']
        for sx in (x1 + 2.0, x2 - 3.0):
            E.append(el(sx, yt, z1 + 0.6, sx + 1.0, yt + sh, z1 + 1.6, DGRAY))
        E.append(el(x1 + 0.3, yt + sh, z1 - 0.8, x2 - 0.3, yt + sh + 0.8, z1 + 3.2, p.get('wing', BLACK)))
    if p.get('stripe') is not None:
        E.append(el(cx - 1.2, yt + 0.8, ze, cx + 1.2, yt + 1.0, z2 - 0.3, p['stripe']))
        E.append(el(cx - 1.2, ytop + 0.8, zs, cx + 1.2, ytop + 1.0, ze, p['stripe']))
    return E

CARS = {
 'sahin':         dict(L=36, W=18, h1=6.0, h2=6.0, cs=9,  ce=28, body=WHITE),
 'taksi':         dict(L=38, W=18, h1=6.0, h2=6.0, cs=9,  ce=30, body=YELLOW, sign=True),
 'hatchback':     dict(L=30, W=17, h1=6.0, h2=6.5, cs=6,  ce=28, body=BLUE),
 'suv':           dict(L=40, W=20, h1=8.0, h2=6.5, cs=6,  ce=34, body=DGRAY, rails=True, wheel=5.6),
 'beyaz_suv':     dict(L=40, W=20, h1=8.0, h2=6.5, cs=6,  ce=34, body=WHITE, rails=True, wheel=5.6),
 'kirmizi_sedan': dict(L=42, W=18, h1=6.0, h2=5.5, cs=11, ce=32, body=RED),
 'trueno':        dict(L=34, W=17, h1=4.8, h2=5.0, cs=11, ce=26, body=WHITE, lower=BLACK, wheel=4.2),
 'bmw_m3':        dict(L=40, W=18, h1=5.6, h2=5.0, cs=11, ce=31, body=BLUE, spoiler=1.2, wing=BLUE),
 'bmw_m4':        dict(L=40, W=19, h1=5.4, h2=4.8, cs=12, ce=30, body=ORANGE, spoiler=1.0, wing=BLACK),
 'spor':          dict(L=40, W=20, h1=4.4, h2=4.2, cs=13, ce=29, body=LIME, spoiler=3.2, wing=BLACK, wheel=4.4),
 'super_spor':    dict(L=44, W=22, h1=3.8, h2=4.0, cs=15, ce=31, body=PURPLE, spoiler=4.2, wing=BLACK, stripe=WHITE, wheel=4.4),
}
for cid, p in CARS.items():
    save_model(f'car_{cid}', car(p))

# ================= TELEFON (3D) =================
ph = [el(4.5, 1, 7.3, 11.5, 15, 8.7, BLACK),
      el(5.0, 1.6, 7.2, 11.0, 14.4, 8.8, GLASS),
      el(5.0, 11.0, 7.15, 11.0, 14.4, 8.85, PURPLE),
      el(5.0, 7.5, 7.15, 11.0, 11.0, 8.85, BLUE),
      el(5.0, 1.6, 7.15, 11.0, 7.5, 8.85, GLASS),
      el(7.3, 0.6, 7.2, 8.7, 1.0, 8.8, CHROME)]
for i, (cx, cy, col) in enumerate([(5.7, 5.2, RED), (7.7, 5.2, YELLOW), (9.7, 5.2, GREEN), (5.7, 3.0, ORANGE), (7.7, 3.0, LIGHT), (9.7, 3.0, LIME)]):
    ph.append(el(cx, cy, 7.1, cx + 1.3, cy + 1.3, 8.9, col))
ph.append(el(9.8, 12.3, 7.0, 10.6, 13.1, 9.0, BLACK))
save_model('phone', ph, {"gui": {"rotation": [0, 0, 0], "scale": [1.15, 1.15, 1.15]},
                         "firstperson_righthand": {"rotation": [0, -90, 20], "translation": [1.0, 2, 0], "scale": [0.6, 0.6, 0.6]},
                         "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.5, 0.5, 0.5]}})

# ================= UYGULAMA IKONLARI (2D, 16x16) =================
GLYPHS = {
 'sehir':  ["00111100","01111110","01100110","01100110","01111110","00111100","00011000","00001000"],
 'isler':  ["00111100","00100100","11111111","11111111","11111111","11111111","11111111","00000000"],
 'arac':   ["00000000","00111100","01111110","11111111","11111111","11111111","01100110","00000000"],
 'cete':   ["11111111","11111111","11111111","11111111","01111110","00111100","00011000","00000000"],
 'sosyal': ["00011000","00111100","00011000","00111100","01111110","11111111","11111111","00000000"],
 'oduller':["00011000","00011000","11111111","01111110","00111100","01100110","11000011","00000000"],
 'kilit':  ["00111100","01000010","01000010","11111111","11111111","11111111","11111111","00000000"],
 'harita': ["11111111","10101011","11011101","10110101","11101011","10111101","11010111","11111111"],
 'kapat':  ["10000001","01000010","00100100","00011000","00011000","00100100","01000010","10000001"],
 'saat':   ["00111100","01000010","10001001","10001001","10000101","10000001","01000010","00111100"],
 'para':   ["00111100","01111110","11011011","11011111","11111011","11011011","01111110","00111100"],
}
COLORS = {'sehir': (40,150,230), 'isler': (40,170,90), 'arac': (230,130,30), 'cete': (200,45,60), 'sosyal': (150,70,210),
          'oduller': (240,190,30), 'kilit': (90,90,110), 'harita': (30,120,120), 'kapat': (170,40,40), 'saat': (60,60,150), 'para': (30,140,60)}
for name, g in GLYPHS.items():
    col = COLORS[name]
    img = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            cx, cy = min(x, 15 - x), min(y, 15 - y)
            if (cx == 0 and cy == 0) or (cx + cy == 1 and (cx == 0 or cy == 0) and False): continue
            if cx == 0 and cy <= 1 or cy == 0 and cx <= 1: continue
            shade = int(30 * (y / 15.0))
            img[y][x] = (max(col[0] - shade, 0), max(col[1] - shade, 0), max(col[2] - shade, 0), 255)
    for gy in range(8):
        for gx in range(8):
            if g[gy][gx] == '1':
                for dy in (0, 1):
                    for dx in (0, 1):
                        px, py = 0 + gx * 1 + 4 - 0, gy * 1 + 4
                        if 0 <= py < 16 and 0 <= px < 16: img[py][px] = (255, 255, 255, 255)
    write_png(f'{OUT}/assets/{NS}/textures/item/app_{name}.png', 16, 16, img)
    wj(f'assets/{NS}/models/item/app_{name}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/app_{name}"}})
    wj(f'assets/{NS}/items/app_{name}.json', {"model": {"type": "minecraft:model", "model": f"{NS}:item/app_{name}"}})

# ================= 3D TABELA YAZILARI =================
F = {
 'A':["01110","10001","10001","11111","10001","10001","10001"], 'B':["11110","10001","10001","11110","10001","10001","11110"],
 'C':["01110","10001","10000","10000","10000","10001","01110"], 'D':["11110","10001","10001","10001","10001","10001","11110"],
 'E':["11111","10000","10000","11110","10000","10000","11111"], 'G':["01110","10001","10000","10111","10001","10001","01111"],
 'H':["10001","10001","10001","11111","10001","10001","10001"], 'I':["01110","00100","00100","00100","00100","00100","01110"],
 'J':["00111","00010","00010","00010","00010","10010","01100"], 'K':["10001","10010","10100","11000","10100","10010","10001"],
 'L':["10000","10000","10000","10000","10000","10000","11111"], 'M':["10001","11011","10101","10101","10001","10001","10001"],
 'N':["10001","11001","10101","10011","10001","10001","10001"], 'O':["01110","10001","10001","10001","10001","10001","01110"],
 'P':["11110","10001","10001","11110","10000","10000","10000"], 'R':["11110","10001","10001","11110","10100","10010","10001"],
 'S':["01111","10000","10000","01110","00001","00001","11110"], 'T':["11111","00100","00100","00100","00100","00100","00100"],
 'U':["10001","10001","10001","10001","10001","10001","01110"], 'V':["10001","10001","10001","10001","10001","01010","00100"],
 'Y':["10001","10001","01010","00100","00100","00100","00100"], 'Z':["11111","00001","00010","00100","01000","10000","11111"],
 ' ':["00000"]*7,
}
EXTRA = {  # (ustte satirlar, altta satirlar)
 'İ': ('I', ["00100"], []), 'Ö': ('O', ["01010"], []), 'Ü': ('U', ["01010"], []), 'Ç': ('C', [], ["00100"]), 'Ş': ('S', [], ["00100"]), 'Ğ': ('G', ["01110"], []),
}
PX, PH, DEPTH = 0.5, 0.55, 1.2

def glyph(ch):
    if ch in EXTRA:
        base, above, below = EXTRA[ch]
        return F[base], above, below
    return F[ch], [], []

def text_elements(lines, color):
    """lines: satir listesi. Doner: (elements, genislik_birim)"""
    els = []
    maxw = max(len(l) for l in lines) * 6 * PX - PX
    line_h = 11 * PH
    total_h = len(lines) * line_h
    top = 8 + total_h / 2
    for li, line in enumerate(lines):
        w = len(line) * 6 * PX - PX
        x0 = 8 - w / 2
        ybase = top - (li + 1) * line_h + 2 * PH
        for ci, ch in enumerate(line):
            rows, above, below = glyph(ch)
            allrows = []
            for k, r in enumerate(above): allrows.append((-len(above) + k, r))
            for k, r in enumerate(rows): allrows.append((k, r))
            for k, r in enumerate(below): allrows.append((7 + k, r))
            for ri, r in allrows:
                y1 = ybase + (6 - ri) * PH
                x = 0
                while x < 5:
                    if r[x] == '1':
                        s = x
                        while x < 5 and r[x] == '1': x += 1
                        els.append(el(x0 + ci * 6 * PX + s * PX, y1, 8 - DEPTH / 2, x0 + ci * 6 * PX + x * PX, y1 + PH, 8 + DEPTH / 2, color))
                    else:
                        x += 1
    return els, maxw

SIGNS = {  # id: (satirlar, renk, genislik_blok)
 'karakol': (["POLİS", "KARAKOLU"], BLUE, 6.5), 'durumcu': (["DÜRÜMCÜ"], RED, 10), 'market': (["MARKET"], GREEN, 9),
 'giyim': (["GİYİM"], BLACK, 8), 'kahve': (["KAHVEHANE"], BROWN, 11), 'silah': (["SİLAHÇI"], DGRAY, 10),
 'demirci': (["DEMİRCİ"], ORANGE, 10), 'kargo': (["KARGO"], ORANGE, 8), 'mekanik': (["MEKANİK"], BLUE, 10),
 'galeri': (["GALERİ"], BLACK, 14), 'jenerator': (["JENERATÖR"], YELLOW, 12), 'pavyon': (["PAVYON"], PURPLE, 10),
 'belediye': (["BELEDİYE"], BLUE, 11), 'hastane': (["HASTANE"], RED, 14), 'banka': (["BANKA"], YELLOW, 8),
}
cfg = []
for sid, (lines, col, wblocks) in SIGNS.items():
    els, units = text_elements(lines, col)
    save_model(f'sign_{sid}', els)
    cfg.append(f"  {sid}: {{units: {units}, width: {wblocks}}}")
open(os.path.join(ROOT, 'signs-def.yml'), 'w').write("signs-def:\n" + "\n".join(cfg) + "\n")

# ---- zip + sha1
zpath = os.path.join(ROOT, 'ChicagoRP-ResourcePack.zip')
if os.path.exists(zpath): os.remove(zpath)
with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED) as z:
    for d, _, fs in os.walk(OUT):
        for f in fs:
            fp = os.path.join(d, f); z.write(fp, os.path.relpath(fp, OUT))
sha = hashlib.sha1(open(zpath, 'rb').read()).hexdigest()
open(os.path.join(ROOT, 'ChicagoRP-ResourcePack.sha1'), 'w').write(sha)
print('OK', len(os.listdir(f'{OUT}/assets/{NS}/models/item')), 'model', os.path.getsize(zpath) // 1024, 'KB', sha)
