package name.lechners.chessomnia.data

import kotlinx.serialization.json.Json
import name.lechners.chessomnia.engine.Level
import name.lechners.chessomnia.rules.Fen
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Resignation, an agreed draw and a timeout do NOT follow from the move list - they have
 * to survive process death separately.
 */
class GameSnapshotTest {

    private val START_FEN = Fen.START

    /**
     * The opponent has to survive process death with the game.
     *
     * If it did not, an app restart would quietly turn a game against the device into a
     * two-player one: the board would simply stop answering, with nothing on screen
     * explaining why.
     */
    @Test
    fun theOpponentRoundTrips() {
        for (level in Level.entries) {
            for (side in Side.entries) {
                val config = OpponentConfig(level, side)
                val (encodedLevel, encodedSide) = GameSnapshot.encodeOpponent(config)
                assertEquals(config, GameSnapshot.decodeOpponent(encodedLevel, encodedSide))
            }
        }
    }

    @Test
    fun aGameBetweenTwoPeopleStoresNoOpponent() {
        val (level, side) = GameSnapshot.encodeOpponent(null)
        assertNull(level)
        assertNull(side)
        assertNull(GameSnapshot.decodeOpponent(null, null))
    }

    /**
     * A game saved by a version that had no opponent yet has to load as what it was.
     * Half a record - one field set, the other not - is treated the same way.
     */
    @Test
    fun anUnreadableOpponentLoadsAsTwoPlayers() {
        assertNull(GameSnapshot.decodeOpponent("GRANDMASTER", "WHITE"))
        assertNull(GameSnapshot.decodeOpponent("CLUB", "GREEN"))
        assertNull(GameSnapshot.decodeOpponent("CLUB", null))
        assertNull(GameSnapshot.decodeOpponent(null, "WHITE"))
    }

    /**
     * The real backwards-compatibility guarantee: a save written before the opponent
     * existed has no such fields at all, and must still load.
     */
    @Test
    fun aSaveFromBeforeTheOpponentStillLoads() {
        val old = """
            {"v":2,"start_fen":"$START_FEN","moves":["e2e4"],
             "clock_enabled":true,"white_ms":1200,"black_ms":900}
        """.trimIndent()
        val snapshot = Json { ignoreUnknownKeys = true }
            .decodeFromString(GameSnapshot.serializer(), old)

        assertEquals(listOf("e2e4"), snapshot.moves)
        assertNull(snapshot.opponentLevel)
        assertNull(snapshot.engineSide)
        assertNull(GameSnapshot.decodeOpponent(snapshot.opponentLevel, snapshot.engineSide))
    }

    @Test
    fun resultsThatCannotBeReplayedRoundTrip() {
        val statuses = listOf(
            GameStatus.Resigned(Side.WHITE),
            GameStatus.Resigned(Side.BLACK),
            GameStatus.AgreedDraw,
        )
        for (s in statuses) {
            val code = GameSnapshot.encodeResult(s)
            assertEquals("encoding of $s", s, GameSnapshot.decodeResult(code))
        }
    }

    @Test
    fun replayableResultsAreNotStored() {
        // These re-emerge from the replay on their own.
        assertNull(GameSnapshot.encodeResult(GameStatus.Ongoing))
        assertNull(GameSnapshot.encodeResult(GameStatus.Checkmate(Side.WHITE)))
        assertNull(GameSnapshot.encodeResult(GameStatus.Stalemate))
        assertNull(GameSnapshot.encodeResult(GameStatus.DrawThreefold))
        assertNull(GameSnapshot.encodeResult(GameStatus.DrawFiftyMove))
        assertNull(GameSnapshot.encodeResult(GameStatus.DrawInsufficientMaterial))
    }

    @Test
    fun unknownCodeDecodesToNull() {
        assertNull(GameSnapshot.decodeResult(null))
        assertNull(GameSnapshot.decodeResult("QUATSCH"))
    }

    /**
     * Old wins on time from v1: the clock no longer ends a game, so a game saved that way
     * simply continues instead of failing to load.
     */
    @Test
    fun legacyTimeoutCodesAreDroppedGracefully() {
        assertNull(GameSnapshot.decodeResult("TIMEOUT_WHITE"))
        assertNull(GameSnapshot.decodeResult("TIMEOUT_INSUFFICIENT_BLACK"))
    }
}
