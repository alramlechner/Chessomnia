package name.lechners.chessomnia.rules

enum class MoveKind {
    QUIET,
    CAPTURE,
    DOUBLE_PAWN_PUSH,
    EN_PASSANT,
    CASTLE_KINGSIDE,
    CASTLE_QUEENSIDE,

    /**
     * Pawn promotion, with or without a capture. Whether a piece is taken follows from
     * the target square; a separate kind for it would merely double the enum.
     */
    PROMOTION,
}

/**
 * A move. Deliberately a `data class` and not a packed Int: `equals`/`hashCode` come for
 * free (the UI looks moves up by target square in a map), and the speed advantage of bit
 * packing is never needed here.
 */
data class Move(
    val from: Square,
    val to: Square,
    val kind: MoveKind,
    val promotion: PieceType? = null,
) {
    val isCastle: Boolean
        get() = kind == MoveKind.CASTLE_KINGSIDE || kind == MoveKind.CASTLE_QUEENSIDE

    override fun toString(): String =
        from.algebraic + to.algebraic + (promotion?.fenChar ?: "")
}
