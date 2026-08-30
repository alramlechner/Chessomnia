package name.lechners.chessomnia.data

import name.lechners.chessomnia.rules.Fen
import name.lechners.chessomnia.rules.LongAlgebraic
import name.lechners.chessomnia.rules.MoveGenerator
import name.lechners.chessomnia.rules.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BugReportTest {

    private val app = AppInfo("1.0.7", 8, "Samsung SM-X200", "15", 35)

    private fun dataFor(fen: String, moves: List<String> = emptyList()): BugReportData {
        val pos = Fen.parse(fen)
        val legal = MoveGenerator.legalMoves(pos)
        return BugReportData(
            app = app,
            timestamp = "2026-08-29 12:00:00",
            startFen = Fen.START,
            moves = moves,
            currentFen = fen,
            status = "Ongoing",
            sideToMove = pos.sideToMove,
            inCheck = name.lechners.chessomnia.rules.Attacks.isInCheck(pos, pos.sideToMove),
            legalMoves = legal.map { LongAlgebraic.of(it) }.sorted(),
            halfmoveClock = pos.halfmoveClock,
            fullmoveNumber = pos.fullmoveNumber,
            board = pos.board,
            whiteThinkMs = 65_000,
            blackThinkMs = 3_723_000,
            clockEnabled = true,
        )
    }

    /**
     * The actual purpose of the report: for "the mate was not detected" it must state in
     * black and white which move was still available. This position looks like mate, but
     * the king can escape to h8.
     */
    @Test
    fun listsTheEscapeMoveThatMakesItNoMate() {
        val text = BugReport.compose(
            "Black was checkmated, the app did not notice",
            dataFor("6k1/5Q2/6K1/8/8/8/8/8 b - - 0 1"),
        )
        assertTrue("move list missing", text.contains("LEGAL MOVES IN THIS POSITION (1)"))
        assertTrue("escape move missing", text.contains("g8h8"))
        assertTrue("description missing", text.contains("the app did not notice"))
    }

    @Test
    fun aRealMateShowsAnEmptyMoveList() {
        val text = BugReport.compose("", dataFor("6k1/6Q1/6K1/8/8/8/8/8 b - - 0 1"))
        assertTrue(text.contains("LEGAL MOVES IN THIS POSITION (0)"))
        assertTrue(text.contains("(none - checkmate or stalemate)"))
        assertTrue("an empty description must be marked as such", text.contains("(none given)"))
    }

    @Test
    fun containsEverythingNeededToReproduce() {
        val text = BugReport.compose("Test", dataFor(Fen.START, listOf("e2e4", "e7e5", "g1f3")))
        for (needle in listOf(
            "CHESSOMNIA BUG REPORT", "1.0.7 (8)", "Samsung SM-X200", "Android 15 (API 35)",
            "Starting position: ${Fen.START}", "FEN:", "Halfmove clock:", "Thinking time:",
        )) {
            assertTrue("missing: $needle", text.contains(needle))
        }
    }

    @Test
    fun movesAreNumberedInPairs() {
        assertEquals(
            "1. e2e4 e7e5   2. g1f3 b8c6   3. f1b5",
            BugReport.numberedMoves(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5")),
        )
        assertEquals("", BugReport.numberedMoves(emptyList()))
    }

    @Test
    fun asciiBoardShowsRankEightAtTheTop() {
        val lines = BugReport.asciiBoard(Fen.parse(Fen.START).board).lines()
        assertTrue("rank 8 on top", lines[1].startsWith("8 | r  n  b  q  k  b  n  r |"))
        assertTrue("rank 1 at the bottom", lines[8].startsWith("1 | R  N  B  Q  K  B  N  R |"))
        assertTrue("file labels", lines.last().contains("a  b  c  d  e  f  g  h"))
    }

    @Test
    fun clockIsOmittedWhenDisabled() {
        val d = dataFor(Fen.START).copy(clockEnabled = false)
        assertTrue(!BugReport.compose("x", d).contains("Thinking time:"))
    }
}
