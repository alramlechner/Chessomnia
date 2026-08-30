package name.lechners.chessomnia.rules

enum class PieceType(val fenChar: Char) {
    PAWN('p'), KNIGHT('n'), BISHOP('b'), ROOK('r'), QUEEN('q'), KING('k');

    companion object {
        fun fromFenChar(c: Char): PieceType? =
            entries.firstOrNull { it.fenChar == c.lowercaseChar() }
    }
}
