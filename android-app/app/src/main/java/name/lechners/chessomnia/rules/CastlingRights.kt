package name.lechners.chessomnia.rules

/** Castling rights as a four-bit mask. */
object CastlingRights {
    const val NONE = 0
    const val WHITE_KING = 1
    const val WHITE_QUEEN = 2
    const val BLACK_KING = 4
    const val BLACK_QUEEN = 8
    const val ALL = WHITE_KING or WHITE_QUEEN or BLACK_KING or BLACK_QUEEN

    fun toFen(rights: Int): String {
        if (rights == NONE) return "-"
        val sb = StringBuilder()
        if (rights and WHITE_KING != 0) sb.append('K')
        if (rights and WHITE_QUEEN != 0) sb.append('Q')
        if (rights and BLACK_KING != 0) sb.append('k')
        if (rights and BLACK_QUEEN != 0) sb.append('q')
        return sb.toString()
    }

    fun fromFen(field: String): Int {
        if (field == "-") return NONE
        var r = NONE
        for (c in field) r = r or when (c) {
            'K' -> WHITE_KING
            'Q' -> WHITE_QUEEN
            'k' -> BLACK_KING
            'q' -> BLACK_QUEEN
            else -> throw IllegalArgumentException("Ungueltiges Rochadefeld: $field")
        }
        return r
    }
}
