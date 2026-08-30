package name.lechners.chessomnia.ui.board

import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.rules.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * BoardGeometry is the only place where the board's orientation is decided, and
 * `squareAt` is the most error-prone function in the UI. Since "swap colours" exists,
 * every assertion has to hold in BOTH orientations - exactly the doubled test matrix that
 * the flip was previously not thought worth.
 */
class BoardGeometryTest {

    private val white = BoardGeometry(800f, Side.WHITE) // 100 px per square
    private val black = BoardGeometry(800f, Side.BLACK)

    @Test
    fun everySquareRoundTripsThroughItsCenter() {
        for (geo in listOf(white, black)) {
            for (rank in 0..7) {
                for (file in 0..7) {
                    val sq = Square.of(file, rank)
                    val hit = geo.squareAt(geo.centerXOf(sq), geo.centerYOf(sq))
                    assertEquals("square ${sq.algebraic} (bottom ${geo.bottomSide})", sq, hit)
                }
            }
        }
    }

    @Test
    fun whiteAtTheBottom() {
        assertEquals(Square.A1, white.squareAt(50f, 750f))   // unten links
        assertEquals(Square.H1, white.squareAt(750f, 750f))  // unten rechts
        assertEquals(Square.A8, white.squareAt(50f, 50f))    // oben links
        assertEquals(Square.H8, white.squareAt(750f, 50f))   // oben rechts
    }

    /** Swapped sides = a 180-degree rotation, so the files are mirrored too. */
    @Test
    fun blackAtTheBottom() {
        assertEquals(Square.H8, black.squareAt(50f, 750f))   // unten links
        assertEquals(Square.A8, black.squareAt(750f, 750f))  // unten rechts
        assertEquals(Square.H1, black.squareAt(50f, 50f))    // oben links
        assertEquals(Square.A1, black.squareAt(750f, 50f))   // oben rechts
    }

    @Test
    fun swappingSidesIsExactlyAOneEightyDegreeRotation() {
        for (rank in 0..7) {
            for (file in 0..7) {
                val sq = Square.of(file, rank)
                assertEquals("x of ${sq.algebraic}", 700f - white.originXOf(sq), black.originXOf(sq))
                assertEquals("y of ${sq.algebraic}", 700f - white.originYOf(sq), black.originYOf(sq))
            }
        }
    }

    @Test
    fun cellLookupMatchesPixelLookup() {
        for (geo in listOf(white, black)) {
            for (row in 0..7) {
                for (column in 0..7) {
                    val byCell = geo.squareAtCell(column, row)
                    val byPixel = geo.squareAt(column * 100f + 50f, row * 100f + 50f)
                    assertEquals(byCell, byPixel)
                }
            }
        }
    }

    @Test
    fun cornersOfASquareBelongToIt() {
        val sq = Square.parse("d5")
        val x = white.originXOf(sq)
        val y = white.originYOf(sq)
        assertEquals(sq, white.squareAt(x, y))
        assertEquals(sq, white.squareAt(x + 99.9f, y + 99.9f))
    }

    @Test
    fun outsideTheBoardIsNull() {
        for (geo in listOf(white, black)) {
            assertNull(geo.squareAt(-1f, 400f))
            assertNull(geo.squareAt(400f, -1f))
            assertNull(geo.squareAt(800f, 400f))
            assertNull(geo.squareAt(400f, 800f))
        }
    }
}
