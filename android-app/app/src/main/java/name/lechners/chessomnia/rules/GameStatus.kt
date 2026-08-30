package name.lechners.chessomnia.rules

/**
 * How the game ended. "Check" is deliberately NOT a status but a separate field in the
 * UI state, which keeps `status == Ongoing` a simple comparison.
 */
sealed interface GameStatus {
    data object Ongoing : GameStatus
    data class Checkmate(val winner: Side) : GameStatus
    data object Stalemate : GameStatus
    data object DrawFiftyMove : GameStatus
    data object DrawThreefold : GameStatus
    data object DrawInsufficientMaterial : GameStatus

    // There is deliberately NO time-based outcome: the clock only measures thinking
    // time and never ends the game.

    data class Resigned(val winner: Side) : GameStatus
    data object AgreedDraw : GameStatus

    val isOver: Boolean get() = this != Ongoing
}
