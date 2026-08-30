package name.lechners.chessomnia.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
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
                onToggleClock = { if (clockRunning.runningFor != null) vm.pauseClock() else vm.startOrResumeClock() },
                onTakeback = { confirm = ConfirmAction.Takeback(top) },
                onResign = { confirm = ConfirmAction.Resign(top) },
                onDraw = { confirm = ConfirmAction.Draw(top) },
                onSwapSides = vm::swapSides,
                onExit = onExit,
                modifier = Modifier.rotate(180f),
            )

            CapturedRow(capturedTop, -advantage, state.halfmoveClock, rotated = true, Modifier.padding(top = 4.dp))

            Box(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                ChessBoard(
                    board = state.board,
                    bottomSide = bottom,
                    selected = (state.selection as? Selection.Piece)?.square,
                    hints = state.hints,
                    lastMove = state.lastMove,
                    checkedKing = state.checkedKingSquare,
                    showCoordinates = state.showCoordinates,
                    onSquareTap = vm::onSquareTap,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                )
            }

            CapturedRow(capturedBottom, advantage, state.halfmoveClock, rotated = false, Modifier.padding(bottom = 4.dp))

            PlayerPanel(
                side = bottom,
                state = state,
                clockFlow = vm.clock,
                clockEnabled = clockRunning.enabled,
                clockRunning = clockRunning.runningFor != null,
                onToggleClock = { if (clockRunning.runningFor != null) vm.pauseClock() else vm.startOrResumeClock() },
                onTakeback = { confirm = ConfirmAction.Takeback(bottom) },
                onResign = { confirm = ConfirmAction.Resign(bottom) },
                onDraw = { confirm = ConfirmAction.Draw(bottom) },
                onSwapSides = vm::swapSides,
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
                        is ConfirmAction.Resign -> vm.resign(action.initiator)
                        is ConfirmAction.Draw -> vm.agreeDraw()
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
    data class Resign(override val initiator: Side) : ConfirmAction
    data class Draw(override val initiator: Side) : ConfirmAction
}
