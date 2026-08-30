package name.lechners.chessomnia.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.R
import androidx.compose.ui.unit.sp

/**
 * A small fifty-move-rule counter: a ring that fills up, plus the number of moves still
 * remaining.
 *
 * It only appears from [SHOW_FROM_HALFMOVES] halfmoves without a pawn move or capture.
 * Before that it would be pure noise: in the opening almost every move resets the
 * counter, the display would keep jumping back to 50, and nobody would know what it
 * stood for.
 */
@Composable
fun FiftyMoveIndicator(
    halfmoveClock: Int,
    rotated: Boolean,
    modifier: Modifier = Modifier,
) {
    if (halfmoveClock < SHOW_FROM_HALFMOVES) return

    val remaining = remainingMoves(halfmoveClock)
    val progress = (halfmoveClock.toFloat() / LIMIT_HALFMOVES).coerceIn(0f, 1f)
    val critical = remaining <= 10

    // Fetched outside the semantics block, which is not a composable context.
    val description = pluralStringResource(R.plurals.fifty_move_remaining, remaining, remaining)

    val accent = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Row(
        modifier = modifier
            .rotate(if (rotated) 180f else 0f)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Canvas(Modifier.size(13.dp)) {
            val stroke = Stroke(width = size.minDimension * 0.18f, cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(
                color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize, style = stroke,
            )
            drawArc(
                color = accent, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize, style = stroke,
            )
        }
        Text(
            text = "$remaining",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (critical) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Halfmoves without a pawn move or capture at which the draw takes effect. */
const val LIMIT_HALFMOVES = 100

/** Before this the display is only noise - see the class comment. */
const val SHOW_FROM_HALFMOVES = 20

/**
 * Full moves until the draw. Rounded up, because a move already begun still counts: at
 * 99 halfmoves exactly one is left, not none.
 */
internal fun remainingMoves(halfmoveClock: Int): Int {
    val left = (LIMIT_HALFMOVES - halfmoveClock).coerceAtLeast(0)
    return (left + 1) / 2
}
