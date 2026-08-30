package name.lechners.chessomnia.ui.game

import name.lechners.chessomnia.rules.Side

/**
 * Who is sitting at the top?
 *
 * Everything that should face a particular player - their panel, the promotion choice,
 * confirmations - has to follow this and NOT the colour. Since "swap colours" exists,
 * Black no longer necessarily sits at the top; a test for `== BLACK` would be exactly
 * upside down after a swap.
 */
fun isSeatedAtTop(side: Side, bottomSide: Side): Boolean = side != bottomSide

/** Rotation in degrees for content that should face [side]. */
fun rotationFor(side: Side, bottomSide: Side): Float =
    if (isSeatedAtTop(side, bottomSide)) 180f else 0f
