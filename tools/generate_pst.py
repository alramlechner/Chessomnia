#!/usr/bin/env python3
"""Generates the piece-square tables in `engine/Evaluation.kt`.

Every table here is *derived*, not tuned. Each one is the arithmetic consequence of
principles written down in this file — a knight's actual mobility, a pawn's distance
from promotion, a king's distance from the centre — so that the numbers can be
regenerated, checked against the stated reason, and argued about on chess grounds
rather than on "these are the values everyone uses".

That matters for more than tidiness. Piece-square tables are the single most-copied
artefact in computer chess: the tables from Tomasz Michniewski's *Simplified Evaluation
Function* on the Chess Programming Wiki appear in hundreds of projects, and the wiki's
content is CC BY-SA 3.0 — a share-alike licence that Chessomnia's Apache-2.0 cannot
satisfy. Deriving the tables from first principles removes the question instead of
answering it. See NOTICE.

The tables are deliberately *weak* by engine standards. Chessomnia's opponent tops out
at a good club amateur on purpose, and every term below is one of the handful of
principles a beginner is actually taught. There is no tuning loop here and there is not
meant to be one.

    python3 tools/generate_pst.py            # print the Kotlin block
    python3 tools/generate_pst.py --check    # compare against Evaluation.kt

Squares are addressed as (file, rank) with rank 0 = the moving side's first rank, and
printed rank 8 first to match how a board is drawn — the same orientation Evaluation.kt
reads them in.
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys

KOTLIN = pathlib.Path(__file__).resolve().parents[1] / (
    "android-app/app/src/main/java/name/lechners/chessomnia/engine/Evaluation.kt"
)

SQUARES = [(f, r) for r in range(8) for f in range(8)]


# ── Facts about the board, not opinions ─────────────────────────────────────────

def knight_mobility(f: int, r: int) -> int:
    """How many squares a knight actually reaches from here on an empty board."""
    steps = [(1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2)]
    return sum(1 for df, dr in steps if 0 <= f + df < 8 and 0 <= r + dr < 8)


def bishop_mobility(f: int, r: int) -> int:
    """Diagonal squares reachable on an empty board: 7 in a corner, 13 in the centre."""
    total = 0
    for df, dr in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        x, y = f + df, r + dr
        while 0 <= x < 8 and 0 <= y < 8:
            total += 1
            x += df
            y += dr
    return total


def centre_distance(f: int, r: int) -> int:
    """Manhattan distance from the centre of the board, doubled: 2 in the middle, 14 in a corner."""
    return abs(2 * f - 7) + abs(2 * r - 7)


# ── The tables ──────────────────────────────────────────────────────────────────

def knight() -> list[int]:
    """A knight is worth what it can reach, and it is worth nothing sitting at home.

    Mobility on an empty board runs from 2 (corner) to 8 (centre) and averages 5.25.
    Scaled by 7 that spans roughly a third of a pawn either way, which is about what a
    badly placed knight really costs. The extra penalty on the first rank is the one
    piece of opening advice every beginner is given: get the knights out.

    Mobility alone is flat across the whole middle of the board - every square from c3
    to f6 reaches eight - so a small centrality term is added on top. Two knights that
    reach equally many squares are not equally good: the one nearer the middle reaches
    more of the *opponent's* half.
    """
    out = []
    for f, r in SQUARES:
        v = round((knight_mobility(f, r) - 5.25) * 7)
        v += 8 - centre_distance(f, r)
        if r == 0:
            v -= 12
        out.append(v)
    return out


def bishop() -> list[int]:
    """Same idea, on the diagonals, plus the fianchetto squares a beginner is shown.

    Bishop mobility runs 7..13 and averages 8.75. The scale is smaller than the
    knight's because a bishop's reach depends far more on the pawns in front of it than
    on which square it stands on — a table cannot see that, so it should not pretend to.
    """
    out = []
    for f, r in SQUARES:
        v = round((bishop_mobility(f, r) - 8.75) * 5)
        if r == 0:
            v -= 12
        if r == 1 and f in (1, 6):
            v += 8
        out.append(v)
    return out


def rook() -> list[int]:
    """A rook does not care where it stands, it cares which file it stands on.

    Rook mobility is 14 from every square on an empty board, so mobility says nothing
    here and the table is built from the two things that do matter: the seventh rank,
    and the open middle files. The small bonus on d1/e1 is where a rook belongs once
    the king has castled away from it.
    """
    out = []
    central_file = [0, 0, 3, 9, 9, 3, 0, 0]
    for f, r in SQUARES:
        v = central_file[f]
        if r == 6:
            v += 20
        if r == 7:
            v += 6
        if r == 0 and f in (3, 4):
            v += 5
        out.append(v)
    return out


def queen() -> list[int]:
    """Deliberately almost flat.

    A queen's placement is decided by tactics, not by squares, and a table with strong
    opinions about it only teaches the engine to shove the queen out early. What is
    left is her mobility (21..27, averaging 23.75) at a small scale, and a nudge off
    the back rank once she has somewhere to go.
    """
    out = []
    for f, r in SQUARES:
        v = round((14 + bishop_mobility(f, r) - 23.75) * 3)
        if r == 0:
            v -= 4
        out.append(v)
    return out


def pawn_mg() -> list[int]:
    """Advance, the centre, and the two pawns that lock in the bishops.

    The rank bonus is deliberately slow at the start and steep near the end: a pawn on
    the sixth is a real asset, a pawn on the third is just a pawn. The centre bonus
    applies while the pawn is still in the middlegame part of the board. The penalty on
    d2/e2 is why beginners are told to push a centre pawn on move one — until one of
    them moves, both bishops are looking at their own pawn.
    """
    advance = [0, 0, 5, 12, 24, 42, 68, 0]
    centre = [0, 0, 5, 11, 11, 5, 0, 0]
    out = []
    for f, r in SQUARES:
        v = advance[r]
        if 2 <= r <= 4:
            v += centre[f]
        if r == 1 and f in (3, 4):
            v -= 14
        out.append(v)
    return out


def pawn_eg() -> list[int]:
    """In an endgame a pawn's only virtue is how far it has come.

    No file term at all: with the pieces gone, a rook's pawn one square from queening
    beats a centre pawn on its third rank, and the table should say so plainly.
    """
    advance = [0, 0, 12, 28, 52, 86, 130, 0]
    return [advance[r] for f, r in SQUARES]


def king_mg() -> list[int]:
    """While the queens are on, the king wants a corner and a roof.

    Two separate penalties, because they are two separate mistakes: walking up the
    board, and standing on an open central file. The bonus on the castled squares is
    what makes the engine castle without being told to — nothing else in the evaluation
    rewards it.
    """
    up = [0, 14, 28, 42, 52, 58, 62, 62]
    exposed = [0, 0, 8, 20, 20, 8, 0, 0]
    castled = [10, 18, 18, 0, 0, 18, 18, 10]
    out = []
    for f, r in SQUARES:
        v = -up[r] - exposed[f]
        if r == 0:
            v += castled[f]
        out.append(v)
    return out


def king_eg() -> list[int]:
    """With the queens gone the king is a piece again, and pieces belong in the middle.

    Pure centrality, nothing else. This is also the term that has to survive next to
    the mate drive in Evaluation.mateDrive without fighting it, which is why it is a
    smooth cone rather than a plateau.
    """
    return [round((8 - centre_distance(f, r)) * 5) for f, r in SQUARES]


TABLES = {
    "PAWN_MG": pawn_mg,
    "PAWN_EG": pawn_eg,
    "KNIGHT_PST": knight,
    "BISHOP_PST": bishop,
    "ROOK_PST": rook,
    "QUEEN_PST": queen,
    "KING_MG": king_mg,
    "KING_EG": king_eg,
}


def assert_mirrored(name: str, values: list[int]) -> None:
    """Both sides of the board have to read alike.

    Every table here is used for White and mirrored for Black, so a table that is not
    symmetric across the files would make the engine prefer one wing for no reason -
    and would quietly break `EvaluationTest.startPositionIsBalanced`, which is the only
    thing that would ever notice.
    """
    for f, r in SQUARES:
        left = values[r * 8 + f]
        right = values[r * 8 + (7 - f)]
        if left != right:
            raise AssertionError(
                f"{name} is not mirrored: file {f} rank {r} is {left}, "
                f"file {7 - f} is {right}"
            )


def as_kotlin(name: str, values: list[int]) -> str:
    """Rank 8 first, the way Evaluation.kt reads and a board is drawn."""
    rows = []
    for r in range(7, -1, -1):
        row = ", ".join(f"{values[r * 8 + f]}" for f in range(8))
        rows.append(f"        {row},")
    body = "\n".join(rows)
    return f"    private val {name} = intArrayOf(\n{body}\n    )"


def parse_kotlin(name: str, source: str) -> list[int] | None:
    """The table as Evaluation.kt currently holds it, back in rank-0-first order."""
    m = re.search(rf"val {name} = intArrayOf\((.*?)\)", source, re.S)
    if not m:
        return None
    printed = [int(x) for x in re.findall(r"-?\d+", m.group(1))]
    if len(printed) != 64:
        return None
    return [printed[(7 - r) * 8 + f] for r in range(8) for f in range(8)]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--check", action="store_true", help="compare against Evaluation.kt")
    args = ap.parse_args()

    if args.check:
        source = KOTLIN.read_text()
        bad = 0
        for name, fn in TABLES.items():
            have = parse_kotlin(name, source)
            want = fn()
            assert_mirrored(name, want)
            if have is None:
                print(f"{name}: not found in Evaluation.kt")
                bad += 1
            elif have != want:
                n = sum(1 for a, b in zip(have, want) if a != b)
                print(f"{name}: differs from the derivation in {n} of 64 squares")
                bad += 1
        if bad:
            print(f"\n{bad} table(s) out of step. Regenerate with: python3 {sys.argv[0]}")
            return 1
        print(f"all {len(TABLES)} tables match their derivation")
        return 0

    for name, fn in TABLES.items():
        values = fn()
        assert_mirrored(name, values)
        print(as_kotlin(name, values))
        print()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
