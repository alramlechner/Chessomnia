package name.lechners.chessomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
 *
 * The actions are icon buttons, not text buttons, for two reasons. Six labels plus the
 * clock plus the status line need roughly 610dp in English and 635dp in German; a
 * portrait phone offers about 316dp inside the panel, so the trailing buttons - the main
 * menu among them - were simply clipped away. And the labels themselves were poor:
 * German "Zurück" next to a "Menü" button reads as navigation, not as taking back a
 * move. Icons are also immune to the width difference between the two languages.
 *
 * Below [COMPACT_BELOW] the actions move to a second line rather than shrinking. Icons
 * alone do not rescue a portrait phone: six 48dp targets plus clock plus status still
 * want about 468dp.
 *
 * ⚠️ No tooltips and no overflow menu here. Both are popups, and a popup would be drawn
 * unrotated over a panel that is rotated 180 degrees for the player at the top. What
 * carries the meaning instead is the confirmation dialog: resign, draw and takeback all
 * spell out what is about to happen, and they do rotate correctly.
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // maxWidth here is already the inner width, so the padding above is accounted
        // for and the breakpoint can be compared against what the content really gets.
        val compact = maxWidth < COMPACT_BELOW

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        if (over) reasonFor(state.status).resolve()
                        else stringResource(runningStatusLine(state, side)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!compact) {
                    Actions(over, state.canTakeback, clockEnabled, clockRunning,
                        onToggleClock, onTakeback, onDraw, onResign, onSwapSides, onExit)
                }
            }

            if (compact) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Actions(over, state.canTakeback, clockEnabled, clockRunning,
                        onToggleClock, onTakeback, onDraw, onResign, onSwapSides, onExit)
                }
            }
        }
    }
}

/**
 * Below this width the actions get a line of their own.
 *
 * Derived, not guessed: clock (~58dp) + a status line that stays readable (~120dp) +
 * six 48dp icon buttons = 466dp.
 */
private val COMPACT_BELOW = 480.dp

@Composable
private fun Actions(
    over: Boolean,
    canTakeback: Boolean,
    clockEnabled: Boolean,
    clockRunning: Boolean,
    onToggleClock: () -> Unit,
    onTakeback: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    onSwapSides: () -> Unit,
    onExit: () -> Unit,
) {
    if (over) {
        // Once the game is over only the ways back remain - which is also how an
        // accidental resignation gets corrected.
        if (canTakeback) ActionButton(R.drawable.ic_takeback, R.string.action_takeback, onTakeback)
        ActionButton(R.drawable.ic_main_menu, R.string.action_menu, onExit)
        return
    }
    if (clockEnabled) {
        ActionButton(
            if (clockRunning) R.drawable.ic_clock_pause else R.drawable.ic_clock_start,
            if (clockRunning) R.string.action_pause else R.string.action_start_clock,
            onToggleClock,
        )
    }
    if (canTakeback) ActionButton(R.drawable.ic_takeback, R.string.action_takeback, onTakeback)
    ActionButton(R.drawable.ic_draw_offer, R.string.action_draw, onDraw)
    ActionButton(R.drawable.ic_resign, R.string.action_resign, onResign)
    ActionButton(R.drawable.ic_flip_board, R.string.action_swap_colours, onSwapSides)
    ActionButton(R.drawable.ic_main_menu, R.string.action_menu, onExit)
}

/**
 * The label is not drawn but announced: with icon-only buttons the content description
 * is the only thing a screen reader has, so it is required rather than optional.
 */
@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    @StringRes description: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = MaterialTheme.colorScheme.primary,
        )
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
