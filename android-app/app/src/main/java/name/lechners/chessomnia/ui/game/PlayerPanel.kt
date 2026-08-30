package name.lechners.chessomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import name.lechners.chessomnia.R
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.ui.theme.LogoBlue

/**
 * One player's panel - it exists twice, at both table edges. The caller rotates the
 * upper one by 180 degrees.
 *
 * When the game ends it also shows the result and the reason. Deliberately here and not
 * in a dialog over the board: after a mate you want to see WHY - the highlighted king
 * and the marked last move are the answer, and nothing may cover them.
 */
@Composable
fun PlayerPanel(
    side: Side,
    state: GameUiState,
    clockFlow: StateFlow<ClockUiState>,
    clockEnabled: Boolean,
    clockRunning: Boolean,
    onToggleClock: () -> Unit,
    onTakeback: () -> Unit,
    onResign: () -> Unit,
    onDraw: () -> Unit,
    onSwapSides: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val over = state.status.isOver
    val isToMove = !over && state.sideToMove == side
    val outcome = outcomeFor(state.status, side)

    val background = when {
        outcome == Outcome.WON -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        outcome == Outcome.LOST -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        over -> MaterialTheme.colorScheme.surfaceVariant
        isToMove -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClockView(side, clockFlow, Modifier.padding(end = 12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                if (over) stringResource(headlineFor(outcome))
                else stringResource(sideNameRes(side)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    outcome == Outcome.WON -> LogoBlue
                    outcome == Outcome.LOST -> MaterialTheme.colorScheme.error
                    isToMove -> LogoBlue
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                if (over) reasonFor(state.status).resolve() else stringResource(runningStatusLine(state, side)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (over) {
            // Once the game is over only the ways back remain - which is also how an
            // accidental resignation gets corrected.
            if (state.canTakeback) TextButton(onClick = onTakeback) { Text(stringResource(R.string.action_takeback)) }
            TextButton(onClick = onExit) { Text(stringResource(R.string.action_menu)) }
        } else {
            if (clockEnabled) {
                TextButton(onClick = onToggleClock) {
                    Text(stringResource(if (clockRunning) R.string.action_pause else R.string.action_start_clock))
                }
            }
            if (state.canTakeback) TextButton(onClick = onTakeback) { Text(stringResource(R.string.action_takeback)) }
            TextButton(onClick = onDraw) { Text(stringResource(R.string.action_draw)) }
            TextButton(onClick = onResign) { Text(stringResource(R.string.action_resign)) }
            TextButton(onClick = onSwapSides) { Text(stringResource(R.string.action_swap_colours)) }
            TextButton(onClick = onExit) { Text(stringResource(R.string.action_menu)) }
        }
    }
}

internal enum class Outcome { WON, LOST, DRAW, RUNNING }

internal fun outcomeFor(status: GameStatus, side: Side): Outcome = when (status) {
    is GameStatus.Ongoing -> Outcome.RUNNING
    is GameStatus.Checkmate -> if (status.winner == side) Outcome.WON else Outcome.LOST
    is GameStatus.Resigned -> if (status.winner == side) Outcome.WON else Outcome.LOST
    else -> Outcome.DRAW
}

/**
 * A translatable text: a resource id plus, where needed, a single argument that is itself
 * a resource ("Checkmate - White wins").
 *
 * Why not simply return a String: the functions below are pure, free of any Android
 * dependency, and therefore checkable as JVM unit tests. A finished string would need a
 * Context and would no longer be translatable.
 */
internal data class ResText(@StringRes val id: Int, @StringRes val arg: Int? = null) {
    @Composable
    fun resolve(): String =
        if (arg == null) stringResource(id) else stringResource(id, stringResource(arg))
}

@StringRes
internal fun sideNameRes(side: Side): Int =
    if (side == Side.WHITE) R.string.side_white else R.string.side_black

@StringRes
private fun headlineFor(outcome: Outcome): Int = when (outcome) {
    Outcome.WON -> R.string.outcome_won
    Outcome.LOST -> R.string.outcome_lost
    Outcome.DRAW -> R.string.outcome_draw
    Outcome.RUNNING -> R.string.empty
}

/** Why the game ended - the same text for both players. */
internal fun reasonFor(status: GameStatus): ResText = when (status) {
    is GameStatus.Checkmate -> ResText(R.string.reason_checkmate, sideNameRes(status.winner))
    is GameStatus.Resigned -> ResText(R.string.reason_resigned, sideNameRes(status.winner))
    is GameStatus.Stalemate -> ResText(R.string.reason_stalemate)
    is GameStatus.DrawFiftyMove -> ResText(R.string.reason_fifty_move)
    is GameStatus.DrawThreefold -> ResText(R.string.reason_threefold)
    is GameStatus.DrawInsufficientMaterial -> ResText(R.string.reason_insufficient_material)
    is GameStatus.AgreedDraw -> ResText(R.string.reason_agreed_draw)
    is GameStatus.Ongoing -> ResText(R.string.empty)
}

/** The status line during a game, from THIS player's point of view. */
@StringRes
private fun runningStatusLine(state: GameUiState, side: Side): Int = when {
    state.inCheck == side -> R.string.status_check
    state.sideToMove == side -> R.string.status_your_turn
    else -> R.string.status_opponent_turn
}
