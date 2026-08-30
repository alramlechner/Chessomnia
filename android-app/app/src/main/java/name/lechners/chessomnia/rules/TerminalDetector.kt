package name.lechners.chessomnia.rules

/** Detects whether and how the game has ended. */
object TerminalDetector {

    /**
     * Evaluated once per move, for the NEW side to move. The legal move list is passed
     * in rather than recomputed: it is needed for the move markers anyway, and a second
     * generation would be pure waste.
     */
    fun evaluate(
        pos: Position,
        legalMoves: List<Move>,
        repetition: RepetitionTracker,
    ): GameStatus {
        if (legalMoves.isEmpty()) {
            return if (Attacks.isInCheck(pos, pos.sideToMove)) {
                GameStatus.Checkmate(pos.sideToMove.opposite)
            } else {
                GameStatus.Stalemate
            }
        }
        if (isInsufficientMaterial(pos)) return GameStatus.DrawInsufficientMaterial
        if (repetition.currentCount() >= 3) return GameStatus.DrawThreefold
        if (pos.halfmoveClock >= 100) return GameStatus.DrawFiftyMove
        return GameStatus.Ongoing
    }

    /**
     * Dead material: K-K, K+B-K, K+N-K and K+B-K+B with same-coloured bishops.
     *
     * K+N+N-K is deliberately NOT included: mate is possible there, merely not
     * forcible, so FIDE does not treat the position as dead.
     */
    fun isInsufficientMaterial(pos: Position): Boolean {
        var minorCount = 0
        var lightSquareBishops = 0
        var darkSquareBishops = 0
        var bishops = 0
        var knights = 0

        for (i in 0 until 128) {
            if (!Square.isOnBoard(i)) continue
            val p = pos.board[i] ?: continue
            when (p.type) {
                PieceType.KING -> Unit
                PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                PieceType.BISHOP -> {
                    bishops++; minorCount++
                    if (Square(i).isLightSquare) lightSquareBishops++ else darkSquareBishops++
                }
                PieceType.KNIGHT -> { knights++; minorCount++ }
            }
        }

        return when {
            minorCount == 0 -> true                                   // K gegen K
            minorCount == 1 -> true                                   // K+L oder K+S gegen K
            minorCount == 2 && knights == 0 && bishops == 2 ->
                lightSquareBishops == 2 || darkSquareBishops == 2     // Laeufer gleicher Feldfarbe
            else -> false
        }
    }
}
