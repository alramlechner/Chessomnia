package name.lechners.chessomnia.ui.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import name.lechners.chessomnia.data.AppInfo
import name.lechners.chessomnia.data.BugReport
import name.lechners.chessomnia.data.BugReportData
import name.lechners.chessomnia.data.ChessomniaPrefs
import name.lechners.chessomnia.data.GameSnapshot
import name.lechners.chessomnia.game.ChessGame
import name.lechners.chessomnia.game.clock.ClockState
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Move
import name.lechners.chessomnia.rules.Piece
import name.lechners.chessomnia.rules.PieceType
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.rules.Square

sealed interface Selection {
    data object None : Selection
    data class Piece(val square: Square) : Selection
    data class AwaitingPromotion(val from: Square, val to: Square, val options: List<Move>) : Selection
}

data class GameUiState(
    val board: Array<Piece?> = arrayOfNulls(128),
    val sideToMove: Side = Side.WHITE,
    val status: GameStatus = GameStatus.Ongoing,
    val inCheck: Side? = null,
    val selection: Selection = Selection.None,
    val hints: List<Move> = emptyList(),
    val lastMove: Move? = null,
    val canTakeback: Boolean = false,
    val moveCount: Int = 0,
    val halfmoveClock: Int = 0,
    val showCoordinates: Boolean = true,
    val allowTakeback: Boolean = true,
    val bottomSide: Side = Side.WHITE,
    /** Pieces captured by White (that is, black ones). */
    val capturedByWhite: List<Piece> = emptyList(),
    val capturedByBlack: List<Piece> = emptyList(),
) {
    val checkedKingSquare: Square?
        get() {
            val side = inCheck ?: return null
            for (i in 0 until 128) {
                if (!Square.isOnBoard(i)) continue
                val p = board[i] ?: continue
                if (p.side == side && p.type == PieceType.KING) return Square(i)
            }
            return null
        }

    // An array inside a data class: equals/hashCode have to be written by hand,
    // otherwise Compose compares references and the board display freezes.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameUiState) return false
        return board.contentEquals(other.board) &&
            sideToMove == other.sideToMove && status == other.status &&
            inCheck == other.inCheck && selection == other.selection &&
            hints == other.hints && bottomSide == other.bottomSide &&
            capturedByWhite == other.capturedByWhite && capturedByBlack == other.capturedByBlack &&
            lastMove == other.lastMove && canTakeback == other.canTakeback &&
            moveCount == other.moveCount && halfmoveClock == other.halfmoveClock &&
            showCoordinates == other.showCoordinates
    }

    override fun hashCode(): Int {
        var r = board.contentHashCode()
        r = 31 * r + sideToMove.hashCode()
        r = 31 * r + status.hashCode()
        r = 31 * r + selection.hashCode()
        r = 31 * r + moveCount
        return r
    }
}

/** What the clock display needs - deliberately separate from the board state. */
data class ClockUiState(
    val enabled: Boolean = false,
    /** Accumulated thinking time - it grows and never expires. */
    val whiteMs: Long = 0L,
    val blackMs: Long = 0L,
    val runningFor: Side? = null,
    val gameOver: Boolean = false,
)

class GameViewModel(
    private val prefs: ChessomniaPrefs,
    private val appInfo: AppInfo,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    private var game: ChessGame = restoreOrNew()

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    /**
     * A flow of its own for the clock. If the time lived in the board state, the
     * 64-square canvas would redraw five times a second for no reason.
     */
    private val _clock = MutableStateFlow(ClockUiState())
    val clock: StateFlow<ClockUiState> = _clock.asStateFlow()

    /**
     * Whether the board is covered because the players took a break. Its own flow, not
     * derived from the clock: see [pauseByUser] for why "paused" and "curtain drawn" are
     * not the same thing. Deliberately not persisted - coming back to a covered board
     * after a restart would only be puzzling.
     */
    private val _curtain = MutableStateFlow(false)
    val curtain: StateFlow<Boolean> = _curtain.asStateFlow()

    /** There is a resumable game (started and not finished). */
    val hasResumableGame: Boolean
        get() = game.moveCount > 0 && !game.status.isOver

    init {
        publish(Selection.None)
        // A pure redraw tick: the time is computed, not incremented. There is nothing
        // to check - the clock cannot run out.
        viewModelScope.launch {
            while (isActive) {
                delay(200)
                if (game.clock.isRunning) publishClock()
            }
        }
    }

    // ── Input ───────────────────────────────────────────────────────────────────

    fun onSquareTap(square: Square) {
        val state = _ui.value
        if (state.selection is Selection.AwaitingPromotion) return
        if (state.status.isOver) return

        val current = (state.selection as? Selection.Piece)?.square

        if (current != null) {
            val targets = game.movesFrom(current).filter { it.to == square }
            when {
                // More than one move to the same target square only happens for a
                // promotion, so no special case is needed here.
                targets.size > 1 -> { publish(Selection.AwaitingPromotion(current, square, targets)); return }
                targets.size == 1 -> { commit(targets[0]); return }
            }
        }

        val piece = game.pieceAt(square)
        // Whoever is not to move cannot select anything at all: a tap on an opponent's
        // piece is ignored entirely - the existing selection even stays in place. If a
        // tap reaches this line, the square was not a valid capture target either; that
        // case is handled further up.
        if (piece != null && piece.side != game.sideToMove) return

        when {
            piece == null || square == current -> publish(Selection.None)
            else -> publish(Selection.Piece(square))
        }
    }

    /** Turns the board, so that swapping colours needs nobody to turn the tablet. */
    fun swapSides() {
        prefs.update { it.copy(boardBottomSide = it.boardBottomSide.opposite) }
        publish(_ui.value.selection)
    }

    fun choosePromotion(type: PieceType) {
        val pending = _ui.value.selection as? Selection.AwaitingPromotion ?: return
        val move = pending.options.firstOrNull { it.promotion == type }
        if (move != null) commit(move) else publish(Selection.None)
    }

    fun cancelPromotion() = publish(Selection.None)

    fun takeback() {
        game.takeback()
        persist()
        publish(Selection.None)
    }

    fun startOrResumeClock() {
        game.startClock(now())
        _curtain.value = false
        publishClock()
    }

    /** Called when the game screen is left - otherwise time would accrue in the menu. */
    fun pauseClock() {
        game.pauseClock(now())
        persist()
        publishClock()
    }

    /**
     * Pausing deliberately, by pressing the button - as opposed to the clock stopping
     * on its own.
     *
     * ⚠️ The distinction matters. The clock also pauses after a takeback and when the
     * game screen is left, and drawing the curtain in those cases would be wrong: a
     * takeback is followed by exactly the discussion in which both players want to look
     * at the position.
     */
    fun pauseByUser() {
        pauseClock()
        _curtain.value = true
    }

    /** A new game - only ever on an explicit request. */
    fun newGame() {
        game = ChessGame.newGame(prefs.settings.value.clockEnabled)
        persist()
        publish(Selection.None)
    }

    // ── State ───────────────────────────────────────────────────────────────────

    private fun commit(move: Move) {
        game.apply(move, now())
        persist()
        publish(Selection.None)
    }

    private fun publish(selection: Selection) {
        val settings = prefs.settings.value
        val sq = (selection as? Selection.Piece)?.square
        val own = if (sq != null) game.movesFrom(sq) else emptyList()

        _ui.value = GameUiState(
            board = game.board.copyOf(),
            sideToMove = game.sideToMove,
            status = game.status,
            inCheck = game.inCheck,
            selection = selection,
            hints = if (settings.showHints) own else emptyList(),
            lastMove = game.lastMove,
            canTakeback = game.canTakeback,
            moveCount = game.moveCount,
            halfmoveClock = game.halfmoveClock,
            showCoordinates = settings.showCoordinates,
            allowTakeback = settings.allowTakeback,
            bottomSide = settings.boardBottomSide,
            capturedByWhite = game.capturedBy(Side.WHITE),
            capturedByBlack = game.capturedBy(Side.BLACK),
        )
        publishClock()
    }

    private fun publishClock() {
        val c = game.clock
        val n = now()
        _clock.value = ClockUiState(
            enabled = game.clockEnabled,
            whiteMs = c.elapsed(Side.WHITE, n),
            blackMs = c.elapsed(Side.BLACK, n),
            runningFor = c.runningFor,
            gameOver = game.status.isOver,
        )
    }

    /**
     * A bug report about the game in progress. Built only when it is shared, so that it
     * shows the state of exactly that moment.
     */
    fun buildBugReport(description: String): String {
        val n = now()
        return BugReport.compose(
            description,
            BugReportData(
                app = appInfo,
                timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                startFen = game.startFen,
                moves = game.moveList(),
                currentFen = game.fen(),
                status = game.status.toString(),
                sideToMove = game.sideToMove,
                inCheck = game.inCheck == game.sideToMove,
                legalMoves = game.allLegalMoveTexts(),
                halfmoveClock = game.halfmoveClock,
                fullmoveNumber = game.fullmoveNumber,
                board = game.board.copyOf(),
                whiteThinkMs = game.clock.elapsed(Side.WHITE, n),
                blackThinkMs = game.clock.elapsed(Side.BLACK, n),
                clockEnabled = game.clockEnabled,
            ),
        )
    }

    // ── Persistence ─────────────────────────────────────────────────────────────

    private fun persist() {
        val c = game.clock
        val n = now()
        prefs.saveGame(
            GameSnapshot(
                startFen = game.startFen,
                moves = game.moveList(),
                clockEnabled = game.clockEnabled,
                whiteElapsedMs = c.elapsed(Side.WHITE, n),
                blackElapsedMs = c.elapsed(Side.BLACK, n),
                result = GameSnapshot.encodeResult(game.status),
            )
        )
    }

    private fun restoreOrNew(): ChessGame {
        val snap = prefs.loadGame() ?: return ChessGame.newGame(prefs.settings.value.clockEnabled)
        val restored = ChessGame.replay(snap.startFen, snap.moves, snap.clockEnabled)
        GameSnapshot.decodeResult(snap.result)?.let { restored.restoreResult(it) }
        // Clock readings from v1 meant REMAINING time and would be badly wrong if read
        // as thinking time - they are discarded.
        val elapsed = if (snap.v >= GameSnapshot.CURRENT) {
            ClockState(snap.whiteElapsedMs, snap.blackElapsedMs, null, 0L)
        } else {
            ClockState.ZERO
        }
        // The clock ALWAYS comes back paused: elapsedRealtime() resets when the device
        // reboots.
        restored.restoreClock(elapsed)
        return restored
    }

    class Factory(
        private val prefs: ChessomniaPrefs,
        private val appInfo: AppInfo,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(prefs, appInfo) as T
    }
}
