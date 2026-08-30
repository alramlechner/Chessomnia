package name.lechners.chessomnia.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class FiftyMoveIndicatorTest {

    @Test
    fun countsDownFromFifty() {
        assertEquals(50, remainingMoves(0))
        assertEquals(50, remainingMoves(1))
        assertEquals(49, remainingMoves(2))
        assertEquals(25, remainingMoves(50))
    }

    /** Rounded up: at 99 halfmoves exactly one is left, not none. */
    @Test
    fun theLastMoveStillCounts() {
        assertEquals(1, remainingMoves(98))
        assertEquals(1, remainingMoves(99))
        assertEquals(0, remainingMoves(100))
    }

    @Test
    fun neverGoesNegative() {
        assertEquals(0, remainingMoves(120))
    }
}
