package name.lechners.chessomnia.game

import name.lechners.chessomnia.game.clock.ClockState
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Move
import name.lechners.chessomnia.rules.UndoInfo

/**
 * One entry of the move history - everything a takeback needs.
 *
 * The clock reading from BEFORE the move belongs here and not in a structure of its own:
 * only that way can move and thinking time not drift apart.
 */
data class MoveRecord(
    val move: Move,
    val undo: UndoInfo,
    /** Repetition key of the position AFTER the move. */
    val repetitionKey: String,
    val statusBefore: GameStatus,
    val clockBefore: ClockState,
)
