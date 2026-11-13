"""Draw Zeno as a 32x32 pixel sprite, print the grid, write PNGs (1x and scaled preview)."""
import math, struct, sys, zlib

N = 32
PAL = {
    ".": None,
    "O": (0x1A, 0x10, 0x30),  # outline
    "H": (0xFF, 0xE6, 0xC2),  # highlight
    "L": (0xFF, 0xB3, 0x6B),  # light
    "M": (0xFF, 0x5E, 0x7E),  # mid
    "S": (0x8A, 0x35, 0xC9),  # shadow
    "E": (0x1A, 0x10, 0x30),  # eye
    "W": (0xFF, 0xFF, 0xFF),  # eye shine
}

CX, CY, R = 15.5, 15.5, 11.6   # body centre and radius (24 px wide)
_l = (-0.38, -0.46, 0.80)
LIGHT = tuple(c / math.sqrt(sum(v * v for v in _l)) for c in _l)  # light from top-left, towards viewer


def body():
    g = [["."] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            if math.hypot(x - CX, y - CY) <= R:
                g[y][x] = "B"
    # outline: body pixels touching the outside
    out = [row[:] for row in g]
    for y in range(N):
        for x in range(N):
            if g[y][x] != "B":
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < N and 0 <= ny < N) or g[ny][nx] == ".":
                    out[y][x] = "O"
    # shading
    for y in range(N):
        for x in range(N):
            if out[y][x] != "B":
                continue
            # classic pixel-art ball: offset circles give clean crescent bands
            if math.hypot(x - 14.0, y - 13.6) > 10.7:
                out[y][x] = "S"
            elif math.hypot(x - 13.2, y - 12.6) > 8.3:
                out[y][x] = "M"
            else:
                out[y][x] = "L"
    for x, y in ((11, 8), (12, 8), (10, 9), (11, 9), (12, 9), (13, 9), (10, 10), (11, 10)):
        out[y][x] = "H"  # hand-placed shine
    # tidy: a lone pixel whose body neighbours are all another single colour takes that colour
    for y in range(1, N - 1):
        for x in range(1, N - 1):
            if out[y][x] not in "LMS":
                continue
            nb = [out[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if out[y + dy][x + dx] in "LMS"]
            if nb and out[y][x] not in nb and len(set(nb)) == 1:
                out[y][x] = nb[0]
    return out


def eyes(g, blink=False, look=0):
    for ex in (11 + look, 19 + look):
        if blink:
            g[16][ex] = g[16][ex + 1] = "E"
        else:
            for y in (14, 15, 16):
                g[y][ex] = g[y][ex + 1] = "E"
            g[14][ex + 1] = "W"
    return g


def write_png(g, path, scale):
    w = h = N * scale
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            c = PAL[g[y // scale][x // scale]]
            raw += bytes((*c, 255)) if c else bytes((0, 0, 0, 0))
    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    open(path, "wb").write(png)


if __name__ == "__main__":
    out_dir = sys.argv[1]
    frames = {"idle": eyes(body()), "blink": eyes(body(), blink=True), "look": eyes(body(), look=1), "look_left": eyes(body(), look=-1)}
    for name, g in frames.items():
        write_png(g, f"{out_dir}/zeno_{name}.png", 1)
        write_png(g, f"{out_dir}/zeno_{name}_preview.png", 16)
    print("\n".join("".join(r) for r in frames["idle"]))
