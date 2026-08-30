package name.lechners.chessomnia.rules

/**
 * A square on the 0x88 board: `index = rank * 16 + file`, a1 = 0, h8 = 119.
 *
 * The gain over an 8x8 array is the off-board test: a single AND instead of file and
 * rank comparisons at every place where a piece walks an offset. It is exactly in those
 * repeated comparisons that off-board bugs otherwise hide.
 *
 * This representation stays inside the rule engine. Conversion to screen rows happens
 * exclusively in the UI (BoardGeometry).
 */
@JvmInline
value class Square(val index: Int) {

    /** 0 = a-file ... 7 = h-file */
    val file: Int get() = index and 7

    /** 0 = rank 1 ... 7 = rank 8 */
    val rank: Int get() = index shr 4

    val algebraic: String get() = "${'a' + file}${rank + 1}"

    /** Square colour - used by the bishop test for insufficient material. */
    val isLightSquare: Boolean get() = (file + rank) and 1 == 1

    override fun toString(): String = algebraic

    companion object {
        /**
         * Masks the 0x88 bits plus everything outside 0..127, which keeps the test
         * correct for negative indices too. A plain `and 0x88` would not be: -256, for
         * example, has none of the 0x88 bits set.
         */
        private const val OFF_BOARD_MASK: Int = -120 // 0xFFFFFF88

        fun isOnBoard(index: Int): Boolean = (index and OFF_BOARD_MASK) == 0

        fun of(file: Int, rank: Int) = Square(rank * 16 + file)

        /** 0..63 with a1 = 0, h8 = 63 - for outward-facing interfaces only. */
        fun fromIndex64(i: Int) = Square((i / 8) * 16 + (i % 8))

        fun parse(s: String): Square {
            require(s.length == 2) { "invalid square: $s" }
            val f = s[0].lowercaseChar() - 'a'
            val r = s[1] - '1'
            require(f in 0..7 && r in 0..7) { "invalid square: $s" }
            return of(f, r)
        }

        // Frequently used squares (castling)
        val A1 = Square(0); val B1 = Square(1); val C1 = Square(2); val D1 = Square(3)
        val E1 = Square(4); val F1 = Square(5); val G1 = Square(6); val H1 = Square(7)
        val A8 = Square(112); val B8 = Square(113); val C8 = Square(114); val D8 = Square(115)
        val E8 = Square(116); val F8 = Square(117); val G8 = Square(118); val H8 = Square(119)
    }
}

/** 0..63 with a1 = 0, h8 = 63. */
fun Square.toIndex64(): Int = rank * 8 + file
