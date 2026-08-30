package name.lechners.chessomnia.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.rules.Piece
import name.lechners.chessomnia.rules.PieceType
import name.lechners.chessomnia.ui.board.PieceAssets

/**
 * The pieces this player has taken from the opponent - as one would place them beside
 * oneself at a real board. If the player sits at the top, the row is rotated with them
 * so the pieces stand the right way up for them.
 */
@Composable
fun CapturedRow(
    captured: List<Piece>,
    /** NET advantage in pawn units; only positive values are shown. */
    advantage: Int,
    halfmoveClock: Int,
    rotated: Boolean,
    modifier: Modifier = Modifier,
) {
    // A fixed height even when empty: otherwise the board would jump by a few pixels
    // on the first capture.
    Row(
        modifier = modifier.fillMaxWidth().height(26.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-4).dp),
    ) {
        for (piece in captured) {
            Image(
                painter = painterResource(PieceAssets.drawableOf(piece)),
                contentDescription = null,
                modifier = Modifier.size(24.dp).rotate(if (rotated) 180f else 0f),
            )
        }
        if (advantage > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                "+$advantage",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (rotated) 180f else 0f),
            )
        }

        // The fifty-move counter sits at the outer edge of this same row, so it costs
        // no additional height beside the board.
        Spacer(Modifier.weight(1f))
        FiftyMoveIndicator(halfmoveClock, rotated)
    }
}

/**
 * The value of the captured pieces in pawn units.
 *
 * The caller forms the DIFFERENCE between the two sides from this. A plain sum would
 * mislead: having traded queen for queen would otherwise read "+9".
 */
internal fun materialValue(captured: List<Piece>): Int =
    captured.fold(0) { sum, piece -> sum + valueOf(piece.type) }

private fun valueOf(type: PieceType): Int = when (type) {
    PieceType.PAWN -> 1
    PieceType.KNIGHT, PieceType.BISHOP -> 3
    PieceType.ROOK -> 5
    PieceType.QUEEN -> 9
    PieceType.KING -> 0
}
