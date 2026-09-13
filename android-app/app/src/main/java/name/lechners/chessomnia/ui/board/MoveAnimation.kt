package name.lechners.chessomnia.ui.board

import name.lechners.chessomnia.rules.Move
import name.lechners.chessomnia.rules.Piece
import name.lechners.chessomnia.rules.Square

/**
 * A move the board should show arriving instead of simply appearing.
 *
 * Only the device's moves get one. A move made by a hand at this table was watched being
 * made, and sliding it would delay the answer to one's own tap; a move made by the device
 * is otherwise a board that has silently changed between two glances.
 *
 * [key] counts moves rather than flagging them. That is what makes "a new move landed"
 * distinguishable from "the same state was published again" — the latter is what a
 * recomposition does, and without the counter the board would replay the last move every
 * time it is recomposed.
 */
data class MoveAnimation(
    val key: Int,
    val move: Move,
    /** The piece this move took. It stays on the board until the mover has arrived. */
    val captured: Piece? = null,
    /** Where that piece stood — not [Move.to] for en passant. */
    val capturedSquare: Square? = null,
)
