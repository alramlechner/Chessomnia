package name.lechners.chessomnia.rules

/** Offset tables for the 0x88 board plus the attack test. */
object Attacks {

    val KNIGHT = intArrayOf(-33, -31, -18, -14, 14, 18, 31, 33)
    val BISHOP = intArrayOf(-17, -15, 15, 17)
    val ROOK = intArrayOf(-16, -1, 1, 16)
    val KING = intArrayOf(-17, -16, -15, -1, 1, 15, 16, 17)

    /**
     * Is `square` attacked by `by`?
     *
     * Used by the legality filter (is our own king in check?), by the castling
     * conditions, and by the UI's check highlight.
     */
    fun isSquareAttacked(pos: Position, square: Int, by: Side): Boolean {
        val board = pos.board

        // Pawns are checked backwards: a white pawn attacking `square` stands on
        // square-15 or square-17.
        val push = by.pawnPush
        for (d in intArrayOf(-1, 1)) {
            val from = square - push + d
            if (!Square.isOnBoard(from)) continue
            val p = board[from]
            if (p != null && p.side == by && p.type == PieceType.PAWN) return true
        }

        for (o in KNIGHT) {
            val from = square + o
            if (!Square.isOnBoard(from)) continue
            val p = board[from]
            if (p != null && p.side == by && p.type == PieceType.KNIGHT) return true
        }

        for (o in KING) {
            val from = square + o
            if (!Square.isOnBoard(from)) continue
            val p = board[from]
            if (p != null && p.side == by && p.type == PieceType.KING) return true
        }

        if (slidingAttack(pos, square, by, BISHOP, PieceType.BISHOP)) return true
        if (slidingAttack(pos, square, by, ROOK, PieceType.ROOK)) return true

        return false
    }

    private fun slidingAttack(
        pos: Position,
        square: Int,
        by: Side,
        offsets: IntArray,
        straight: PieceType,
    ): Boolean {
        val board = pos.board
        for (o in offsets) {
            var sq = square + o
            while (Square.isOnBoard(sq)) {
                val p = board[sq]
                if (p != null) {
                    if (p.side == by && (p.type == straight || p.type == PieceType.QUEEN)) return true
                    break
                }
                sq += o
            }
        }
        return false
    }

    fun isInCheck(pos: Position, side: Side): Boolean =
        pos.hasKing(side) && isSquareAttacked(pos, pos.kingSquare(side).index, side.opposite)
}
