package name.lechners.chessomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-comparison against an INDEPENDENT implementation.
 *
 * `reference_positions.txt` comes from python-chess (see
 * tools/generate_reference_corpus.py) and holds, for every position, the complete list of
 * legal moves plus the game status. That way our own engine is not checked against
 * itself - the weak point of every hand-built test, and for mate detection precisely the
 * point that matters.
 *
 * The corpus is deliberately dense around terminal positions: random games alone barely
 * reach mate, and without mate positions it would not answer the decisive question.
 */
class ReferenceCorpusTest {

    private data class Case(val fen: String, val status: String, val moves: List<String>)

    private fun load(): List<Case> {
        val stream = javaClass.classLoader!!.getResourceAsStream("reference_positions.txt")
            ?: error("reference_positions.txt fehlt")
        return stream.bufferedReader().useLines { lines ->
            lines.map { line ->
                val parts = line.split("|")
                Case(parts[0], parts[1], if (parts[2].isBlank()) emptyList() else parts[2].split(" "))
            }.toList()
        }
    }

    @Test
    fun legalMovesMatchTheReferenceEverywhere() {
        val cases = load()
        assertTrue("corpus too small: ${cases.size}", cases.size > 4000)

        for (case in cases) {
            val pos = Fen.parse(case.fen)
            val mine = MoveGenerator.legalMoves(pos).map { LongAlgebraic.of(it) }.sorted()
            assertEquals("moves in ${case.fen}", case.moves.sorted(), mine)
        }
    }

    /**
     * The heart of the question "does the app detect mate?". For every position in the
     * corpus our own status has to match the reference - in particular a mate must never
     * pass as a stalemate, or the other way round.
     */
    @Test
    fun terminalStatusMatchesTheReference() {
        val cases = load()
        var mates = 0
        var stalemates = 0

        for (case in cases) {
            val pos = Fen.parse(case.fen)
            val moves = MoveGenerator.legalMoves(pos)
            val status = TerminalDetector.evaluate(pos, moves, RepetitionTracker())

            when (case.status) {
                "CHECKMATE" -> {
                    assertEquals(
                        "mate not detected: ${case.fen}",
                        GameStatus.Checkmate(pos.sideToMove.opposite), status,
                    )
                    mates++
                }
                "STALEMATE" -> {
                    assertEquals("stalemate not detected: ${case.fen}", GameStatus.Stalemate, status)
                    stalemates++
                }
                "INSUFFICIENT" -> assertEquals(
                    "dead material not detected: ${case.fen}",
                    GameStatus.DrawInsufficientMaterial, status,
                )
                "FIFTY_MOVE" -> assertEquals(
                    "fifty-move rule not detected: ${case.fen}",
                    GameStatus.DrawFiftyMove, status,
                )
                "ONGOING" -> assertTrue(
                    "ongoing game wrongly ended (${status}): ${case.fen}",
                    status == GameStatus.Ongoing,
                )
            }
        }

        assertTrue("corpus holds too few mate positions: $mates", mates > 500)
        assertTrue("corpus holds too few stalemate positions: $stalemates", stalemates > 300)
    }
}
