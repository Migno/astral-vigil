"""Tiny dependency-free RGBA PNG reader/writer for the asset generators (no Pillow needed)."""
import struct
import zlib


def read(path):
    """Returns (width, height, rows) with rows[y][x] = (r, g, b, a). Supports RGBA, RGB, grey (+alpha) and palette PNGs
    (palette and grey also at 1/2/4 bits)."""
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos, idat, palette, trns = 8, b"", None, None
    while pos < len(data):
        length, ctype = struct.unpack(">I4s", data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if ctype == b"IHDR":
            w, h, depth, color = struct.unpack(">IIBB", chunk[:10])
            assert depth == 8 or (color in (0, 3) and depth in (1, 2, 4)), "unsupported PNG bit depth"
        elif ctype == b"PLTE":
            palette = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif ctype == b"tRNS":
            trns = chunk
        elif ctype == b"IDAT":
            idat += chunk
    channels = {6: 4, 2: 3, 3: 1, 0: 1, 4: 2}[color]
    bits = depth * channels
    bpp = max(1, bits // 8)
    stride = (w * bits + 7) // 8
    raw = zlib.decompress(idat)
    rows, prev = [], bytearray(stride)
    for y in range(h):
        f = raw[y * (stride + 1)]
        line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if f == 1:
                line[i] = (line[i] + a) & 255
            elif f == 2:
                line[i] = (line[i] + b) & 255
            elif f == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        prev = line
        row = []
        for x in range(w):
            if depth < 8:
                per = 8 // depth
                byte = line[x // per]
                shift = 8 - depth * (x % per + 1)
                val = (byte >> shift) & ((1 << depth) - 1)
                if color == 3:
                    alpha = trns[val] if trns is not None and val < len(trns) else 255
                    row.append(palette[val] + (alpha,))
                else:
                    g = val * 255 // ((1 << depth) - 1)
                    row.append((g, g, g, 255))
                continue
            px = line[x * bpp:(x + 1) * bpp]
            if color == 6:
                row.append(tuple(px))
            elif color == 2:
                row.append(tuple(px) + (255,))
            elif color == 0:
                row.append((px[0], px[0], px[0], 255))
            elif color == 4:
                row.append((px[0], px[0], px[0], px[1]))
            else:
                idx = px[0]
                alpha = trns[idx] if trns is not None and idx < len(trns) else 255
                row.append(palette[idx] + (alpha,))
        rows.append(row)
    return w, h, rows


def write(path, rows):
    h, w = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def blank(w, h):
    return [[(0, 0, 0, 0)] * w for _ in range(h)]


def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def colours(rows):
    return {px for row in rows for px in row if px[3] > 0}
