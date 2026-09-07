package name.lechners.chessomnia.ui.game

import name.lechners.chessomnia.game.ChessGame
import name.lechners.chessomnia.rules.Fen
import name.lechners.chessomnia.rules.Side
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Taking a move back against the device.
 *
 * The rule looks like "undo two halfmoves" and is not: how many come off depends on whose
 * turn it was when the button was pressed.
 */
class TakebackTest {

    private fun game(vararg moves: String) =
        ChessGame.replay(Fen.START, moves.toList(), clockEnabled = false)

    /** The ordinary case: the device has answered, and both halfmoves come off. */
    @Test
    fun undoesBothHalfmovesAfterTheDeviceHasReplied() {
        val g = game("e2e4", "e7e5")
        takeBackToHumanTurn(g, Side.WHITE)
        assertEquals(0, g.moveCount)
        assertEquals(Side.WHITE, g.sideToMove)
    }

    /**
     * Pressed while the device is still thinking, only one's own move exists - taking off
     * two would undo a move the other side never made.
     */
    @Test
    fun undoesOnlyOneHalfmoveWhileTheDeviceIsStillThinking() {
        val g = game("e2e4")
        takeBackToHumanTurn(g, Side.WHITE)
        assertEquals(0, g.moveCount)
        assertEquals(Side.WHITE, g.sideToMove)
    }

    /**
     * ⚠️ The case that froze the board. Playing Black, the only move on the board is the
     * device's opening move; undoing it leaves the device to move with nothing left to
     * undo. The rule must stop rather than loop, and the caller has to let the device
     * move again - see `GameViewModel.takeback`.
     */
    @Test
    fun stopsWhenPlayingBlackAndOnlyTheDevicesOpeningMoveExists() {
        val g = game("e2e4")
        takeBackToHumanTurn(g, Side.BLACK)
        assertEquals(0, g.moveCount)
        assertEquals("the device is to move again", Side.WHITE, g.sideToMove)
    }

    @Test
    fun undoesBothHalfmovesWhenPlayingBlack() {
        val g = game("e2e4", "e7e5", "g1f3")
        takeBackToHumanTurn(g, Side.BLACK)
        assertEquals(1, g.moveCount)
        assertEquals(Side.BLACK, g.sideToMove)
    }

    /** Between two people nothing changes: one press, one halfmove. */
    @Test
    fun undoesExactlyOneHalfmoveWithoutAnOpponent() {
        val g = game("e2e4", "e7e5")
        takeBackToHumanTurn(g, null)
        assertEquals(1, g.moveCount)
        assertEquals(Side.BLACK, g.sideToMove)
    }

    @Test
    fun doesNothingOnAnEmptyBoard() {
        val g = game()
        takeBackToHumanTurn(g, Side.WHITE)
        assertEquals(0, g.moveCount)
        assertEquals(Side.WHITE, g.sideToMove)
    }
}
