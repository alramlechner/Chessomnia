package name.lechners.chessomnia.engine

import name.lechners.chessomnia.game.ChessGame
import name.lechners.chessomnia.rules.Fen
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.RepetitionTracker
import name.lechners.chessomnia.rules.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Whole games, played by the engine against itself.
 *
 * These are the tests that decide whether the opponent is worth shipping. Finding a mate
 * in one proves very little: the classic failure of a small engine is the won endgame it
 * never finishes, shuffling a queen around until the fifty-move rule ends the game in a
 * draw. Nothing but playing it out catches that.
 *
 * Every move goes through [ChessGame.apply], which refuses anything the rule engine does
 * not list as legal - so these double as an end-to-end legality check.
 */
class GamePlayTest {

    private class Playout(val game: ChessGame, val halfmoves: Int)

    private fun playOut(startFen: String, level: Level, maxHalfmoves: Int): Playout {
        val game = ChessGame.replay(startFen, emptyList(), clockEnabled = false)
        val engines = mapOf(
            Side.WHITE to Engine(level, Random(7)),
            Side.BLACK to Engine(level, Random(8)),
        )
        val repetition = RepetitionTracker()
        repetition.push(RepetitionTracker.keyOf(Fen.parse(game.fen())))

        var halfmoves = 0
        while (!game.status.isOver && halfmoves < maxHalfmoves) {
            val position = Fen.parse(game.fen())
            val move = engines.getValue(game.sideToMove).chooseMove(position, repetition) ?: break
            assertTrue(
                "the engine offered a move the rule engine rejects: $move",
                game.apply(move),
            )
            repetition.push(RepetitionTracker.keyOf(Fen.parse(game.fen())))
            halfmoves++
        }
        return Playout(game, halfmoves)
    }

    @Test
    fun matesWithQueenAndKing() {
        val result = playOut("4k3/8/8/8/8/8/8/3QK3 w - - 0 1", Level.CASUAL, maxHalfmoves = 120)
        assertEquals(
            "queen and king failed to mate in ${result.halfmoves} halfmoves " +
                "(ended as ${result.game.status})",
            GameStatus.Checkmate(Side.WHITE),
            result.game.status,
        )
    }

    /**
     * Harder than the queen: the rook cannot cover the escape squares on its own, so
     * this only works if the king is driven along as well.
     */
    @Test
    fun matesWithRookAndKing() {
        val result = playOut("4k3/8/8/8/8/8/8/R3K3 w - - 0 1", Level.CLUB, maxHalfmoves = 160)
        assertEquals(
            "rook and king failed to mate in ${result.halfmoves} halfmoves " +
                "(ended as ${result.game.status})",
            GameStatus.Checkmate(Side.WHITE),
            result.game.status,
        )
    }

    /**
     * A full game from the opening. It is not asserted who wins - only that the engine
     * keeps producing legal moves for a hundred halfmoves and that the game either ends
     * properly or is simply still running.
     */
    @Test
    fun playsAFullGameWithoutGettingStuck() {
        val result = playOut(Fen.START, Level.BEGINNER, maxHalfmoves = 100)
        assertTrue(
            "the game stopped after only ${result.halfmoves} halfmoves as ${result.game.status}",
            result.halfmoves >= 20,
        )
    }
}
