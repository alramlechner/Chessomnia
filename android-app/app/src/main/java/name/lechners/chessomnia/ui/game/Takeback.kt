package name.lechners.chessomnia.ui.game

import name.lechners.chessomnia.game.ChessGame
import name.lechners.chessomnia.rules.Side

/**
 * Takes the last move back - and against the device, as many as it takes to hand the
 * board back to the person.
 *
 * Its own function rather than a few lines in the ViewModel so that it can be checked
 * without Android: the rule has three distinct outcomes depending on whose turn it was,
 * and one of them ends with the device to move again.
 *
 * @param human the colour the person plays, or null in a game between two people - then
 *   exactly one halfmove comes off, as it always did.
 */
internal fun takeBackToHumanTurn(game: ChessGame, human: Side?) {
    game.takeback()
    if (human == null) return
    // Undoing only the device's reply would hand the board straight back to it, and it
    // would answer again - the move would appear not to have been taken back at all.
    while (game.canTakeback && game.sideToMove != human) game.takeback()
}
