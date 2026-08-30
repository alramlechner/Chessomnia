package name.lechners.chessomnia.data

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
