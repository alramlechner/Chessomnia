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
    /**
     * Whether taking moves back is offered at all. When off, the undo button is not
     * shown during a game.
     *
     * ⚠️ This replaced a "confirm takeback" switch that never did anything: nothing
     * read it, so the confirmation appeared either way. A switch that offers or
     * withholds the feature is both honest and more useful than one that only governs
     * a prompt.
     */
    val allowTakeback: Boolean = true,
    val keepScreenOn: Boolean = true,
)
