package name.lechners.chessomnia.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastlingTest {

    private fun movesOf(fen: String) = MoveGenerator.legalMoves(Fen.parse(fen)).map { LongAlgebraic.of(it) }

    @Test
    fun bothSidesPossibleOnEmptyBoard() {
        val m = movesOf("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertTrue("kingside castling missing", "e1g1" in m)
        assertTrue("queenside castling missing", "e1c1" in m)
    }

    @Test
    fun cannotCastleOutOfCheck() {
        // The rook on e8 gives check along the e-file
        val m = movesOf("4r3/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        assertFalse("Rochade aus dem Schach", "e1g1" in m)
        assertFalse("Rochade aus dem Schach", "e1c1" in m)
    }

    @Test
    fun cannotCastleThroughAttackedSquare() {
        // The rook on f8 covers f1 - the transit square of kingside castling
        val m = movesOf("5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        assertFalse("Rochade durch bedrohtes f1", "e1g1" in m)
        assertTrue("queenside castling unaffected", "e1c1" in m)
    }

    @Test
    fun cannotCastleIntoAttackedSquare() {
        val m = movesOf("6r1/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        assertFalse("Rochade auf bedrohtes g1", "e1g1" in m)
    }

    /**
     * The second classic trap: for queenside castling b1 must be EMPTY but may be
     * attacked - the king never enters it.
     */
    @Test
    fun queensideAllowedWhenOnlyBFileIsAttacked() {
        val m = movesOf("1r6/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        assertTrue("queenside castling forbidden despite b1 only being attacked", "e1c1" in m)
    }

    @Test
    fun queensideBlockedByPieceOnB1() {
        val m = movesOf("8/8/8/8/8/8/8/RN2K2R w KQ - 0 1")
        assertFalse("lange Rochade trotz Springer auf b1", "e1c1" in m)
        assertTrue("kingside castling unaffected", "e1g1" in m)
    }

    @Test
    fun castlingWithAttackedRookIsAllowed() {
        // The black rook on a8 attacks a1 - irrelevant for castling
        val m = movesOf("r7/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        assertTrue("queenside castling with the rook attacked", "e1c1" in m)
    }

    @Test
    fun rightsLostWhenKingMoves() {
        val pos = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        pos.makeMove(LongAlgebraic.parse(pos, "e1e2")!!)
        assertTrue(pos.castlingRights and CastlingRights.WHITE_KING == 0)
        assertTrue(pos.castlingRights and CastlingRights.WHITE_QUEEN == 0)
        assertTrue("Black keeps their rights", pos.castlingRights and CastlingRights.BLACK_KING != 0)
    }

    @Test
    fun rightsLostWhenRookMoves() {
        val pos = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        pos.makeMove(LongAlgebraic.parse(pos, "a1b1")!!)
        assertTrue(pos.castlingRights and CastlingRights.WHITE_QUEEN == 0)
        assertTrue(pos.castlingRights and CastlingRights.WHITE_KING != 0)
    }

    /**
     * The single most common implementation bug: the right must also expire when the rook
     * is CAPTURED on its home square.
     */
    @Test
    fun rightsLostWhenRookIsCaptured() {
        val pos = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        pos.makeMove(LongAlgebraic.parse(pos, "a1a8")!!)
        assertTrue(
            "Black must lose their queenside castling right",
            pos.castlingRights and CastlingRights.BLACK_QUEEN == 0,
        )
        assertTrue("the kingside right remains", pos.castlingRights and CastlingRights.BLACK_KING != 0)
    }

    @Test
    fun rookIsMovedAlongWithKing() {
        val pos = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        pos.makeMove(LongAlgebraic.parse(pos, "e1g1")!!)
        assertTrue(pos.pieceAt(Square.G1) == Piece.W_KING)
        assertTrue(pos.pieceAt(Square.F1) == Piece.W_ROOK)
        assertTrue(pos.pieceAt(Square.H1) == null)

        val pos2 = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1")
        pos2.makeMove(LongAlgebraic.parse(pos2, "e8c8")!!)
        assertTrue(pos2.pieceAt(Square.C8) == Piece.B_KING)
        assertTrue(pos2.pieceAt(Square.D8) == Piece.B_ROOK)
        assertTrue(pos2.pieceAt(Square.A8) == null)
    }
}
