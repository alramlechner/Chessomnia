package name.lechners.chessomnia.engine

import name.lechners.chessomnia.game.ChessGame
import name.lechners.chessomnia.rules.Fen
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.LongAlgebraic
import name.lechners.chessomnia.rules.MoveGenerator
import name.lechners.chessomnia.rules.RepetitionTracker
import name.lechners.chessomnia.rules.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * What an opponent has to get right to be worth playing.
 *
 * The mate tests deliberately do not name the winning move. They play whatever the
 * engine chose through [ChessGame] and ask the rule engine whether that was mate - so
 * the test proves the opponent mates, not that it agrees with a move the test author
 * happened to like.
 */
class EngineTest {

    private fun engine(level: Level = Level.CLUB, seed: Int = 1) =
        Engine(level, Random(seed))

    private fun statusAfter(fen: String, moveText: String): GameStatus =
        ChessGame.replay(fen, listOf(moveText), clockEnabled = false).status

    @Test
    fun findsMateInOne() {
        val positions = listOf(
            "6k1/5ppp/8/8/8/8/8/R6K w - - 0 1",       // back rank
            "7k/8/6KQ/8/8/8/8/8 w - - 0 1",           // queen supported by the king
            "3r2k1/5ppp/8/8/8/8/5PPP/6K1 b - - 0 1",  // the same idea for Black
        )
        for (fen in positions) {
            val move = engine().chooseMove(Fen.parse(fen))
            assertNotNull("no move at all for $fen", move)
            val status = statusAfter(fen, LongAlgebraic.of(move!!))
            assertTrue(
                "expected mate after ${LongAlgebraic.of(move)} in $fen, got $status",
                status is GameStatus.Checkmate,
            )
        }
    }

    @Test
    fun seesAForcedMateInTwo() {
        // Kf6 + Qh1 against a king on h8: 1.Qh6+ Kg8 2.Qg7 mate.
        val scored = Search().run(
            root = Fen.parse("7k/8/5K2/8/8/8/8/7Q w - - 0 1"),
            maxDepth = 4,
            budgetMs = 5_000,
        )
        assertTrue(
            "expected a mate score, got ${scored.first().score}",
            scored.first().score >= Search.MATE_THRESHOLD,
        )
    }

    @Test
    fun takesTheFreeQueen() {
        val fen = "4k3/8/8/8/7q/8/8/R3K2R w KQ - 0 1"
        val move = engine(Level.CASUAL).chooseMove(Fen.parse(fen))
        assertEquals("h1h4", LongAlgebraic.of(move!!))
    }

    /**
     * The point of the weakest level, stated as a test.
     *
     * The other three take the queen every time, and that is exactly what made them
     * unplayable for a nine-year-old: a child leaves a piece hanging several times a
     * game, and an opponent that simply accepts the presents wins without ever having to
     * play well. [Level.LEARNING] measures its tolerance from the position it is already
     * standing in rather than from the best move it found, so a free queen no longer
     * stands out - it is one of many moves that do not make its own position worse.
     *
     * Both halves matter. Without the second assertion the level could be weak by being
     * random, which is a different and worse thing.
     */
    @Test
    fun theWeakestLevelUsuallyWalksPastAFreeQueen() {
        // Black's queen on g4 is simply hanging to Qxg4, with 27 legal moves to choose
        // from - an ordinary opening blunder rather than a forced endgame.
        val fen = "rnb1kbnr/pppp1ppp/8/4p3/6q1/4P3/PPPP1PPP/RNBQKBNR w KQkq - 0 1"
        val tries = 60

        fun grabs(level: Level, seed: Int): Boolean {
            val move = Engine(level, Random(seed)).chooseMove(Fen.parse(fen))!!
            return LongAlgebraic.of(move) == "d1g4"
        }

        var learning = 0
        var beginner = 0
        for (seed in 1..tries) {
            if (grabs(Level.LEARNING, seed)) learning++
            if (grabs(Level.BEGINNER, seed)) beginner++
        }
        assertTrue(
            "the weakest level took the queen $learning times out of $tries",
            learning < tries / 5,
        )
        assertEquals("BEGINNER is supposed to take it every time", tries, beginner)
    }

    /**
     * Careless, not suicidal. The tolerance is measured downwards from where the engine
     * already stands, so it may drift - a pawn here, a knight for a pawn there - but a
     * move that hands over a whole rook for nothing is further down than the tolerance
     * reaches, and stays out of the draw whatever the seed.
     *
     * Black's pawn on c5 attacks the rook on d4 and nothing defends it. Leaving it there
     * costs a rook for a pawn; every seed has to see that much.
     */
    @Test
    fun theWeakestLevelStillDoesNotHandOverARook() {
        val fen = "4k3/8/8/2p5/3R4/8/8/4K3 w - - 0 1"
        for (seed in 1..30) {
            val move = Engine(Level.LEARNING, Random(seed)).chooseMove(Fen.parse(fen))!!
            val game = ChessGame.replay(fen, listOf(LongAlgebraic.of(move)), clockEnabled = false)
            val reply = Engine(Level.CLUB, Random(seed)).chooseMove(Fen.parse(game.fen()))
            if (reply != null) game.apply(reply)
            assertTrue(
                "seed $seed played ${LongAlgebraic.of(move)} and lost the rook",
                materialOf(game.fen()) > 0,
            )
        }
    }

    /** White's material minus Black's, kings excluded. */
    private fun materialOf(fen: String): Int {
        var sum = 0
        for (piece in Fen.parse(fen).board) {
            if (piece == null) continue
            val value = Evaluation.valueOf(piece.type)
            sum += if (piece.side == Side.WHITE) value else -value
        }
        return sum
    }

    /**
     * Quiescence is what this really tests: without it the search stops one ply before
     * the recapture and happily gives the queen away for a pawn.
     */
    @Test
    fun doesNotTakeADefendedPawnWithTheQueen() {
        // The pawn on d5 is guarded by the one on e6; Qxd5 loses the queen.
        val fen = "4k3/8/4p3/3p4/8/8/8/3QK3 w - - 0 1"
        val move = engine(Level.CASUAL).chooseMove(Fen.parse(fen))
        assertTrue(
            "engine grabbed the guarded pawn: ${LongAlgebraic.of(move!!)}",
            LongAlgebraic.of(move) != "d1d5",
        )
    }

    @Test
    fun hasNothingToSayWhenTheGameIsOver() {
        // Mate on the board, Black to move.
        assertNull(engine().chooseMove(Fen.parse("R5k1/5ppp/8/8/8/8/8/6K1 b - - 0 1")))
        // Stalemate, Black to move.
        assertNull(engine().chooseMove(Fen.parse("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")))
    }

    @Test
    fun onlyEverReturnsALegalMove() {
        val positions = listOf(
            Fen.START,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
        )
        for (level in Level.entries) {
            for (fen in positions) {
                val position = Fen.parse(fen)
                val legal = MoveGenerator.legalMoves(position)
                val move = engine(level).chooseMove(position)
                assertTrue("$level played an illegal move in $fen", move in legal)
            }
        }
    }

    /** The engine must not disturb the position the UI thread still owns. */
    @Test
    fun leavesTheCallersPositionUntouched() {
        val position = Fen.parse(Fen.START)
        engine(Level.CASUAL).chooseMove(position)
        assertEquals(Fen.START, Fen.serialize(position))
    }

    @Test
    fun isReproducibleForAGivenSeed() {
        val position = Fen.parse(Fen.START)
        val first = Engine(Level.BEGINNER, Random(42)).chooseMove(position)
        val second = Engine(Level.BEGINNER, Random(42)).chooseMove(position)
        assertEquals(first, second)
    }

    /**
     * The time budget is a ceiling, not a promise. On a slow device the first iteration
     * has to finish anyway - a search that never completed depth one holds nothing but
     * zeroes, and the engine would pick a legal move at random.
     */
    @Test
    fun finishesTheFirstIterationEvenWithNoTimeAtAll() {
        val scored = Search().run(
            root = Fen.parse("4k3/8/8/8/7q/8/8/R3K2R w KQ - 0 1"),
            maxDepth = 6,
            budgetMs = 0,
        )
        assertEquals("h1h4", LongAlgebraic.of(scored.first().move))
    }

    /**
     * A cancelled search still has to hand back something legal: the caller has already
     * decided to throw the answer away, but a null or a crash there would be a bug in
     * the takeback path rather than a saved millisecond.
     */
    @Test
    fun survivesBeingCancelledImmediately() {
        val position = Fen.parse(Fen.START)
        val legal = MoveGenerator.legalMoves(position)
        val move = engine().chooseMove(position, isCancelled = { true })
        assertTrue(move in legal)
    }

    /**
     * A won position that has already occurred twice is a draw, not a win. The score
     * has to say so, otherwise the engine shuffles a winning endgame into a threefold.
     */
    @Test
    fun aRepetitionIsScoredAsADraw() {
        val fen = "4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1"
        val position = Fen.parse(fen)

        val plain = Search().run(position, maxDepth = 3, budgetMs = 5_000)
        val favourite = plain.first().move

        // Pretend the position after the favourite move has been on the board twice.
        val tracker = RepetitionTracker()
        val probe = position.copy()
        probe.makeMove(favourite)
        val key = RepetitionTracker.keyOf(probe)
        tracker.push(key)
        tracker.push(key)

        val guarded = Search().run(position, maxDepth = 3, budgetMs = 5_000, repetition = tracker)
        assertEquals(
            "the repetition should score as a draw",
            Evaluation.DRAW,
            guarded.first { it.move == favourite }.score,
        )
        assertTrue(
            "the engine should avoid the draw while it is winning",
            engine().chooseMove(position, repetition = tracker) != favourite,
        )
    }
}
