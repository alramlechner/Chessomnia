package name.lechners.chessomnia.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import name.lechners.chessomnia.R
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.ui.board.ChessBoard

/**
 * The game screen.
 *
 * The tablet lies flat between the players, who sit opposite each other. The whole
 * structure follows from that: the board square in the middle, and EVERYTHING else
 * twice - one panel at each edge where somebody is sitting. Black's panel is rotated by
 * 180 degrees. A single panel would always be upside down for one of the two.
 *
 * That is also why there are no separate portrait and landscape variants here: the
 * players face each other, never sit side by side, so the panels always belong at the
 * top and the bottom.
 */
@Composable
fun GameScreen(
    vm: GameViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.ui.collectAsState()
    val clockRunning by vm.clock.collectAsState()
    val curtain by vm.curtain.collectAsState()
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }

    // Thinking time runs while the board is visible. The menu does NOT end the game,
    // but no time should accrue there either - so resume on entering and pause on
    // leaving. Since the clock never expires, resuming automatically is harmless.
    DisposableEffect(Unit) {
        vm.startOrResumeClock()
        onDispose { vm.pauseClock() }
    }

    // Who sits at the bottom is decided by the "swap colours" setting - the tablet
    // does not have to be turned around for it.
    val bottom = state.bottomSide
    val top = bottom.opposite
    val capturedBottom = if (bottom == Side.WHITE) state.capturedByWhite else state.capturedByBlack
    val capturedTop = if (top == Side.WHITE) state.capturedByWhite else state.capturedByBlack
    val advantage = materialValue(capturedBottom) - materialValue(capturedTop)

    // The system back gesture closes whatever is on top first. Without this it would
    // fall straight through to the activity and leave the app, which is exactly what it
    // used to do.
    BackHandler(enabled = curtain || confirm != null ||
        state.selection is Selection.AwaitingPromotion) {
        when {
            confirm != null -> confirm = null
            state.selection is Selection.AwaitingPromotion -> vm.cancelPromotion()
            else -> vm.startOrResumeClock()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerPanel(
                side = top,
                state = state,
                clockFlow = vm.clock,
                clockEnabled = clockRunning.enabled,
                clockRunning = clockRunning.runningFor != null,
                onToggleClock = { if (clockRunning.runningFor != null) vm.pauseByUser() else vm.startOrResumeClock() },
                allowTakeback = state.allowTakeback,
                onTakeback = { confirm = ConfirmAction.Takeback(top) },
                onSwapSides = vm::swapSides,
                onNewGame = { confirm = ConfirmAction.NewGame(top, state.moveCount) },
                onExit = onExit,
                modifier = Modifier.rotate(180f),
            )

            CapturedRow(capturedTop, -advantage, state.halfmoveClock, rotated = true, Modifier.padding(top = 4.dp))

            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                // The board is square, so its side is the SMALLER of the two axes.
                //
                // ⚠️ This used to be fillMaxHeight().aspectRatio(1f), which derives the
                // side from the height alone. On a tablet in landscape the height is the
                // smaller axis and that happened to be right; on a phone in portrait it
                // is the larger one, and the board came out 608dp wide on a 344dp
                // screen - about 57 % of it was visible. Deriving the side explicitly
                // is correct in every orientation and does not depend on the order in
                // which aspectRatio() falls back through the incoming constraints.
                ChessBoard(
                    board = state.board,
                    bottomSide = bottom,
                    selected = (state.selection as? Selection.Piece)?.square,
                    hints = state.hints,
                    lastMove = state.lastMove,
                    checkedKing = state.checkedKingSquare,
                    showCoordinates = state.showCoordinates,
                    onSquareTap = vm::onSquareTap,
                    modifier = Modifier.size(min(maxWidth, maxHeight)),
                    piecesHidden = curtain,
                )

                if (curtain) {
                    PauseCurtain(
                        onResume = vm::startOrResumeClock,
                        modifier = Modifier.size(min(maxWidth, maxHeight)),
                    )
                }
            }

            CapturedRow(capturedBottom, advantage, state.halfmoveClock, rotated = false, Modifier.padding(bottom = 4.dp))

            PlayerPanel(
                side = bottom,
                state = state,
                clockFlow = vm.clock,
                clockEnabled = clockRunning.enabled,
                clockRunning = clockRunning.runningFor != null,
                onToggleClock = { if (clockRunning.runningFor != null) vm.pauseByUser() else vm.startOrResumeClock() },
                allowTakeback = state.allowTakeback,
                onTakeback = { confirm = ConfirmAction.Takeback(bottom) },
                onSwapSides = vm::swapSides,
                onNewGame = { confirm = ConfirmAction.NewGame(bottom, state.moveCount) },
                onExit = onExit,
            )
        }

        // The promotion choice is turned to face whoever is moving - in a fixed
        // orientation it would always be upside down for one of them.
        (state.selection as? Selection.AwaitingPromotion)?.let { pending ->
            PromotionOverlay(
                side = state.sideToMove,
                bottomSide = bottom,
                options = pending.options,
                onChoose = vm::choosePromotion,
                onCancel = vm::cancelPromotion,
            )
        }

        confirm?.let { action ->
            ConfirmOverlay(
                action = action,
                bottomSide = bottom,
                onConfirm = {
                    when (action) {
                        is ConfirmAction.Takeback -> vm.takeback()
                        is ConfirmAction.NewGame -> vm.newGame()
                    }
                    confirm = null
                },
                onDismiss = { confirm = null },
            )
        }
    }
}

/**
 * A confirmation knows which panel triggered it - that is the only way to turn it
 * towards the right player.
 */
sealed interface ConfirmAction {
    val initiator: Side

    data class Takeback(override val initiator: Side) : ConfirmAction

    /** Carries the move count so the prompt can say what is about to be lost. */
    data class NewGame(override val initiator: Side, val moveCount: Int) : ConfirmAction
}

/**
 * Covers the board while the players are on a break. The pieces are gone, so the
 * position cannot be studied while the clock is stopped.
 *
 * A symbol rather than a word in the middle: it has no orientation and therefore reads
 * the same from both sides of the table. The word is there too, but twice - once for
 * each seat - which is the same reasoning that gives every panel a twin.
 */
@Composable
private fun PauseCurtain(
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .clickable(onClick = onResume),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.paused),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp).rotate(180f),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_paused),
                contentDescription = stringResource(R.string.paused),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.paused_tap_to_resume),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            stringResource(R.string.paused),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
        )
    }
}
