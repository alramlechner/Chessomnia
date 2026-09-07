package name.lechners.chessomnia.data

import name.lechners.chessomnia.engine.Level
import name.lechners.chessomnia.rules.Side

/**
 * That this game is against the device, and how.
 *
 * ⚠️ It belongs to the **game**, not to the settings. A saved game has to remember that
 * it had an opponent and which colour it was playing - otherwise an app restart quietly
 * turns a game against the device into a two-player one, and the board simply stops
 * answering. `Settings` only remembers what to preselect the next time the dialog opens.
 */
data class OpponentConfig(
    val level: Level,
    /** The colour the device plays. The human has the other one. */
    val enginePlays: Side,
) {
    val humanPlays: Side get() = enginePlays.opposite
}
