package name.lechners.chessomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The best value-to-lines ratio in the whole project.
 *
 * make/unmake carries the legality filter AND the user's takeback. A castling right, ep
 * square or en-passant victim that is not restored would otherwise only show up three
 * moves later as an apparently inexplicable bug.
 */
class UnmakeTest {

    @Test
    fun roundTripRestoresFenExactly() {
        for (case in PerftPositions.ALL) {
            val pos = Fen.parse(case.fen)
            verify(pos, 2, case.name)
        }
    }

    private fun verify(pos: Position, depth: Int, label: String) {
        val before = Fen.serialize(pos)
        for (m in MoveGenerator.legalMoves(pos)) {
            val undo = pos.makeMove(m)
            if (depth > 1) verify(pos, depth - 1, label)
            pos.unmakeMove(m, undo)
            assertEquals("$label after ${LongAlgebraic.of(m)}", before, Fen.serialize(pos))
        }
    }

    @Test
    fun kingSquareIsRestored() {
        val pos = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        for (m in MoveGenerator.legalMoves(pos)) {
            val whiteBefore = pos.kingSquare(Side.WHITE)
            val blackBefore = pos.kingSquare(Side.BLACK)
            val undo = pos.makeMove(m)
            pos.unmakeMove(m, undo)
            assertEquals("White after ${LongAlgebraic.of(m)}", whiteBefore, pos.kingSquare(Side.WHITE))
            assertEquals("Black after ${LongAlgebraic.of(m)}", blackBefore, pos.kingSquare(Side.BLACK))
        }
    }
}
