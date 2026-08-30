package name.lechners.chessomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FenTest {

    @Test
    fun roundTrip() {
        val fens = PerftPositions.ALL.map { it.fen } + listOf(
            "8/8/8/8/8/8/8/K6k w - - 99 150",
            "4k3/8/8/8/8/8/8/4K3 b - - 0 1",
        )
        for (fen in fens) assertEquals(fen, Fen.serialize(Fen.parse(fen)))
    }

    @Test
    fun parsesStartPosition() {
        val pos = Position.startPosition()
        assertEquals(Piece.W_ROOK, pos.pieceAt(Square.A1))
        assertEquals(Piece.W_KING, pos.pieceAt(Square.E1))
        assertEquals(Piece.B_KING, pos.pieceAt(Square.E8))
        assertEquals(Side.WHITE, pos.sideToMove)
        assertEquals(CastlingRights.ALL, pos.castlingRights)
        assertEquals(null, pos.epTarget)
    }

    @Test
    fun rejectsMalformedInput() {
        for (bad in listOf(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1",   // Reihe fehlt
            "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1", // Reihe zu lang
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1", // Zugfarbe
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNX w KQkq - 0 1", // bad piece letter
        )) {
            assertThrows(bad, IllegalArgumentException::class.java) { Fen.parse(bad) }
        }
    }

    @Test
    fun squareMapping() {
        assertEquals("a1", Square.A1.algebraic)
        assertEquals("h8", Square.H8.algebraic)
        assertEquals("e4", Square.parse("e4").algebraic)
        assertEquals(0, Square.A1.toIndex64())
        assertEquals(63, Square.H8.toIndex64())
        for (i in 0..63) assertEquals(i, Square.fromIndex64(i).toIndex64())
        // a1 is a dark square, h1 a light one
        assertEquals(false, Square.A1.isLightSquare)
        assertEquals(true, Square.H1.isLightSquare)
    }
}
