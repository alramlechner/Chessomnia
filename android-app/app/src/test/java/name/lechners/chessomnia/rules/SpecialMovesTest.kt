package name.lechners.chessomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialMovesTest {

    private fun movesOf(fen: String) = MoveGenerator.legalMoves(Fen.parse(fen)).map { LongAlgebraic.of(it) }

    // ── En passant ──────────────────────────────────────────────────────────────

    @Test
    fun enPassantIsGeneratedAndRemovesTheRightPawn() {
        val pos = Fen.parse("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2")
        val ep = LongAlgebraic.parse(pos, "e5d6")
        assertTrue("en passant not generated", ep != null)
        assertEquals(MoveKind.EN_PASSANT, ep!!.kind)

        pos.makeMove(ep)
        assertEquals(Piece.W_PAWN, pos.pieceAt(Square.parse("d6")))
        assertEquals("the captured pawn must disappear from d5", null, pos.pieceAt(Square.parse("d5")))
        assertEquals(null, pos.pieceAt(Square.parse("e5")))
    }

    @Test
    fun enPassantExpiresAfterOneMove() {
        val pos = Fen.parse("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2")
        pos.makeMove(LongAlgebraic.parse(pos, "e1e2")!!)      // White does not capture
        pos.makeMove(LongAlgebraic.parse(pos, "e8e7")!!)      // Schwarz zieht
        assertEquals("the ep square must be cleared", null, pos.epTarget)
        assertFalse("en passant must no longer be possible",
            MoveGenerator.legalMoves(pos).any { it.kind == MoveKind.EN_PASSANT })
    }

    /**
     * The famous exception: `exd3 e.p.` is illegal because TWO pawns leave the fourth
     * rank at once, exposing the black king to the queen on h4. Analytical pin detection
     * notoriously gets exactly this wrong; the make/unmake filter handles it without any
     * special case.
     */
    @Test
    fun enPassantIsIllegalWhenItExposesTheKingAlongTheRank() {
        val m = movesOf("8/8/8/8/1k1Pp2Q/8/8/4K3 b - d3 0 1")
        assertFalse("en passant allowed despite the rank pin", "e4d3" in m)
    }

    @Test
    fun doublePushSetsEnPassantTarget() {
        val pos = Position.startPosition()
        pos.makeMove(LongAlgebraic.parse(pos, "e2e4")!!)
        assertEquals(Square.parse("e3"), pos.epTarget)
    }

    // ── Promotion ───────────────────────────────────────────────────────────────

    @Test
    fun promotionGeneratesExactlyFourMoves() {
        val promos = MoveGenerator.legalMoves(Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"))
            .filter { it.kind == MoveKind.PROMOTION }
        assertEquals(4, promos.size)
        assertEquals(
            setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT),
            promos.mapNotNull { it.promotion }.toSet(),
        )
    }

    @Test
    fun promotionByCaptureAlsoGeneratesFour() {
        val m = movesOf("1n2k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        assertEquals(4, m.count { it.startsWith("a7b8") })
        assertEquals(4, m.count { it.startsWith("a7a8") })
    }

    @Test
    fun promotionReplacesThePawn() {
        val pos = Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        pos.makeMove(LongAlgebraic.parse(pos, "a7a8n")!!)
        assertEquals(Piece.W_KNIGHT, pos.pieceAt(Square.A8))
    }

    /**
     * Consequence for the UI: if a target square has more than one legal move, it is by
     * definition a promotion. No special case is needed there.
     */
    @Test
    fun onlyPromotionYieldsMultipleMovesToTheSameTarget() {
        val pos = Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        val byTarget = MoveGenerator.legalMoves(pos).groupBy { it.to }
        val ambiguous = byTarget.filterValues { it.size > 1 }
        assertEquals(setOf(Square.A8), ambiguous.keys)
    }

    // ── Pins ────────────────────────────────────────────────────────────────────

    @Test
    fun pinnedPieceMayOnlyMoveAlongThePin() {
        // The white rook on e2 is pinned to the king on e1 by the black rook on e8
        val m = movesOf("4r3/8/8/8/8/8/4R3/4K3 w - - 0 1")
        assertTrue("move along the pin missing", "e2e3" in m)
        assertTrue("capturing the pinner missing", "e2e8" in m)
        assertFalse("a pinned rook must not move sideways", "e2d2" in m)
        assertFalse("a pinned rook must not move sideways", "e2f2" in m)
    }

    @Test
    fun kingMayNotRetreatAlongTheCheckingRay() {
        // The black queen on e8 gives check; the e-file stays covered, also behind the king
        val m = movesOf("4q3/8/8/8/8/8/8/4K3 w - - 0 1")
        assertFalse("the king retreats along the checking line", "e1e2" in m)
        assertTrue("stepping aside must be possible", "e1d1" in m || "e1f1" in m)
    }
}
