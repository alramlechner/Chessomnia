package name.lechners.chessomnia.engine

import name.lechners.chessomnia.rules.Fen
import name.lechners.chessomnia.rules.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluationTest {

    @Test
    fun startPositionIsBalanced() {
        assertEquals(0, Evaluation.evaluate(Position.startPosition()))
    }

    /**
     * The single most important property of the score: it is read from the side to
     * move. A sign that survives here but is lost inside the negamax would make the
     * engine play its opponent's best move.
     */
    @Test
    fun theSameBoardIsMirroredForTheOtherSide() {
        val whiteToMove = Fen.parse("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")
        val blackToMove = Fen.parse("4k3/8/8/8/8/8/8/3QK3 b - - 0 1")
        assertEquals(
            Evaluation.evaluate(whiteToMove),
            -Evaluation.evaluate(blackToMove),
        )
    }

    @Test
    fun aQueenUpIsWorthAboutAQueen() {
        val score = Evaluation.evaluate(Fen.parse("4k3/8/8/8/8/8/8/3QK3 w - - 0 1"))
        assertTrue("expected a queen's worth, got $score", score > 700)
    }

    @Test
    fun materialOutweighsPlacement() {
        // Black has a rook against a knight, and no square is worth 180 centipawns.
        val score = Evaluation.evaluate(Fen.parse("4k2r/8/8/3N4/8/8/8/4K3 w k - 0 1"))
        assertTrue("expected Black to be better, got $score", score < 0)
    }

    /**
     * The bare-king term is the one piece of evaluation not written symmetrically, so
     * it is checked for both colours. Without it the engine wins the queen and then
     * shuffles until the fifty-move rule.
     */
    @Test
    fun aBareKingIsDrivenTowardsTheEdge() {
        val centre = Evaluation.evaluate(Fen.parse("8/8/8/3k4/8/8/8/3QK3 w - - 0 1"))
        val corner = Evaluation.evaluate(Fen.parse("k7/8/8/8/8/8/8/3QK3 w - - 0 1"))
        assertTrue("White should prefer the cornered king ($corner vs $centre)", corner > centre)
    }

    @Test
    fun aBareKingIsDrivenTowardsTheEdgeForBlackToo() {
        val centre = Evaluation.evaluate(Fen.parse("8/8/8/3K4/8/8/8/3qk3 b - - 0 1"))
        val corner = Evaluation.evaluate(Fen.parse("K7/8/8/8/8/8/8/3qk3 b - - 0 1"))
        assertTrue("Black should prefer the cornered king ($corner vs $centre)", corner > centre)
    }
}
