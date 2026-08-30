package name.lechners.chessomnia.data

import name.lechners.chessomnia.rules.Side

/**
 * All settings. Deliberately one flat value - it is read, written and observed by the UI
 * as a whole.
 */
data class Settings(
    /** Whether to run the thinking clock. It only counts; it never ends the game. */
    val clockEnabled: Boolean = true,
    val showHints: Boolean = true,
    val showCoordinates: Boolean = true,
    /**
     * Colour of the player at the LOWER table edge. The "swap colours" button flips it,
     * so that nobody has to turn the tablet around.
     */
    val boardBottomSide: Side = Side.WHITE,
    val confirmTakeback: Boolean = true,
    val keepScreenOn: Boolean = true,
)
