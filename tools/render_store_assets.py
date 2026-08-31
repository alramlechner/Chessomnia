#!/usr/bin/env python3
"""
Renders the Google Play store artwork from the app's own vector resources.

Everything here is derived from what the app actually ships -- the same piece
drawables, the same wordmark, the same palette -- so the listing cannot drift
away from the product. Nothing is hand-drawn in an image editor.

    python3 tools/render_store_assets.py
    rsvg-convert store/play-icon-512.svg  -w 512  -h 512 -o store/play-icon-512.png
    rsvg-convert store/play-feature.svg   -w 1024 -h 500 -o store/play-feature-1024x500.png

Produces:
    store/play-icon-512.svg       app icon, 512x512, opaque
    store/play-feature.svg        feature graphic, 1024x500

⚠️ Screenshots are deliberately NOT generated here. Google Play requires
screenshots to show the real app; a mock-up of the board would misrepresent it.
Take those on a device.
"""
import pathlib
import re
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "android-app/app/src/main/res/drawable"
OUT = ROOT / "store"

NAVY = "#2B303E"
BLUE = "#3193C6"
LIGHT_SQUARE = "#F0D9B5"
DARK_SQUARE = "#B58863"


GROUP_ATTRS = {"name", "translateX", "translateY", "rotation",
               "scaleX", "scaleY", "pivotX", "pivotY"}


def group_transform(el, path):
    """
    A VectorDrawable <group> as an SVG transform.

    Order per the platform documentation: scale, then rotate, then translate,
    each about the pivot. Written out, that is

        translate(tx,ty) translate(px,py) rotate(r) scale(sx,sy) translate(-px,-py)

    ⚠️ scaleX/scaleY must not be dropped. The launcher foreground relies on them
    to fit the motif into the inner ~66 % of its grid; ignoring them silently
    produces a clipped, off-centre icon -- which is exactly what an earlier
    version of this script did. Hence the explicit check below: an attribute this
    code does not understand aborts the run instead of quietly changing the
    artwork.
    """
    for a in el.attrib:
        if a.startswith(ANDROID) and a[len(ANDROID):] not in GROUP_ATTRS:
            raise SystemExit(f"{path.name}: unsupported group attribute {a[len(ANDROID):]}")

    g = lambda n, d="0": el.get(ANDROID + n, d)
    tx, ty = g("translateX"), g("translateY")
    px, py = g("pivotX"), g("pivotY")
    sx, sy = g("scaleX", "1"), g("scaleY", "1")
    rot = g("rotation")

    parts = [f"translate({tx},{ty})"]
    if (px, py) != ("0", "0"):
        parts.append(f"translate({px},{py})")
    if rot != "0":
        parts.append(f"rotate({rot})")
    if (sx, sy) != ("1", "1"):
        parts.append(f"scale({sx},{sy})")
    if (px, py) != ("0", "0"):
        parts.append(f"translate(-{px},-{py})")
    return " ".join(parts)


def vector_to_svg_group(path, scale=1.0, dx=0.0, dy=0.0):
    """
    Turns a VectorDrawable into an SVG <g>.

    Only what the app's own vector files use is supported. Anything else aborts
    rather than silently dropping detail -- see the warning in group_transform.
    """
    root = ET.parse(path).getroot()
    vw = float(root.get(ANDROID + "viewportWidth"))
    out = [f'<g transform="translate({dx},{dy}) scale({scale / vw})">']

    def emit_path(el, indent):
        d = el.get(ANDROID + "pathData")
        bits = [f'd="{d}"', f'fill="{el.get(ANDROID + "fillColor", "none")}"']
        stroke = el.get(ANDROID + "strokeColor")
        if stroke:
            bits += [
                f'stroke="{stroke}"',
                f'stroke-width="{el.get(ANDROID + "strokeWidth", "1")}"',
                f'stroke-linecap="{el.get(ANDROID + "strokeLineCap", "butt")}"',
                f'stroke-linejoin="{el.get(ANDROID + "strokeLineJoin", "miter")}"',
            ]
        if el.get(ANDROID + "fillType", "").lower() == "evenodd":
            bits.append('fill-rule="evenodd"')
        out.append(f'{indent}<path {" ".join(bits)}/>')

    def walk(node, indent):
        for el in node:
            if el.tag == "path":
                emit_path(el, indent)
            elif el.tag == "group":
                out.append(f'{indent}<g transform="{group_transform(el, path)}">')
                walk(el, indent + "  ")
                out.append(f"{indent}</g>")
            else:
                raise SystemExit(f"{path.name}: unsupported element <{el.tag}>")

    walk(root, "  ")
    out.append("</g>")
    return "\n".join(out)


def board_svg(fen_ranks, x, y, square, border=0):
    """An 8x8 board with pieces, in the app's own board colours."""
    parts = []
    if border:
        parts.append(
            f'<rect x="{x - border}" y="{y - border}" '
            f'width="{square * 8 + 2 * border}" height="{square * 8 + 2 * border}" '
            f'rx="{border}" fill="#00000055"/>'
        )
    for rank in range(8):
        for file in range(8):
            fill = LIGHT_SQUARE if (rank + file) % 2 == 0 else DARK_SQUARE
            parts.append(
                f'<rect x="{x + file * square}" y="{y + rank * square}" '
                f'width="{square}" height="{square}" fill="{fill}"/>'
            )
    for rank, row in enumerate(fen_ranks):
        file = 0
        for ch in row:
            if ch.isdigit():
                file += int(ch)
                continue
            colour = "w" if ch.isupper() else "b"
            name = {"k": "king", "q": "queen", "r": "rook",
                    "b": "bishop", "n": "knight", "p": "pawn"}[ch.lower()]
            parts.append(vector_to_svg_group(
                RES / f"piece_{colour}_{name}.xml",
                scale=square, dx=x + file * square, dy=y + rank * square,
            ))
            file += 1
    return "\n".join(parts)


def wordmark(fill):
    """The generated wordmark, recoloured for a dark carrier."""
    svg = (ROOT / "branding/chessomnia-wordmark.svg").read_text(encoding="utf-8")
    d = re.search(r'<path fill="url\(#chessomnia\)" d="([^"]+)"', svg).group(1)
    tx, ty = re.search(r'<g transform="translate\(([-\d.]+),([-\d.]+)\)">', svg).group(1, 2)
    w = float(re.search(r'viewBox="0 0 ([\d.]+)', svg).group(1))
    h = float(re.search(r'viewBox="0 0 [\d.]+ ([\d.]+)"', svg).group(1))
    return f'<g transform="translate({tx},{ty})"><path fill="{fill}" d="{d}"/></g>', w, h


def main():
    OUT.mkdir(exist_ok=True)

    # ── App icon ────────────────────────────────────────────────────────────
    # The launcher foreground draws on a 108 grid with the placement already
    # baked into its path data (see tools/generate_app_icon.py); reusing it here
    # keeps store icon and launcher icon identical rather than merely similar.
    icon_group = vector_to_svg_group(RES / "ic_launcher_foreground.xml", scale=512)
    (OUT / "play-icon-512.svg").write_text(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" '
        f'viewBox="0 0 512 512">\n'
        f'  <rect width="512" height="512" fill="{NAVY}"/>\n'
        f'{icon_group}\n</svg>\n', encoding="utf-8")

    # ── Feature graphic ─────────────────────────────────────────────────────
    # Shows the product, not just the logo: a real board with the real pieces,
    # in a position where the two-player framing is obvious.
    ranks = ["r1bqkb1r", "pppp1ppp", "2n2n2", "4p3",
             "2B1P3", "5N2", "PPPP1PPP", "RNBQK2R"]
    wm, ww, wh = wordmark("#F2F5F8")
    scale = 0.62
    feature = f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1024" y2="500" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#1E222B"/>
      <stop offset="1" stop-color="{NAVY}"/>
    </linearGradient>
  </defs>
  <rect width="1024" height="500" fill="url(#bg)"/>

  <!-- The wordmark is lightened: its start colour is nearly black and would
       otherwise vanish into this background. -->
  <g transform="translate(64,150) scale({scale})">
    {wm}
  </g>
  <text x="64" y="250" font-family="Roboto, DejaVu Sans, sans-serif" font-size="27" fill="#8FC4E2">The chess board for two players</text>
  <text x="64" y="300" font-family="Roboto, DejaVu Sans, sans-serif" font-size="23" fill="#9AA3B2">No engine &#183; no ads &#183; no tracking &#183; works offline</text>

  <!-- Slight tilt so the board reads as an object on a table rather than a
       screenshot pasted onto the canvas. -->
  <g transform="translate(660,32) rotate(-8 190 190)">
{board_svg(ranks, 0, 0, 47, border=10)}
  </g>
</svg>
'''
    (OUT / "play-feature.svg").write_text(feature, encoding="utf-8")
    print(f"{OUT / 'play-icon-512.svg'}\n{OUT / 'play-feature.svg'}")


if __name__ == "__main__":
    main()
