package name.lechners.chessomnia.ui.board

import androidx.compose.ui.graphics.Color

/**
 * Board colours. Deliberately independent of the app palette taken from the logo: the
 * board should look classical, not match the chrome.
 */
object BoardColors {
    val light = Color(0xFFF0D9B5)
    val dark = Color(0xFFB58863)
    val border = Color(0xFF6B4A32)

    /** Origin and target square of the last move. */
    val lastMove = Color(0x66F7D26B)

    /** The currently selected square. */
    val selection = Color(0x803193C6)

    /** A dot on a legal target square, or a ring when the move is a capture. */
    val moveHint = Color(0x7A2B303E)

    /** Castling and en passant - the special moves get a colour of their own. */
    val specialHint = Color(0xCC3193C6)

    /** Moves of an opponent's piece (learning aid), dimmed. */
    val opponentHint = Color(0x4D8B1E1E)

    val check = Color(0x99D32F2F)
    val coordinate = Color(0x996B4A32)
}
