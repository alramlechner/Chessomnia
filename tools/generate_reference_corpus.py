#!/usr/bin/env python3
"""
Builds a comparison corpus using python-chess as an INDEPENDENT reference.

This keeps our own rule engine from being checked against itself and pits it against a
foreign, widely used implementation instead: for every position the complete list of
legal moves and the game status.

Deliberately dense around terminal positions: whenever a mating move is available it is
usually played. Random games alone barely reach mate, and a corpus without mates would
fail to answer exactly the question that matters.

Usage:   python3 generate_reference_corpus.py <out.txt>
Format:  FEN|STATUS|move1 move2 ...

Requires python-chess (pip install chess). It is a development-only dependency and is
not redistributed with the app; see tools/README.md on licensing.
"""
import random
import sys

import chess


def status_of(board: chess.Board) -> str:
    if board.is_checkmate():
        return "CHECKMATE"
    if board.is_stalemate():
        return "STALEMATE"
    if board.is_insufficient_material():
        return "INSUFFICIENT"
    # python-chess does not report the fifty-move rule by itself: under FIDE it is
    # claimable, not automatic. Chessomnia deliberately declares the draw on its own at a
    # home board - so the corpus has to mirror that.
    if board.halfmove_clock >= 100:
        return "FIFTY_MOVE"
    return "ONGOING"


def row_of(board: chess.Board) -> str:
    return "%s|%s|%s" % (
        board.fen(), status_of(board),
        " ".join(sorted(m.uci() for m in board.legal_moves)),
    )


def play(board: chess.Board, rnd: random.Random, plies: int, prefer_mate: bool):
    """Plays on at random and yields every position visited."""
    for _ in range(plies):
        yield board.copy(stack=False)
        moves = list(board.legal_moves)
        if not moves:
            return
        if prefer_mate:
            mating = [m for m in moves if board.gives_check(m) and is_mate(board, m)]
            if mating and rnd.random() < 0.9:
                board.push(rnd.choice(mating))
                continue
        board.push(rnd.choice(moves))


# Low-material starting positions: stalemates only arise there at all
ENDGAME_FENS = [
    "8/8/8/4k3/8/8/8/3QK3 w - - 0 1",
    "8/8/8/4k3/8/8/8/3RK3 w - - 0 1",
    "8/8/4k3/8/8/8/4P3/4K3 w - - 0 1",
    "8/5k2/8/8/8/8/1Q6/4K3 b - - 0 1",
    "6k1/8/8/8/8/8/6QK/8 b - - 0 1",
    "8/8/8/8/2k5/8/1R6/4K3 w - - 0 1",
]


def main(target: str) -> None:
    rnd = random.Random(20260829)
    terminal: dict[str, str] = {}
    ongoing: dict[str, str] = {}

    # A cap per status: without it "dead material" swamps everything else, because
    # K-vs-K is the end state of nearly every endgame.
    LIMITS = {"CHECKMATE": 800, "STALEMATE": 500, "INSUFFICIENT": 400,
              "FIFTY_MOVE": 200, "ONGOING": 3000}
    counts: dict[str, int] = {}

    def collect(board, _unused=None):
        st = status_of(board)
        if counts.get(st, 0) >= LIMITS[st]:
            return
        key = board.fen()
        bucket = terminal if st != "ONGOING" else ongoing
        if key in bucket:
            return
        if st == "ONGOING" and rnd.random() >= 0.02:
            return
        bucket[key] = row_of(board)
        counts[st] = counts.get(st, 0) + 1

    # Full games: these supply mates and ordinary middlegame positions
    for _ in range(900):
        for b in play(chess.Board(), rnd, 240, prefer_mate=True):
            collect(b)

    # Endgames: this is where stalemate and dead material arise
    for fen in ENDGAME_FENS:
        for _ in range(2500):
            for b in play(chess.Board(fen), rnd, 120, prefer_mate=False):
                collect(b)

    rows = list(terminal.values()) + list(ongoing.values())
    rows.sort()
    with open(target, "w") as fh:
        for line in rows:
            fh.write(line + "\n")

    print(f"{len(rows)} positions: " + ", ".join(f"{v}x {k}" for k, v in sorted(counts.items())))


def is_mate(board: chess.Board, move: chess.Move) -> bool:
    board.push(move)
    try:
        return board.is_checkmate()
    finally:
        board.pop()


if __name__ == "__main__":
    main(sys.argv[1])
