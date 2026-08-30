package name.lechners.chessomnia.rules

object MoveGenerator {

    private val PROMOTION_PIECES = arrayOf(
        PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    )

    /**
     * Every legal move for the side to move.
     *
     * The filter works by trying rather than analysing: make the move, test whether our
     * own king is attacked, unmake it. That handles the following without any special
     * case at all:
     *   - pinned pieces,
     *   - a king retreating along the checking line and thereby still shielding itself,
     *   - the en-passant pin along a rank (two pawns leave the rank at once and expose
     *     their own king) - precisely the case analytical pin detection notoriously
     *     gets wrong.
     */
    fun legalMoves(pos: Position): List<Move> {
        val us = pos.sideToMove
        val result = ArrayList<Move>(48)
        for (m in pseudoLegalMoves(pos)) {
            val undo = pos.makeMove(m)
            val legal = !Attacks.isInCheck(pos, us)
            pos.unmakeMove(m, undo)
            if (legal) result.add(m)
        }
        return result
    }

    /** Legal moves grouped by origin square, so a UI tap costs one hash lookup. */
    fun legalMovesByOrigin(pos: Position): Map<Square, List<Move>> =
        legalMoves(pos).groupBy { it.from }

    fun pseudoLegalMoves(pos: Position): List<Move> {
        val moves = ArrayList<Move>(48)
        val us = pos.sideToMove
        for (i in 0 until 128) {
            if (!Square.isOnBoard(i)) continue
            val piece = pos.board[i] ?: continue
            if (piece.side != us) continue
            when (piece.type) {
                PieceType.PAWN -> pawnMoves(pos, i, us, moves)
                PieceType.KNIGHT -> stepMoves(pos, i, us, Attacks.KNIGHT, moves)
                PieceType.BISHOP -> slideMoves(pos, i, us, Attacks.BISHOP, moves)
                PieceType.ROOK -> slideMoves(pos, i, us, Attacks.ROOK, moves)
                PieceType.QUEEN -> {
                    slideMoves(pos, i, us, Attacks.BISHOP, moves)
                    slideMoves(pos, i, us, Attacks.ROOK, moves)
                }
                PieceType.KING -> {
                    stepMoves(pos, i, us, Attacks.KING, moves)
                    castlingMoves(pos, us, moves)
                }
            }
        }
        return moves
    }

    // ── Piece types ─────────────────────────────────────────────────────────────

    private fun stepMoves(pos: Position, from: Int, us: Side, offsets: IntArray, out: MutableList<Move>) {
        for (o in offsets) {
            val to = from + o
            if (!Square.isOnBoard(to)) continue
            val target = pos.board[to]
            if (target == null) {
                out.add(Move(Square(from), Square(to), MoveKind.QUIET))
            } else if (target.side != us) {
                out.add(Move(Square(from), Square(to), MoveKind.CAPTURE))
            }
        }
    }

    private fun slideMoves(pos: Position, from: Int, us: Side, offsets: IntArray, out: MutableList<Move>) {
        for (o in offsets) {
            var to = from + o
            while (Square.isOnBoard(to)) {
                val target = pos.board[to]
                if (target == null) {
                    out.add(Move(Square(from), Square(to), MoveKind.QUIET))
                } else {
                    if (target.side != us) out.add(Move(Square(from), Square(to), MoveKind.CAPTURE))
                    break
                }
                to += o
            }
        }
    }

    private fun pawnMoves(pos: Position, from: Int, us: Side, out: MutableList<Move>) {
        val push = us.pawnPush
        val fromSq = Square(from)

        // Single step (possibly promoting), then the double step
        val one = from + push
        if (Square.isOnBoard(one) && pos.board[one] == null) {
            addPawnAdvance(fromSq, Square(one), us, out)
            if (fromSq.rank == us.pawnStartRank) {
                val two = one + push
                if (Square.isOnBoard(two) && pos.board[two] == null) {
                    out.add(Move(fromSq, Square(two), MoveKind.DOUBLE_PAWN_PUSH))
                }
            }
        }

        // Captures and en passant
        for (d in intArrayOf(-1, 1)) {
            val to = from + push + d
            if (!Square.isOnBoard(to)) continue
            val target = pos.board[to]
            if (target != null) {
                if (target.side != us) addPawnAdvance(fromSq, Square(to), us, out)
            } else if (pos.epTarget?.index == to) {
                out.add(Move(fromSq, Square(to), MoveKind.EN_PASSANT))
            }
        }
    }

    /**
     * A pawn move to `to`: on the promotion rank this yields FOUR moves that differ only
     * in their `promotion` field. Pleasant consequence for the UI: if the move list for
     * a target square holds more than one entry, it is by definition a promotion - no
     * special case needed there.
     */
    private fun addPawnAdvance(from: Square, to: Square, us: Side, out: MutableList<Move>) {
        if (to.rank == us.promotionRank) {
            for (p in PROMOTION_PIECES) out.add(Move(from, to, MoveKind.PROMOTION, p))
        } else {
            out.add(Move(from, to, MoveKind.QUIET))
        }
    }

    // ── Castling ────────────────────────────────────────────────────────────────

    /**
     * Checked in full here rather than in the legality filter: condition (4) concerns
     * squares that are neither `from` nor `to`, so the generic filter would miss them.
     */
    private fun castlingMoves(pos: Position, us: Side, out: MutableList<Move>) {
        val them = us.opposite
        val e = if (us == Side.WHITE) Square.E1 else Square.E8
        if (pos.board[e.index]?.let { it.side == us && it.type == PieceType.KING } != true) return

        val kingRight = if (us == Side.WHITE) CastlingRights.WHITE_KING else CastlingRights.BLACK_KING
        val queenRight = if (us == Side.WHITE) CastlingRights.WHITE_QUEEN else CastlingRights.BLACK_QUEEN
        val base = e.index

        // Kingside: f and g empty, e/f/g not attacked
        if (pos.castlingRights and kingRight != 0 && isOwnRook(pos, base + 3, us)) {
            if (pos.board[base + 1] == null && pos.board[base + 2] == null &&
                !Attacks.isSquareAttacked(pos, base, them) &&
                !Attacks.isSquareAttacked(pos, base + 1, them) &&
                !Attacks.isSquareAttacked(pos, base + 2, them)
            ) {
                out.add(Move(e, Square(base + 2), MoveKind.CASTLE_KINGSIDE))
            }
        }

        // Queenside: d, c AND b empty - but b may be attacked, the king never enters
        // it. Forgetting b1/b8 is the second classic trap.
        if (pos.castlingRights and queenRight != 0 && isOwnRook(pos, base - 4, us)) {
            if (pos.board[base - 1] == null && pos.board[base - 2] == null && pos.board[base - 3] == null &&
                !Attacks.isSquareAttacked(pos, base, them) &&
                !Attacks.isSquareAttacked(pos, base - 1, them) &&
                !Attacks.isSquareAttacked(pos, base - 2, them)
            ) {
                out.add(Move(e, Square(base - 2), MoveKind.CASTLE_QUEENSIDE))
            }
        }
    }

    private fun isOwnRook(pos: Position, index: Int, us: Side): Boolean {
        val p = pos.board[index] ?: return false
        return p.side == us && p.type == PieceType.ROOK
    }
}
