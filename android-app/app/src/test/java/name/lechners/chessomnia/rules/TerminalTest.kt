package name.lechners.chessomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalTest {

    private fun statusOf(pos: Position, rep: RepetitionTracker = RepetitionTracker()) =
        TerminalDetector.evaluate(pos, MoveGenerator.legalMoves(pos), rep)

    @Test
    fun foolsMate() {
        // 1. f3 e5 2. g4 Qh4#
        val pos = Position.startPosition()
        for (mv in listOf("f2f3", "e7e5", "g2g4", "d8h4")) {
            pos.makeMove(LongAlgebraic.parse(pos, mv) ?: error("move $mv is not legal"))
        }
        assertEquals(GameStatus.Checkmate(Side.BLACK), statusOf(pos))
    }

    @Test
    fun backRankMate() {
        val pos = Fen.parse("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1")
        pos.makeMove(LongAlgebraic.parse(pos, "a1a8")!!)
        assertEquals(GameStatus.Checkmate(Side.WHITE), statusOf(pos))
    }

    @Test
    fun smotheredMate() {
        val pos = Fen.parse("6rk/6pp/8/6N1/8/8/8/6K1 w - - 0 1")
        pos.makeMove(LongAlgebraic.parse(pos, "g5f7")!!)
        assertEquals(GameStatus.Checkmate(Side.WHITE), statusOf(pos))
    }

    @Test
    fun stalemate() {
        // Black to move, no legal move, but not in check
        assertEquals(GameStatus.Stalemate, statusOf(Fen.parse("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")))
    }

    @Test
    fun fiftyMoveRule() {
        assertEquals(GameStatus.DrawFiftyMove, statusOf(Fen.parse("4k3/8/8/8/8/8/4R3/4K3 w - - 100 80")))
        assertEquals(GameStatus.Ongoing, statusOf(Fen.parse("4k3/8/8/8/8/8/4R3/4K3 w - - 99 80")))
    }

    @Test
    fun halfmoveClockResetsOnPawnMoveAndCapture() {
        val pos = Fen.parse("4k3/8/8/3p4/4P3/8/8/4K3 w - - 17 30")
        pos.makeMove(LongAlgebraic.parse(pos, "e1e2")!!)
        assertEquals(18, pos.halfmoveClock)
        pos.makeMove(LongAlgebraic.parse(pos, "d5d4")!!)
        assertEquals("a pawn move resets it", 0, pos.halfmoveClock)

        val pos2 = Fen.parse("4k3/8/8/8/3n4/8/8/3RK3 w - - 40 30")
        pos2.makeMove(LongAlgebraic.parse(pos2, "d1d4")!!)
        assertEquals("a capture resets it", 0, pos2.halfmoveClock)
    }

    @Test
    fun threefoldRepetition() {
        val pos = Fen.parse("4k3/8/8/8/8/8/8/R3K2R w - - 0 1")
        val rep = RepetitionTracker()
        rep.push(RepetitionTracker.keyOf(pos))
        // Rooks shuffling back and forth: a1-b1-a1 / e8-d8-e8, twice
        for (mv in listOf("a1b1", "e8d8", "b1a1", "d8e8", "a1b1", "e8d8", "b1a1", "d8e8")) {
            pos.makeMove(LongAlgebraic.parse(pos, mv) ?: error("move $mv is not legal"))
            rep.push(RepetitionTracker.keyOf(pos))
        }
        assertEquals(GameStatus.DrawThreefold, statusOf(pos, rep))
    }

    /**
     * The classic false positive: two visually identical positions are NOT a repetition
     * when their castling rights differ.
     */
    @Test
    fun differentCastlingRightsAreNotARepetition() {
        val withRights = RepetitionTracker.keyOf(Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"))
        val withoutRights = RepetitionTracker.keyOf(Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1"))
        assertFalse("castling rights missing from the repetition key", withRights == withoutRights)
    }

    /** The ep square may only count when an en-passant capture is genuinely available. */
    @Test
    fun unusableEnPassantSquareDoesNotChangeTheKey() {
        val noCapturer = RepetitionTracker.keyOf(Fen.parse("4k3/8/8/3p4/8/8/8/4K3 w - d6 0 1"))
        val noEp = RepetitionTracker.keyOf(Fen.parse("4k3/8/8/3p4/8/8/8/4K3 w - - 0 1"))
        assertEquals(noEp, noCapturer)

        val withCapturer = RepetitionTracker.keyOf(Fen.parse("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"))
        assertTrue("a usable ep square must appear in the key", withCapturer.endsWith(" d6"))
    }

    @Test
    fun insufficientMaterial() {
        for (fen in listOf(
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1",          // K gegen K
            "4k3/8/8/8/8/8/8/3BK3 w - - 0 1",         // K+L gegen K
            "4k3/8/8/8/8/8/8/3NK3 w - - 0 1",         // K+S gegen K
            "2b1k3/8/8/8/8/8/8/3BK3 w - - 0 1",       // Laeufer gleicher Feldfarbe (c8/d1 hell)
        )) {
            assertTrue(fen, TerminalDetector.isInsufficientMaterial(Fen.parse(fen)))
        }

        for (fen in listOf(
            "3bk3/8/8/8/8/8/8/3BK3 w - - 0 1",        // Laeufer verschiedener Feldfarbe
            "4k3/8/8/8/8/8/8/2NNK3 w - - 0 1",        // K+N+N: mate is possible, so not dead
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",        // Bauer
            "4k3/8/8/8/8/8/8/3RK3 w - - 0 1",         // Turm
        )) {
            assertFalse(fen, TerminalDetector.isInsufficientMaterial(Fen.parse(fen)))
        }
    }

}
