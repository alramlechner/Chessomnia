#!/usr/bin/env python3
"""
Captures Google Play screenshots from a real device over ADB.

Play requires screenshots to show the real app, so nothing here is rendered or
mocked: the script drives the installed build by tapping squares, exactly as a
player would, and grabs the framebuffer.

    ADB_SERIAL=<host:port> python3 tools/capture_screenshots.py [outdir]

The device is addressed through ADB_SERIAL and has no default on purpose -- a
device address is local to whoever runs this and does not belong in the
repository.

Positions are reached by playing real moves rather than by injecting state. It
costs a few taps and buys something worth more: whatever is on screen is a
position the rules engine actually produced, so a screenshot can never show
something the app would not do.

⚠️ Each scenario starts from `pm clear`, so the app's settings and any saved game
on the device are reset. Use a test device, not the one with the game you care
about.
"""
import os
import re
import subprocess
import sys
import time
from io import BytesIO

from PIL import Image

SERIAL = os.environ.get("ADB_SERIAL")
if not SERIAL:
    raise SystemExit("set ADB_SERIAL=<host:port> (see the module docstring)")

PKG = "name.lechners.chessomnia"
ADB = ["adb", "-s", SERIAL]

# The board colours from ui/theme/Color.kt. Used to locate the board on screen
# instead of hard-coding a layout that every device would break.
LIGHT_SQ = (0xF0, 0xD9, 0xB5)
DARK_SQ = (0xB5, 0x88, 0x63)
BUTTON = (0x31, 0x93, 0xC6)


def sh(*args, binary=False):
    r = subprocess.run(ADB + list(args), capture_output=True, timeout=120)
    if r.returncode:
        raise SystemExit(f"adb {' '.join(args)} failed: {r.stderr.decode()[:200]}")
    return r.stdout if binary else r.stdout.decode(errors="replace")


def cap():
    return Image.open(BytesIO(sh("exec-out", "screencap", "-p", binary=True))).convert("RGB")


def tap(x, y, settle=0.6):
    sh("shell", "input", "tap", str(int(x)), str(int(y)))
    time.sleep(settle)


def near(px, ref, tol=26):
    return all(abs(a - b) <= tol for a, b in zip(px, ref))


def find_region(img, ref, step=4):
    """Bounding box of everything matching a colour. None if absent."""
    w, h = img.size
    px = img.load()
    xs, ys = [], []
    for y in range(0, h, step):
        for x in range(0, w, step):
            if near(px[x, y], ref):
                xs.append(x)
                ys.append(y)
    if not xs:
        return None
    return min(xs), min(ys), max(xs), max(ys)


def find_board(img):
    """(x0, y0, square_size). The board is the only thing in these two colours."""
    boxes = [find_region(img, c) for c in (LIGHT_SQ, DARK_SQ)]
    boxes = [b for b in boxes if b]
    if not boxes:
        raise SystemExit("no board on screen -- is the game showing?")
    x0 = min(b[0] for b in boxes)
    y0 = min(b[1] for b in boxes)
    x1 = max(b[2] for b in boxes)
    y1 = max(b[3] for b in boxes)
    return x0, y0, (x1 - x0) / 8.0


def square_xy(sq, board):
    """Algebraic square -> screen centre, with white at the bottom."""
    x0, y0, s = board
    file_ = ord(sq[0]) - ord("a")
    rank = int(sq[1])
    return x0 + (file_ + 0.5) * s, y0 + (8 - rank + 0.5) * s


def play(moves, board, settle=0.55):
    """Each move is 'e2e4': tap the piece, then its destination."""
    for m in moves:
        tap(*square_xy(m[:2], board), settle=settle)
        tap(*square_xy(m[2:4], board), settle=settle)


def night(on):
    sh("shell", "cmd", "uimode", "night", "yes" if on else "no")
    time.sleep(2.5)


def demo_status_bar(on):
    """A neutral status bar: 12:00, full battery, no stray notification icons."""
    sh("shell", "settings", "put", "global", "sysui_demo_allowed", "1" if on else "0")
    if on:
        sh("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
           "-e", "command", "enter")
        sh("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
           "-e", "command", "clock", "-e", "hhmm", "1200")
        sh("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
           "-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false")
        sh("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
           "-e", "command", "notifications", "-e", "visible", "false")
    else:
        sh("shell", "am", "broadcast", "-a", "com.android.systemui.demo",
           "-e", "command", "exit")
    time.sleep(1)


def fresh_game():
    """Back to a known state: cleared app, new game, board on screen."""
    sh("shell", "pm", "clear", PKG)
    sh("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(3.5)
    img = cap()
    box = find_region(img, BUTTON)
    if not box:
        raise SystemExit("no primary button on the home screen")
    tap((box[0] + box[2]) / 2, (box[1] + box[3]) / 2, settle=2.5)
    return find_board(cap())


# name, dark theme?, moves to play, square to select for the final shot
SCENARIOS = [
    ("01-opening-move-options", False,
     ["e2e4", "e7e5", "g1f3", "b8c6"], "f1"),          # bishop's options fan out
    ("02-castling-available", True,
     ["e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5"], "e1"),
    ("03-capture-offered", False,
     ["e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6"], "c4"),   # Bxf7 on offer
    ("04-en-passant", True,
     ["e2e4", "a7a6", "e4e5", "d7d5"], "e5"),
    ("05-check", False,
     ["e2e4", "e7e5", "g1f3", "f7f6", "f3e5", "f6e5", "d1h5"], None),
    ("06-promotion-dialog", True,
     ["e2e4", "d7d5", "e4d5", "c7c6", "d5c6", "a7a6", "c6b7", "a6a5", "b7a8"], None),
    ("07-checkmate", False,
     ["e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"], None),
]


def main():
    outdir = sys.argv[1] if len(sys.argv) > 1 else "/tmp/shots/play"
    os.makedirs(outdir, exist_ok=True)
    demo_status_bar(True)
    try:
        for name, dark, moves, select in SCENARIOS:
            night(dark)
            board = fresh_game()
            play(moves, board)
            if select:
                tap(*square_xy(select, board), settle=1.2)
            time.sleep(1.2)
            path = os.path.join(outdir, f"{name}-{'dark' if dark else 'light'}.png")
            cap().save(path)
            print(f"{path}  {Image.open(path).size}")
    finally:
        demo_status_bar(False)
        night(False)


if __name__ == "__main__":
    main()
