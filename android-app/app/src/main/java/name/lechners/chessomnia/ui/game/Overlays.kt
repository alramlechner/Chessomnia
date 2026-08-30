package name.lechners.chessomnia.ui.game

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.R
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Move
import name.lechners.chessomnia.rules.Piece
import name.lechners.chessomnia.rules.PieceType
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.ui.board.PieceAssets

/**
 * A semi-transparent scrim. Even with `dismissOnTap = false` it swallows taps on the
 * board - otherwise moves could still be played while a dialog is open.
 */
@Composable
private fun Scrim(onTap: (() -> Unit)?, content: @Composable BoxScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(interaction, indication = null) { onTap?.invoke() },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * The promotion choice, turned to face whoever is moving. In a fixed orientation it
 * would be upside down for one of them - the tablet lies between the two.
 */
@Composable
fun PromotionOverlay(
    side: Side,
    bottomSide: Side,
    options: List<Move>,
    onChoose: (PieceType) -> Unit,
    onCancel: () -> Unit,
) {
    Scrim(onTap = onCancel) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            // Facing the player who is promoting - they do not necessarily sit at the
            // bottom.
            modifier = Modifier.rotate(rotationFor(side, bottomSide)),
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.promotion_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (type in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                        if (options.none { it.promotion == type }) continue
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onChoose(type) },
                        ) {
                            Image(
                                painter = painterResource(PieceAssets.drawableOf(Piece.of(side, type))),
                                contentDescription = stringResource(pieceNameRes(type)),
                                modifier = Modifier.size(78.dp).padding(6.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
            }
        }
    }
}

@Composable
fun ConfirmOverlay(
    action: ConfirmAction,
    bottomSide: Side,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, text) = when (action) {
        is ConfirmAction.Takeback ->
            stringResource(R.string.confirm_takeback_title) to
                stringResource(R.string.confirm_takeback_text)
        is ConfirmAction.Resign ->
            stringResource(R.string.confirm_resign_title) to
                stringResource(R.string.confirm_resign_text, stringResource(sideNameRes(action.initiator)))
        is ConfirmAction.Draw ->
            stringResource(R.string.confirm_draw_title) to
                stringResource(R.string.confirm_draw_text)
    }
    // Facing whoever asked - every action knows which panel it came from.
    val rotate = isSeatedAtTop(action.initiator, bottomSide)

    Scrim(onTap = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.rotate(if (rotate) 180f else 0f),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                    Button(onClick = onConfirm) { Text(stringResource(R.string.common_yes)) }
                }
            }
        }
    }
}

@StringRes
private fun pieceNameRes(type: PieceType): Int = when (type) {
    PieceType.QUEEN -> R.string.piece_queen
    PieceType.ROOK -> R.string.piece_rook
    PieceType.BISHOP -> R.string.piece_bishop
    PieceType.KNIGHT -> R.string.piece_knight
    PieceType.PAWN -> R.string.piece_pawn
    PieceType.KING -> R.string.piece_king
}
