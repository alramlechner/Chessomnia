#!/usr/bin/env python3
"""
Renders the "testers wanted" graphic for sharing in messengers.

Like tools/render_store_assets.py, everything is built from what the app
actually ships -- the same board colours, the same piece drawables, the same
generated wordmark -- so the poster cannot advertise something the app is not.

    python3 tools/render_promo.py
    rsvg-convert store/promo-tester-de.svg -w 1080 -h 1080 -o store/promo-tester-de.png

Produces:
    store/promo-tester-de.svg     1080x1080, German, for WhatsApp and the like

Square rather than 16:9: a square image is shown whole inside a WhatsApp chat
bubble, while a wide one is cropped to a letterbox strip and loses the ask at
the bottom.

The text is set in Lato because that is what is installed here; the wordmark is
the real Montserrat one and carries the brand. Small type is Semibold or heavier
throughout -- messengers re-encode shared images, and thin light type at 30 px
is the first thing that turns to mush.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from render_store_assets import board_svg, wordmark, NAVY, BLUE  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "store"

W = H = 1080
CX = W / 2

# The same position as the feature graphic: 30 pieces still on, so it reads as
# "a chess board" even at the size a chat preview gives it.
RANKS = ["r1bqkb1r", "pppp1ppp", "2n2n2", "4p3",
         "2B1P3", "5N2", "PPPP1PPP", "RNBQK2R"]

BOARD = 470
BOARD_X = CX - BOARD / 2
BOARD_Y = 248
SQUARE = BOARD / 8


def text(y, s, size, fill, weight="400", family="Lato", spacing=None):
    ls = f' letter-spacing="{spacing}"' if spacing else ""
    return (f'<text x="{CX}" y="{y}" text-anchor="middle" '
            f'font-family="{family}, DejaVu Sans, sans-serif" font-size="{size}" '
            f'font-weight="{weight}" fill="{fill}"{ls}>{s}</text>')


def main():
    wm, ww, wh = wordmark("#F2F5F8")
    wm_scale = 560 / ww

    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="{W}" y2="{H}" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#1E222B"/>
      <stop offset="1" stop-color="{NAVY}"/>
    </linearGradient>
  </defs>
  <rect width="{W}" height="{H}" fill="url(#bg)"/>

  <!-- Wordmark lightened: its own gradient starts near black and would sink
       into this background. -->
  <g transform="translate({CX - 560 / 2},74) scale({wm_scale})">
    {wm}
  </g>
  {text(196, "Das Schachbrett f&#252;r zwei.", 38, "#8FC4E2", "300")}

  <!-- Tilted so it reads as a board on a table, not a screenshot pasted on. -->
  <g transform="rotate(-7 {CX} {BOARD_Y + BOARD / 2})">
{board_svg(RANKS, BOARD_X, BOARD_Y, SQUARE, border=10)}
  </g>

  {text(858, "TESTER GESUCHT", 86, "#FFFFFF", "900", spacing="2")}
  {text(910, "12 Leute, 14 Tage &#8211; dann darf die App in den Play Store.", 32, "#9AA3B2", "400")}

  <rect x="90" y="952" width="{W - 180}" height="66" rx="33" fill="#3193C61F" stroke="{BLUE}" stroke-width="2"/>
  {text(995, "kostenlos &#183; keine Werbung &#183; kein Konto &#183; offline", 31, "#8FC4E2", "600")}

  {text(1052, "Nur Android &#183; schreib mir, dann bekommst du den Link", 25, "#98A1B0", "600")}
</svg>
'''
    (OUT / "promo-tester-de.svg").write_text(svg, encoding="utf-8")
    print(OUT / "promo-tester-de.svg")


if __name__ == "__main__":
    main()
