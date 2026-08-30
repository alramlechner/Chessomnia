package name.lechners.chessomnia.rules

/**
 * White or Black.
 *
 * Deliberately called `Side` and not `Color`: otherwise
 * `androidx.compose.ui.graphics.Color` would clash in every UI file.
 */
enum class Side {
    WHITE, BLACK;

    val opposite: Side get() = if (this == WHITE) BLACK else WHITE

    /** Direction of a pawn move on the 0x88 board. */
    val pawnPush: Int get() = if (this == WHITE) 16 else -16

    /** Rank (zero-based) on which this side's pawns start. */
    val pawnStartRank: Int get() = if (this == WHITE) 1 else 6

    /** Rank (zero-based) on which this side's pawns promote. */
    val promotionRank: Int get() = if (this == WHITE) 7 else 0
}
