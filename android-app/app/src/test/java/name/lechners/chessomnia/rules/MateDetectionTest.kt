package name.lechners.chessomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Mate detection, attacked from several angles.
 *
 * The detection itself is trivial ("no legal moves + in check"), so only two things can
 * go wrong: the move generator returns one move too many (and the mate disappears), or
 * `isInCheck` is wrong (and the app reports STALEMATE instead of mate - exactly the
 * symptom behind "the mate was not detected").
 *
 * The second case has a plausible cause: `Position` tracks the king square in a field
 * rather than searching for it. If that ever drifted, `isInCheck` would be silently
 * wrong. The cross-check below aims at precisely that.
 */
class MateDetectionTest {

    // ── Known mate positions ────────────────────────────────────────────────────

    // Every one of these is cross-checked with python-chess. Five "mate positions"
    // that used to stand here were not mates - the engine had been right every time.
    private val mates = listOf(
        "Narrenmatt" to "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3",
        "Grundreihenmatt" to "R5k1/5ppp/8/8/8/8/8/6K1 b - - 0 1",
        "Ersticktes Matt" to "6rk/5Npp/8/8/8/8/8/6K1 b - - 0 1",
        "Damenmatt am Rand" to "6k1/6Q1/6K1/8/8/8/8/8 b - - 0 1",
        "Turmmatt zu zweit" to "R6k/1R6/8/8/8/8/8/6K1 b - - 0 1",
        "Zwei Laeufer" to "7k/5B2/5BK1/8/8/8/8/8 b - - 0 1",
        "Doppelschach" to "R2k4/8/3K4/8/8/8/8/1B6 b - - 0 1",
        "Matt durch Umwandlung" to "5k1Q/8/5K2/8/8/8/8/8 b - - 0 1",
        "Epaulettenmatt" to "3rkr2/4Q3/4K3/8/8/8/8/8 b - - 0 1",
        "Laeufer und Springer" to "7k/8/5BKN/8/8/8/8/8 b - - 0 1",
    )

    @Test
    fun knownMatesAreDetected() {
        for ((name, fen) in mates) {
            val pos = Fen.parse(fen)
            val moves = MoveGenerator.legalMoves(pos)
            assertTrue("$name: moves are still available ${moves.map { LongAlgebraic.of(it) }}", moves.isEmpty())
            assertTrue("$name: the king is not in check", Attacks.isInCheck(pos, pos.sideToMove))
            assertEquals(
                "$name",
                GameStatus.Checkmate(pos.sideToMove.opposite),
                TerminalDetector.evaluate(pos, moves, RepetitionTracker()),
            )
        }
    }

    /** The counter-test: stalemate must NEVER be reported as mate, or the other way round. */
    @Test
    fun knownStalematesAreNotReportedAsMate() {
        val stalemates = listOf(
            "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1",
            "k7/8/1Q6/8/8/8/8/7K b - - 0 1",
            "7k/8/6QK/8/8/8/8/8 b - - 0 1",
        )
        for (fen in stalemates) {
            val pos = Fen.parse(fen)
            val moves = MoveGenerator.legalMoves(pos)
            assertTrue("$fen: moves are still available", moves.isEmpty())
            assertEquals(fen, GameStatus.Stalemate, TerminalDetector.evaluate(pos, moves, RepetitionTracker()))
        }
    }

    /** Positions that LOOK like mate but are not. */
    @Test
    fun nearMissesAreNotMate() {
        val notMate = listOf(
            // The king can capture the checking queen
            "6k1/6Q1/8/8/8/8/8/6K1 b - - 0 1" to "Dame ist ungedeckt schlagbar",
            // A rook can interpose
            "R5k1/8/8/8/8/8/6r1/6K1 b - - 0 1" to "Turm kann blocken",
        )
        for ((fen, why) in notMate) {
            val pos = Fen.parse(fen)
            val moves = MoveGenerator.legalMoves(pos)
            assertTrue("$why: should have had moves", moves.isNotEmpty())
            assertEquals(why, GameStatus.Ongoing, TerminalDetector.evaluate(pos, moves, RepetitionTracker()))
        }
    }

    // ── Cross-check over many random games ──────────────────────────────────────

    /**
     * Plays random games and checks after EVERY halfmove that:
     *  - the tracked king square agrees with a board scan,
     *  - `isInCheck` agrees with an independent brute-force test,
     *  - with no legal moves the status is exactly mate or stalemate, never "ongoing".
     */
    @Test
    fun invariantsHoldAcrossRandomGames() {
        val rnd = Random(20260829)
        var mateCount = 0
        var stalemateCount = 0

        repeat(400) {
            val pos = Position.startPosition()
            repeat(220) {
                checkKingSquareCache(pos)
                checkInCheckAgainstBruteForce(pos)

                val moves = MoveGenerator.legalMoves(pos)
                if (moves.isEmpty()) {
                    val status = TerminalDetector.evaluate(pos, moves, RepetitionTracker())
                    val inCheck = bruteForceInCheck(pos, pos.sideToMove)
                    if (inCheck) {
                        assertEquals(Fen.serialize(pos), GameStatus.Checkmate(pos.sideToMove.opposite), status)
                        mateCount++
                    } else {
                        assertEquals(Fen.serialize(pos), GameStatus.Stalemate, status)
                        stalemateCount++
                    }
                    return@repeat
                }
                pos.makeMove(moves[rnd.nextInt(moves.size)])
            }
        }

        // Without real terminal positions the test would be worthless - so demand them.
        assertTrue("no mate position reached ($mateCount)", mateCount > 0)
        assertTrue("no stalemate position reached ($stalemateCount)", stalemateCount > 0)
    }

    /** The cached king square must not drift across make/unmake either. */
    @Test
    fun kingSquareSurvivesMakeUnmakeEverywhere() {
        val rnd = Random(4711)
        repeat(60) {
            val pos = Position.startPosition()
            repeat(60) {
                val moves = MoveGenerator.legalMoves(pos)
                if (moves.isEmpty()) return@repeat
                for (m in moves) {
                    val undo = pos.makeMove(m)
                    checkKingSquareCache(pos)
                    pos.unmakeMove(m, undo)
                    checkKingSquareCache(pos)
                }
                pos.makeMove(moves[rnd.nextInt(moves.size)])
            }
        }
    }

    // ── Helpers: deliberately naive, so they share nothing with production code ───

    private fun checkKingSquareCache(pos: Position) {
        for (side in Side.entries) {
            val scanned = scanForKing(pos, side)
            assertEquals(
                "Koenigsposition ${side} weicht ab in ${Fen.serialize(pos)}",
                scanned, pos.kingSquare(side).index,
            )
        }
    }

    private fun checkInCheckAgainstBruteForce(pos: Position) {
        for (side in Side.entries) {
            assertEquals(
                "isInCheck ${side} weicht ab in ${Fen.serialize(pos)}",
                bruteForceInCheck(pos, side),
                Attacks.isInCheck(pos, side),
            )
        }
    }

    private fun scanForKing(pos: Position, side: Side): Int {
        for (i in 0 until 128) {
            if (!Square.isOnBoard(i)) continue
            val p = pos.board[i] ?: continue
            if (p.side == side && p.type == PieceType.KING) return i
        }
        return -1
    }

    /**
     * An independent check test: generates every pseudo-legal move of the other side and
     * looks whether one of them hits the king. Deliberately shares no logic with
     * `Attacks.isSquareAttacked` - otherwise the test would be checking itself.
     */
    private fun bruteForceInCheck(pos: Position, side: Side): Boolean {
        val kingSquare = scanForKing(pos, side)
        if (kingSquare < 0) return false
        val probe = pos.copy()
        probe.sideToMove = side.opposite
        return MoveGenerator.pseudoLegalMoves(probe).any { it.to.index == kingSquare }
    }
}
