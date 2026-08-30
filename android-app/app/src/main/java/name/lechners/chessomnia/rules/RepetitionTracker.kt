package name.lechners.chessomnia.rules

/**
 * Counts position repetitions.
 *
 * The key is a normalised FEN string rather than a Zobrist hash: a game holds fewer than
 * 300 positions, about 18 kB. In exchange there is no collision risk, no PRNG table to
 * serialise, and the key is human-readable in a debugger.
 */
class RepetitionTracker {

    private val counts = HashMap<String, Int>()
    private val stack = ArrayList<String>()

    fun push(key: String) {
        stack.add(key)
        counts[key] = (counts[key] ?: 0) + 1
    }

    /** Needed by the takeback, which is why there is a stack beside the map. */
    fun pop() {
        if (stack.isEmpty()) return
        val key = stack.removeAt(stack.size - 1)
        val c = counts[key] ?: return
        if (c <= 1) counts.remove(key) else counts[key] = c - 1
    }

    fun count(key: String): Int = counts[key] ?: 0

    fun currentCount(): Int = if (stack.isEmpty()) 0 else count(stack[stack.size - 1])

    fun clear() { counts.clear(); stack.clear() }

    companion object {
        /**
         * The first four FEN fields.
         *
         * The en-passant field is only included when an en-passant capture is actually
         * available. Otherwise two visually identical positions would count as different
         * and a repetition would never be detected.
         */
        fun keyOf(pos: Position): String {
            val ep = pos.epTarget
            val epField =
                if (ep != null && hasEnPassantCapture(pos, ep)) ep.algebraic else "-"
            return buildString {
                append(Fen.piecePlacement(pos))
                append(' ').append(if (pos.sideToMove == Side.WHITE) 'w' else 'b')
                append(' ').append(CastlingRights.toFen(pos.castlingRights))
                append(' ').append(epField)
            }
        }

        private fun hasEnPassantCapture(pos: Position, ep: Square): Boolean {
            val us = pos.sideToMove
            for (d in intArrayOf(-1, 1)) {
                val from = ep.index - us.pawnPush + d
                if (!Square.isOnBoard(from)) continue
                val p = pos.board[from]
                if (p != null && p.side == us && p.type == PieceType.PAWN) return true
            }
            return false
        }
    }
}
