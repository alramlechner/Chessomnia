package name.lechners.chessomnia.ui.game

import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Since "swap colours" exists, Black no longer necessarily sits at the top. A test for
 * `== BLACK` therefore turned the promotion choice upside down for BOTH players after a
 * swap - which is why everything that should face a player follows the seat, not the
 * colour.
 */
class SeatOrientationTest {

    @Test
    fun normalSeating() {
        assertFalse("Weiss sitzt unten", isSeatedAtTop(Side.WHITE, bottomSide = Side.WHITE))
        assertTrue("Schwarz sitzt oben", isSeatedAtTop(Side.BLACK, bottomSide = Side.WHITE))
        assertEquals(0f, rotationFor(Side.WHITE, Side.WHITE))
        assertEquals(180f, rotationFor(Side.BLACK, Side.WHITE))
    }

    @Test
    fun afterSwappingColours() {
        assertTrue("Weiss sitzt jetzt oben", isSeatedAtTop(Side.WHITE, bottomSide = Side.BLACK))
        assertFalse("Schwarz sitzt jetzt unten", isSeatedAtTop(Side.BLACK, bottomSide = Side.BLACK))
        assertEquals(180f, rotationFor(Side.WHITE, Side.BLACK))
        assertEquals(0f, rotationFor(Side.BLACK, Side.BLACK))
    }

    @Test
    fun everyPlayerSeesTheirOwnDialogUpright() {
        for (bottom in Side.entries) {
            for (side in Side.entries) {
                val rotated = rotationFor(side, bottom) == 180f
                assertEquals("side=$side bottom=$bottom", side != bottom, rotated)
            }
        }
    }
}
