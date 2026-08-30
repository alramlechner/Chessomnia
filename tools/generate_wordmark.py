#!/usr/bin/env python3
"""
Generates the Chessomnia wordmark (res/drawable/logo_wordmark.xml) from the
Montserrat typeface.

The wordmark is not a drawing but typesetting: the word CHESSOMNIA set in
Montserrat Light (Julieta Ulanovsky et al., SIL Open Font License 1.1), converted
to outlines and given the app's gradient. Generating it from the font rather than
storing a hand-edited path keeps the provenance complete and reproducible.

The font file is deliberately not kept in the repository -- only the outlines go
into the app, and under the OFL a document set with a font is not itself subject
to the OFL. Fetch it when needed:

    curl -sSLO https://raw.githubusercontent.com/JulietaUla/Montserrat/master/fonts/ttf/Montserrat-Light.ttf
    python3 tools/generate_wordmark.py Montserrat-Light.ttf \
        android-app/app/src/main/res/drawable/logo_wordmark.xml \
        branding/chessomnia-wordmark.svg

Requires python3-fonttools (Debian: apt install python3-fonttools).

Typesetting
-----------
Capitals, Light weight (300) and the font's natural advance widths -- no extra
tracking.

The stemless E
--------------
The one departure from plain typesetting: the E is set without its vertical stem,
as three free-standing bars (CH=SSOMNIA). The bars are not guessed but derived
from the contour of the real Montserrat E, so they keep the font's stroke weight,
its heights and its differing bar lengths; the glyph's advance width is unchanged,
so the remaining letters do not shift.
"""
import sys
import xml.sax.saxutils as sax

from fontTools.misc.transform import Transform
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.recordingPen import RecordingPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

TEXT = "CHESSOMNIA"

# App palette (ui/theme/Color.kt). The board colours are deliberately independent.
COLOR_START = "#2B303E"
COLOR_END = "#3193C6"

FONT_SIZE = 100.0   # arbitrary; the path is normalised against PAD afterwards anyway
PAD = 8.0           # margin left/right so the gradient does not start on the glyph

HEIGHT_DP = 23      # the width follows from the aspect ratio


def glyph_names(font):
    """Character -> glyph name via the best available cmap."""
    cmap = font.getBestCmap()
    missing = [c for c in TEXT if ord(c) not in cmap]
    if missing:
        raise SystemExit(f"font does not contain these characters: {missing}")
    return [cmap[ord(c)] for c in TEXT]


def stemless_e_bars(glyph_set, name):
    """Derives the three bars of the stemless E from the real glyph contour.

    The Montserrat E is a single twelve-point contour with straight edges only.
    In drawing order, starting bottom left, counter-clockwise:

        0 bottom-left     1 top-left        2 top-right       3
        4 (stem edge)     5                 6 middle bar      7
        8                 9 (stem edge)    10 bottom-right   11

    That yields the stem edge (point 4), the y bands, and the right edge of each
    bar, which differs per bar. If the contour does not match this pattern the
    script aborts rather than silently drawing something wrong.
    """
    rec = RecordingPen()
    glyph_set[name].draw(rec)

    pts = []
    for op, args in rec.value:
        if op in ("moveTo", "lineTo"):
            pts.append(args[0])
        elif op == "closePath":
            continue
        else:
            raise SystemExit(
                f"E glyph '{name}' contains {op} - a pure polygon contour was expected. "
                f"The stemless E would have to be derived again."
            )
    if len(pts) != 12:
        raise SystemExit(
            f"E glyph '{name}' has {len(pts)} points instead of 12 - font version "
            f"changed? The stemless E would have to be derived again."
        )

    x_stem = pts[4][0]                      # right edge of the vertical stem
    if not (pts[4][0] == pts[5][0] == pts[8][0] == pts[9][0] > pts[0][0]):
        raise SystemExit("E contour deviates from the expected pattern (stem edge).")

    # (y_bottom, y_top, x_right) per bar
    return x_stem, [
        (pts[11][1], pts[9][1],                          pts[10][0]),  # bottom
        (pts[7][1],  pts[5][1],                          pts[6][0]),   # middle
        (pts[3][1],  pts[1][1],                          pts[2][0]),   # top
    ]


def draw_stemless_e(pen, glyph_set, name):
    """Draws the E as three rectangles - without the vertical stem."""
    x_stem, bars = stemless_e_bars(glyph_set, name)
    for lo, hi, x1 in bars:
        pen.moveTo((x_stem, lo))
        pen.lineTo((x1, lo))
        pen.lineTo((x1, hi))
        pen.lineTo((x_stem, hi))
        pen.closePath()


def layout(font):
    """Lays out TEXT and returns (svg path data, minX, minY, maxX, maxY)."""
    upem = font["head"].unitsPerEm
    scale = FONT_SIZE / upem
    glyphs = font.getGlyphSet()
    hmtx = font["hmtx"]

    names = glyph_names(font)

    # The SVGPathPen collects all ten letters into ONE path. Android renders
    # several subpaths in a single <path> without trouble, and one path keeps the
    # file small.
    # One decimal is plenty at a 100-unit em: the wordmark is rendered a few hundred
    # pixels wide, so a tenth of a unit is far below one pixel. Full float precision
    # would roughly double the path data for no visible gain, and Android's lint warns
    # about long vector paths for good reason.
    svg_pen = SVGPathPen(glyphs, ntos=lambda v: f"{v:.1f}".rstrip("0").rstrip("."))
    bounds_pen = BoundsPen(glyphs)

    e_name = font.getBestCmap()[ord("E")]

    x = 0.0
    for name in names:
        # y is mirrored: fonts count upwards, SVG and Android downwards.
        t = Transform(scale, 0, 0, -scale, x, 0)
        for pen in (svg_pen, bounds_pen):
            tp = TransformPen(pen, t)
            if name == e_name:
                draw_stemless_e(tp, glyphs, name)
            else:
                glyphs[name].draw(tp)
        # The advance width stays that of the unmodified glyph, so the rest of
        # the wordmark sits exactly as it would in plain typesetting.
        x += hmtx[name][0] * scale

    if bounds_pen.bounds is None:
        raise SystemExit("no outline produced - wrong font file?")
    return svg_pen.getCommands(), bounds_pen.bounds


SVG_TEMPLATE = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}">
  <!-- Chessomnia wordmark. GENERATED by tools/generate_wordmark.py from
       {font} ({version}), SIL Open Font License 1.1.
       Source for store graphics; the app uses res/drawable/logo_wordmark.xml. -->
  <defs>
    <linearGradient id="chessomnia" gradientUnits="userSpaceOnUse" x1="{pad}" y1="0" x2="{gx2}" y2="0">
      <stop offset="0" stop-color="{c0}"/>
      <stop offset="1" stop-color="{c1}"/>
    </linearGradient>
  </defs>
  <g transform="translate({dx},{dy})"><path fill="url(#chessomnia)" d="{d}"/></g>
</svg>
"""


def main(argv):
    if len(argv) not in (3, 4):
        raise SystemExit(
            f"usage: {argv[0]} <Montserrat-Light.ttf> <out.xml> [out.svg]"
        )
    ttf_path, out_path = argv[1], argv[2]
    svg_path = argv[3] if len(argv) == 4 else None

    font = TTFont(ttf_path)
    path_data, (min_x, min_y, max_x, max_y) = layout(font)

    # Normalise onto a viewport that starts at PAD on the left and top. The path
    # itself is not recomputed; the <group> translation does that at draw time.
    width = round(max_x - min_x + 2 * PAD, 2)
    height = round(max_y - min_y + 2 * PAD, 2)
    dx = round(PAD - min_x, 2)
    dy = round(PAD - min_y, 2)
    width_dp = round(HEIGHT_DP * width / height)

    name = font["name"].getDebugName(4) or "Montserrat"
    version = font["name"].getDebugName(5) or "?"

    xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!--
  Chessomnia wordmark. GENERATED - do not edit by hand.
  Source: tools/generate_wordmark.py, set in {sax.escape(name)} ({sax.escape(version)}),
  SIL Open Font License 1.1 (licenses/OFL-1.1-Montserrat.txt). Only the outlines
  go into the app; the font file itself is not redistributed.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="{width_dp}dp"
    android:height="{HEIGHT_DP}dp"
    android:viewportWidth="{width}"
    android:viewportHeight="{height}">
    <group android:translateX="{dx}" android:translateY="{dy}">
        <path android:pathData="{path_data}">
            <aapt:attr name="android:fillColor">
                <gradient
                    android:type="linear"
                    android:startX="{PAD}"
                    android:startY="0"
                    android:endX="{round(width - PAD, 2)}"
                    android:endY="0">
                    <item android:offset="0" android:color="{COLOR_START}" />
                    <item android:offset="1" android:color="{COLOR_END}" />
                </gradient>
            </aapt:attr>
        </path>
    </group>
</vector>
"""
    with open(out_path, "w", encoding="utf-8") as fh:
        fh.write(xml)

    print(f"{out_path}: {width}x{height} viewport, {width_dp}x{HEIGHT_DP} dp, "
          f"{len(path_data)} chars of path data")

    if svg_path:
        with open(svg_path, "w", encoding="utf-8") as fh:
            fh.write(SVG_TEMPLATE.format(
                w=width, h=height, pad=PAD, gx2=round(width - PAD, 2),
                c0=COLOR_START, c1=COLOR_END, dx=dx, dy=dy, d=path_data,
                font=sax.escape(name), version=sax.escape(version),
            ))
        print(f"{svg_path}: SVG version for store graphics")


if __name__ == "__main__":
    main(sys.argv)
