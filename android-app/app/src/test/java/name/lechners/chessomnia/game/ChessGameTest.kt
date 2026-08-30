package name.lechners.chessomnia.game

import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.rules.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessGameTest {

    private fun game(clockEnabled: Boolean = true) = ChessGame.newGame(clockEnabled)

    private fun ChessGame.play(text: String, nowMs: Long = 0L) {
        val move = movesFrom(Square.parse(text.substring(0, 2)))
            .firstOrNull { name.lechners.chessomnia.rules.LongAlgebraic.of(it) == text }
            ?: error("move $text is not legal")
        apply(move, nowMs)
    }

    @Test
    fun clockSwitchesOnEveryMoveAndCountsUp() {
        val g = game()
        g.startClock(0L)
        assertEquals(Side.WHITE, g.clock.runningFor)
        g.play("e2e4", 5_000L)
        assertEquals(Side.BLACK, g.clock.runningFor)
        assertEquals("White thought for 5 s", 5_000L, g.clock.elapsed(Side.WHITE, 60_000L))
        g.play("e7e5", 9_000L)
        assertEquals(Side.WHITE, g.clock.runningFor)
        assertEquals("Black thought for 4 s", 4_000L, g.clock.elapsed(Side.BLACK, 60_000L))
    }

    /**
     * Whoever takes a move back also gets the thinking time they spent on it returned -
     * that is how a takeback works among friends. Afterwards the clock is paused so that
     * nobody loses time during the ensuing discussion.
     */
    @Test
    fun takebackRestoresPositionAndClockAndPauses() {
        val g = game()
        g.startClock(0L)
        g.play("e2e4", 20_000L)
        val fenAfter = g.fen()
        g.play("e7e5", 35_000L)

        g.takeback()
        assertEquals(fenAfter, g.fen())
        assertEquals(Side.BLACK, g.sideToMove)
        assertNull("the clock must be paused after a takeback", g.clock.runningFor)
        assertEquals("White unchanged", 20_000L, g.clock.elapsed(Side.WHITE, 999_999L))
        assertEquals("Black gets their thinking time back", 0L, g.clock.elapsed(Side.BLACK, 999_999L))
    }

    @Test
    fun takebackWorksAfterGameOver() {
        val g = game(clockEnabled = false)
        for (m in listOf("f2f3", "e7e5", "g2g4", "d8h4")) g.play(m)
        assertEquals(GameStatus.Checkmate(Side.BLACK), g.status)
        g.takeback()
        assertEquals("a takeback must reopen the game", GameStatus.Ongoing, g.status)
        assertTrue(g.movesFrom(Square.parse("d8")).isNotEmpty())
    }

    @Test
    fun resignCanBeUndoneByTakeback() {
        val g = game(clockEnabled = false)
        g.play("e2e4")
        g.resign(Side.WHITE)
        assertEquals(GameStatus.Resigned(Side.BLACK), g.status)
        g.takeback()
        assertEquals(GameStatus.Ongoing, g.status)
    }

    @Test
    fun takebackOnEmptyHistoryIsANoOp() {
        val g = game()
        assertFalse(g.canTakeback)
        assertNull(g.takeback())
    }

    /** The clock never ends the game - even after hours it simply keeps running. */
    @Test
    fun theClockNeverEndsTheGame() {
        val g = game()
        g.startClock(0L)
        g.play("e2e4", 3_600_000L)
        assertEquals(GameStatus.Ongoing, g.status)
        assertEquals(3_600_000L, g.clock.elapsed(Side.WHITE, 3_600_000L))
    }

    @Test
    fun replayReconstructsHistoryAndRepetition() {
        val moves = listOf("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8")
        val g = ChessGame.replay(name.lechners.chessomnia.rules.Fen.START, moves, clockEnabled = false)
        assertEquals(8, g.moveCount)
        assertEquals(
            "replaying must carry the repetition count along",
            GameStatus.DrawThreefold, g.status,
        )
        assertTrue("the undo history must exist", g.canTakeback)
    }

    /**
     * Read from the move history, not computed as the difference from the start position:
     * after a promotion that difference would be wrong - suddenly two queens and one pawn
     * fewer, without anything having been captured.
     */
    @Test
    fun capturedPiecesComeFromTheMoveHistory() {
        val g = ChessGame.newGame(clockEnabled = false)
        assertTrue(g.capturedBy(Side.WHITE).isEmpty())
        assertTrue(g.capturedBy(Side.BLACK).isEmpty())

        for (m in listOf("e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5e5")) g.play(m)
        assertEquals(
            listOf(name.lechners.chessomnia.rules.Piece.B_PAWN),
            g.capturedBy(Side.WHITE),
        )
        assertEquals(
            listOf(name.lechners.chessomnia.rules.Piece.W_PAWN),
            g.capturedBy(Side.BLACK),
        )
    }

    @Test
    fun promotionDoesNotCountAsACapture() {
        val g = ChessGame.replay(
            "4k3/P7/8/8/8/8/8/4K3 w - - 0 1",
            listOf("a7a8q"),
            clockEnabled = false,
        )
        assertTrue("a promotion is not a capture", g.capturedBy(Side.WHITE).isEmpty())
    }

    @Test
    fun takebackRemovesTheCapturedPieceAgain() {
        val g = ChessGame.newGame(clockEnabled = false)
        for (m in listOf("e2e4", "d7d5", "e4d5")) g.play(m)
        assertEquals(1, g.capturedBy(Side.WHITE).size)
        g.takeback()
        assertTrue(g.capturedBy(Side.WHITE).isEmpty())
    }

    @Test
    fun disabledClockNeverRuns() {
        val g = game(clockEnabled = false)
        g.startClock(0L)
        assertNull(g.clock.runningFor)
        g.play("e2e4", 1_000L)
        assertNull(g.clock.runningFor)
    }
}
