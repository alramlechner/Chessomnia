package name.lechners.chessomnia.ui.board

import androidx.annotation.DrawableRes
import name.lechners.chessomnia.R
import name.lechners.chessomnia.rules.Piece

/**
 * The only place that refers to R.drawable.piece_*. The `when` is exhaustive, so an
 * additional piece set would be a compile error rather than an empty square at runtime.
 */
object PieceAssets {
    @DrawableRes
    fun drawableOf(piece: Piece): Int = when (piece) {
        Piece.W_KING -> R.drawable.piece_w_king
        Piece.W_QUEEN -> R.drawable.piece_w_queen
        Piece.W_ROOK -> R.drawable.piece_w_rook
        Piece.W_BISHOP -> R.drawable.piece_w_bishop
        Piece.W_KNIGHT -> R.drawable.piece_w_knight
        Piece.W_PAWN -> R.drawable.piece_w_pawn
        Piece.B_KING -> R.drawable.piece_b_king
        Piece.B_QUEEN -> R.drawable.piece_b_queen
        Piece.B_ROOK -> R.drawable.piece_b_rook
        Piece.B_BISHOP -> R.drawable.piece_b_bishop
        Piece.B_KNIGHT -> R.drawable.piece_b_knight
        Piece.B_PAWN -> R.drawable.piece_b_pawn
    }
}
