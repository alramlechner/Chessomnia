package name.lechners.chessomnia.rules

/** The twelve pieces. Enum entries are singletons, so the board array does no boxing. */
enum class Piece(val side: Side, val type: PieceType) {
    W_PAWN(Side.WHITE, PieceType.PAWN),
    W_KNIGHT(Side.WHITE, PieceType.KNIGHT),
    W_BISHOP(Side.WHITE, PieceType.BISHOP),
    W_ROOK(Side.WHITE, PieceType.ROOK),
    W_QUEEN(Side.WHITE, PieceType.QUEEN),
    W_KING(Side.WHITE, PieceType.KING),
    B_PAWN(Side.BLACK, PieceType.PAWN),
    B_KNIGHT(Side.BLACK, PieceType.KNIGHT),
    B_BISHOP(Side.BLACK, PieceType.BISHOP),
    B_ROOK(Side.BLACK, PieceType.ROOK),
    B_QUEEN(Side.BLACK, PieceType.QUEEN),
    B_KING(Side.BLACK, PieceType.KING);

    val fenChar: Char
        get() = if (side == Side.WHITE) type.fenChar.uppercaseChar() else type.fenChar

    companion object {
        private val BY_SIDE_AND_TYPE: Array<Piece> =
            Array(12) { i -> entries[i] }

        fun of(side: Side, type: PieceType): Piece =
            BY_SIDE_AND_TYPE[side.ordinal * 6 + type.ordinal]

        fun fromFenChar(c: Char): Piece? {
            val type = PieceType.fromFenChar(c) ?: return null
            return of(if (c.isUpperCase()) Side.WHITE else Side.BLACK, type)
        }
    }
}
