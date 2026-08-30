package name.lechners.chessomnia.ui.game

import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameResultTextTest {

    @Test
    fun eachPlayerSeesTheirOwnOutcome() {
        val mate = GameStatus.Checkmate(Side.WHITE)
        assertEquals(Outcome.WON, outcomeFor(mate, Side.WHITE))
        assertEquals(Outcome.LOST, outcomeFor(mate, Side.BLACK))

        val resign = GameStatus.Resigned(Side.BLACK)
        assertEquals(Outcome.LOST, outcomeFor(resign, Side.WHITE))
        assertEquals(Outcome.WON, outcomeFor(resign, Side.BLACK))
    }

    @Test
    fun drawsLookTheSameFromBothSides() {
        for (status in listOf(
            GameStatus.Stalemate, GameStatus.DrawFiftyMove, GameStatus.DrawThreefold,
            GameStatus.DrawInsufficientMaterial, GameStatus.AgreedDraw,
        )) {
            assertEquals(Outcome.DRAW, outcomeFor(status, Side.WHITE))
            assertEquals(Outcome.DRAW, outcomeFor(status, Side.BLACK))
        }
    }

    /** Every ending has to be justified - an empty line would be a bug. */
    @Test
    fun everyEndingHasAReason() {
        for (status in listOf(
            GameStatus.Checkmate(Side.WHITE), GameStatus.Resigned(Side.BLACK),
            GameStatus.Stalemate, GameStatus.DrawFiftyMove, GameStatus.DrawThreefold,
            GameStatus.DrawInsufficientMaterial, GameStatus.AgreedDraw,
        )) {
            assertNotEquals("no reason for $status", R.string.empty, reasonFor(status).id)
        }
        assertEquals(R.string.empty, reasonFor(GameStatus.Ongoing).id)
    }

    /**
     * The winner has to appear in the reason. Since localisation the text itself is a
     * resource, so what is checked is that the argument names the right side. This is
     * exactly where the bug used to sit that showed both players the same winner.
     */
    @Test
    fun theWinnerIsNamedInTheReason() {
        val mate = reasonFor(GameStatus.Checkmate(Side.WHITE))
        assertEquals(R.string.reason_checkmate, mate.id)
        assertEquals(R.string.side_white, mate.arg)

        val resign = reasonFor(GameStatus.Resigned(Side.BLACK))
        assertEquals(R.string.reason_resigned, resign.id)
        assertEquals(R.string.side_black, resign.arg)
    }

    /** Without an argument none may be set - otherwise a raw "%s" would leak through. */
    @Test
    fun reasonsWithoutAWinnerCarryNoArgument() {
        for (status in listOf(
            GameStatus.Stalemate, GameStatus.DrawFiftyMove, GameStatus.DrawThreefold,
            GameStatus.DrawInsufficientMaterial, GameStatus.AgreedDraw,
        )) {
            assertNull("unexpected argument for $status", reasonFor(status).arg)
        }
    }
}
