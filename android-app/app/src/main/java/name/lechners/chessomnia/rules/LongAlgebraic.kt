package name.lechners.chessomnia.rules

/**
 * Long algebraic notation: "e2e4", "e1g1" (castling as a king move), "e7e8q".
 *
 * Deliberately not SAN: this format is unambiguous without position context when
 * writing, short enough for persistence, and directly readable in tests. A SAN display
 * belongs in the UI layer, should it ever be needed.
 */
object LongAlgebraic {

    fun of(move: Move): String =
        move.from.algebraic + move.to.algebraic + (move.promotion?.fenChar ?: "")

    /** Looks up the matching legal move, so `kind` and promotion are always correct. */
    fun parse(pos: Position, text: String): Move? {
        val legal = MoveGenerator.legalMoves(pos)
        return legal.firstOrNull { of(it) == text }
    }
}
