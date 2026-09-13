package name.lechners.chessomnia.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.delay
import name.lechners.chessomnia.R
import name.lechners.chessomnia.rules.Piece
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.ui.board.BoardGeometry
import name.lechners.chessomnia.ui.board.MoveAnimation
import name.lechners.chessomnia.ui.board.PieceAssets
import name.lechners.chessomnia.ui.board.SLIDE_MS

/** How long the piece takes to float from its square out to the edge. */
private const val FLOAT_MS = 500

/** How long it then stays there, large enough to be looked up rather than caught. */
private const val REST_MS = 10_000L

private const val FADE_MS = 400

/** Its size once it has arrived - against 26dp in the captured row below. */
private val RESTING_SIZE = 72.dp

/**
 * The piece the device has just taken: it floats off its square out to the edge of the
 * board, grows on the way, and stays there for [REST_MS].
 *
 * It floats rather than vanishing because vanishing is precisely the problem — a piece
 * that is simply gone leaves nothing to work out what happened from. Travelling to the
 * edge answers "which piece" and "from where" in one movement, and then waits there for
 * whoever looks up a moment too late.
 *
 * ⚠️ The first stretch is a keyframe that holds still, not a `delay`: for that opening
 * [SLIDE_MS] the board itself is still drawing this piece, under the piece arriving to
 * take it. Both are tweens and both stretch together when the system's animation scale is
 * not 1 — a `delay` would not, and the piece would be handed over at the wrong moment.
 */
@Composable
fun CapturedPieceFlight(
    animation: MoveAnimation?,
    /** The board's edge length. The square grid is derived from it, as on the board. */
    boardSize: Dp,
    bottomSide: Side,
    /** Which edge it travels to - the one the device's captured row is at. */
    towardsTop: Boolean,
    /** Turned for whoever is actually sitting at this table. */
    rotated: Boolean,
) {
    var shown by remember { mutableStateOf<Piece?>(null) }
    var from by remember { mutableStateOf(Pair(0.dp, 0.dp)) }
    val progress = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    val square = boardSize / 8
    // Out past the edge, so it covers no square while it waits.
    val restY = boardSize / 2 + RESTING_SIZE / 2 + 4.dp

    LaunchedEffect(animation?.key) {
        val piece = animation?.captured
        val victimSquare = animation?.capturedSquare
        if (piece == null || victimSquare == null) {
            shown = null
            return@LaunchedEffect
        }
        // Reusing the board's own geometry, in dp rather than pixels: it is deliberately
        // free of Compose and is the one place that knows which way round the board is.
        val geo = BoardGeometry(boardSize.value, bottomSide)
        from = Pair(
            (geo.centerXOf(victimSquare) - boardSize.value / 2).dp,
            (geo.centerYOf(victimSquare) - boardSize.value / 2).dp,
        )
        shown = piece
        fade.snapTo(1f)
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = SLIDE_MS + FLOAT_MS
                0f at SLIDE_MS
                1f at SLIDE_MS + FLOAT_MS
            },
        )
        delay(REST_MS)
        fade.animateTo(0f, tween(FADE_MS))
        shown = null
    }

    val piece = shown ?: return
    val travelled = progress.value
    // Zero means the board is still drawing it on its square, under the piece taking it.
    if (travelled <= 0f) return

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(PieceAssets.drawableOf(piece)),
            contentDescription = stringResource(
                R.string.capture_spotlight, stringResource(pieceNameRes(piece.type)),
            ),
            modifier = Modifier
                .offset(
                    x = lerp(from.first, 0.dp, travelled),
                    y = lerp(from.second, if (towardsTop) -restY else restY, travelled),
                )
                .size(lerp(square, RESTING_SIZE, travelled))
                .alpha(fade.value)
                .rotate(if (rotated) 180f else 0f),
        )
    }
}
