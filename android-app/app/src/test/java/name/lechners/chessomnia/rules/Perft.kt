package name.lechners.chessomnia.rules

/** Counts every leaf node down to `depth` - the standard test for move generators. */
object Perft {

    fun count(pos: Position, depth: Int): Long {
        if (depth == 0) return 1L
        val moves = MoveGenerator.legalMoves(pos)
        if (depth == 1) return moves.size.toLong()
        var total = 0L
        for (m in moves) {
            val undo = pos.makeMove(m)
            total += count(pos, depth - 1)
            pos.unmakeMove(m, undo)
        }
        return total
    }

    /**
     * Nodes per root move. Comparing this list against `go perft` from a known engine
     * locates a generator bug in minutes rather than hours.
     */
    fun divide(pos: Position, depth: Int): Map<String, Long> {
        val result = LinkedHashMap<String, Long>()
        for (m in MoveGenerator.legalMoves(pos).sortedBy { LongAlgebraic.of(it) }) {
            val undo = pos.makeMove(m)
            result[LongAlgebraic.of(m)] = count(pos, depth - 1)
            pos.unmakeMove(m, undo)
        }
        return result
    }
}

/** Die sechs Standardstellungen samt Referenzzahlen. */
object PerftPositions {
    data class Case(val name: String, val fen: String, val expected: List<Long>)

    val ALL = listOf(
        Case(
            "Grundstellung", Fen.START,
            listOf(20L, 400L, 8_902L, 197_281L, 4_865_609L),
        ),
        Case(
            "Kiwipete",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            listOf(48L, 2_039L, 97_862L, 4_085_603L),
        ),
        Case(
            "Endspiel",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            listOf(14L, 191L, 2_812L, 43_238L, 674_624L),
        ),
        Case(
            "Position 4",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            listOf(6L, 264L, 9_467L, 422_333L),
        ),
        Case(
            "Position 5",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            listOf(44L, 1_486L, 62_379L, 2_103_487L),
        ),
        Case(
            "Position 6",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            listOf(46L, 2_079L, 89_890L),
        ),
    )
}
