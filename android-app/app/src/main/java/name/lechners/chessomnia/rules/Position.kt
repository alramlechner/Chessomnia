package name.lechners.chessomnia.rules

/**
 * What `makeMove` returns so that `unmakeMove` can restore the position exactly.
 * `sideToMove` and `fullmoveNumber` follow from the move itself and are therefore not
 * stored here.
 */
class UndoInfo(
    val captured: Piece?,
    /** For en passant this is NOT `move.to`. */
    val capturedSquare: Int,
    val prevCastlingRights: Int,
    val prevEpTarget: Square?,
    val prevHalfmoveClock: Int,
)

/**
 * The complete game state. Mutable: moves are applied and undone with
 * `makeMove`/`unmakeMove`, not by copying.
 *
 * The same mechanism carries three things: the legality filter of move generation, the
 * user's takeback, and the perft tests. It therefore cannot drift apart between those
 * three uses.
 */
class Position(
    val board: Array<Piece?>,
    var sideToMove: Side,
    var castlingRights: Int,
    var epTarget: Square?,
    var halfmoveClock: Int,
    var fullmoveNumber: Int,
) {
    /** King square per side, tracked rather than searched for. */
    private val kingSquares = IntArray(2) { -1 }

    init { refreshKingSquares() }

    fun refreshKingSquares() {
        kingSquares[0] = -1
        kingSquares[1] = -1
        for (i in 0 until 128) {
            if (!Square.isOnBoard(i)) continue
            val p = board[i] ?: continue
            if (p.type == PieceType.KING) kingSquares[p.side.ordinal] = i
        }
    }

    fun kingSquare(side: Side): Square = Square(kingSquares[side.ordinal])

    fun hasKing(side: Side): Boolean = kingSquares[side.ordinal] >= 0

    fun pieceAt(sq: Square): Piece? = board[sq.index]

    fun copy(): Position = Position(
        board.copyOf(), sideToMove, castlingRights, epTarget, halfmoveClock, fullmoveNumber
    )

    // ── Applying a move ─────────────────────────────────────────────────────────

    fun makeMove(m: Move): UndoInfo {
        val us = sideToMove
        val from = m.from.index
        val to = m.to.index
        val moving = board[from] ?: error("Kein Stein auf ${m.from}")

        // Determine the victim - for en passant it stands behind the target square
        val capturedSquare =
            if (m.kind == MoveKind.EN_PASSANT) to - us.pawnPush else to
        val captured = board[capturedSquare]

        val undo = UndoInfo(captured, capturedSquare, castlingRights, epTarget, halfmoveClock)

        if (captured != null) board[capturedSquare] = null

        board[from] = null
        board[to] = if (m.promotion != null) Piece.of(us, m.promotion) else moving

        // Move the castling rook along
        when (m.kind) {
            MoveKind.CASTLE_KINGSIDE -> { board[to - 1] = board[to + 1]; board[to + 1] = null }
            MoveKind.CASTLE_QUEENSIDE -> { board[to + 1] = board[to - 2]; board[to - 2] = null }
            else -> Unit
        }

        if (moving.type == PieceType.KING) kingSquares[us.ordinal] = to

        // Castling rights: `from` covers "king or rook has moved", `to` covers "a rook
        // was captured on its home square". That second clause is the single most
        // common implementation bug in chess programming.
        castlingRights = castlingRights and CASTLING_MASK[from] and CASTLING_MASK[to]

        epTarget =
            if (m.kind == MoveKind.DOUBLE_PAWN_PUSH) Square((from + to) / 2) else null

        halfmoveClock =
            if (moving.type == PieceType.PAWN || captured != null) 0 else halfmoveClock + 1

        if (us == Side.BLACK) fullmoveNumber++
        sideToMove = us.opposite

        return undo
    }

    fun unmakeMove(m: Move, undo: UndoInfo) {
        val us = sideToMove.opposite // die Seite, die den Zug gemacht hat
        val from = m.from.index
        val to = m.to.index

        sideToMove = us
        if (us == Side.BLACK) fullmoveNumber--
        castlingRights = undo.prevCastlingRights
        epTarget = undo.prevEpTarget
        halfmoveClock = undo.prevHalfmoveClock

        when (m.kind) {
            MoveKind.CASTLE_KINGSIDE -> { board[to + 1] = board[to - 1]; board[to - 1] = null }
            MoveKind.CASTLE_QUEENSIDE -> { board[to - 2] = board[to + 1]; board[to + 1] = null }
            else -> Unit
        }

        val moved = board[to]
        board[from] = if (m.promotion != null) Piece.of(us, PieceType.PAWN) else moved
        board[to] = null

        if (undo.captured != null) board[undo.capturedSquare] = undo.captured

        if (moved != null && moved.type == PieceType.KING) kingSquares[us.ordinal] = from
    }

    companion object {
        /**
         * Per-square mask: which castling rights survive when this square is the origin
         * or the target of a move.
         */
        private val CASTLING_MASK = IntArray(128) { CastlingRights.ALL }.apply {
            this[Square.E1.index] = CastlingRights.ALL and
                (CastlingRights.WHITE_KING or CastlingRights.WHITE_QUEEN).inv()
            this[Square.A1.index] = CastlingRights.ALL and CastlingRights.WHITE_QUEEN.inv()
            this[Square.H1.index] = CastlingRights.ALL and CastlingRights.WHITE_KING.inv()
            this[Square.E8.index] = CastlingRights.ALL and
                (CastlingRights.BLACK_KING or CastlingRights.BLACK_QUEEN).inv()
            this[Square.A8.index] = CastlingRights.ALL and CastlingRights.BLACK_QUEEN.inv()
            this[Square.H8.index] = CastlingRights.ALL and CastlingRights.BLACK_KING.inv()
        }

        fun startPosition(): Position = Fen.parse(Fen.START)

        fun empty(): Position = Position(
            arrayOfNulls(128), Side.WHITE, CastlingRights.NONE, null, 0, 1
        )
    }
}
