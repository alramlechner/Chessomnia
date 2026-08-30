package name.lechners.chessomnia.game

import name.lechners.chessomnia.game.clock.ClockState
import name.lechners.chessomnia.rules.*

/**
 * A game in progress: position, move history, repetition counter and status.
 *
 * Takebacks go through `Position.unmakeMove` - the same function the move generator's
 * legality filter already uses and that the unmake tests hammer hardest. A separate
 * snapshot stack could drift away from it and would silently lose the repetition
 * history.
 */
class ChessGame private constructor(
    val startFen: String,
    private var position: Position,
    /** Whether thinking time is counted at all. It never ends the game. */
    val clockEnabled: Boolean,
) {
    private val history = ArrayList<MoveRecord>()
    private val repetition = RepetitionTracker()

    /**
     * The clock belongs to the game, not to the UI: only that way does a takeback
     * restore position and thinking time in a single step.
     */
    var clock: ClockState = ClockState.ZERO
        private set

    var status: GameStatus = GameStatus.Ongoing
        private set

    /** Legal moves for the current position, grouped by origin square. */
    var legalMoves: Map<Square, List<Move>> = emptyMap()
        private set

    private var allLegalMoves: List<Move> = emptyList()

    init {
        repetition.push(RepetitionTracker.keyOf(position))
        recompute()
    }

    val board: Array<Piece?> get() = position.board
    val sideToMove: Side get() = position.sideToMove
    val moveCount: Int get() = history.size

    /** Halfmoves without a pawn move or capture - the basis of the fifty-move rule. */
    val halfmoveClock: Int get() = position.halfmoveClock
    val canTakeback: Boolean get() = history.isNotEmpty()
    val lastMove: Move? get() = history.lastOrNull()?.move

    /** The side currently in check, or null. Deliberately not part of [status]. */
    var inCheck: Side? = null
        private set

    fun pieceAt(sq: Square): Piece? = position.pieceAt(sq)

    fun movesFrom(sq: Square): List<Move> = legalMoves[sq] ?: emptyList()

    /**
     * The pieces `side` has taken from the opponent.
     *
     * Read from the move history rather than computed as the difference from the start
     * position: after a promotion that difference would be wrong (suddenly two queens
     * and one pawn fewer, without anything having been captured). Sorted by value so the
     * row looks calm.
     */
    fun capturedBy(side: Side): List<Piece> =
        history.mapNotNull { it.undo.captured }
            .filter { it.side == side.opposite }
            .sortedByDescending { pieceOrder(it.type) }

    fun apply(move: Move, nowMs: Long = 0L): Boolean {
        if (status.isOver) return false
        if (allLegalMoves.none { it == move }) return false

        val clockBefore = clock
        val undo = position.makeMove(move)
        history.add(MoveRecord(move, undo, RepetitionTracker.keyOf(position), status, clockBefore))
        repetition.push(RepetitionTracker.keyOf(position))
        recompute()

        // The clock switches only as a consequence of a move - there is no tapping the
        // clock, because the tablet IS the board.
        if (clockEnabled) {
            clock = if (status.isOver) clock.pause(nowMs) else clock.switchTo(position.sideToMove, nowMs)
        }
        return true
    }

    /** Starts the clock for the side to move (first start or resume). */
    fun startClock(nowMs: Long) {
        if (!clockEnabled || status.isOver || clock.isRunning) return
        clock = clock.resume(position.sideToMove, nowMs)
    }

    fun pauseClock(nowMs: Long) { clock = clock.pause(nowMs) }

    /**
     * Takes back the last move - also after the game has ended, because that is exactly
     * how an accidental "resign" gets corrected.
     */
    fun takeback(): MoveRecord? {
        val record = history.removeLastOrNull() ?: return null
        repetition.pop()
        position.unmakeMove(record.move, record.undo)
        status = record.statusBefore
        // The thinking time spent on the retracted move is returned as well - that is
        // how a takeback works among friends. Afterwards the clock is paused so nothing
        // keeps running during the ensuing discussion.
        clock = record.clockBefore.copy(runningFor = null, runningSinceMs = 0L)
        recomputeMovesOnly()
        return record
    }

    /**
     * Sets an outcome that does NOT follow from the move list (used when restoring).
     *
     * ⚠️ The only remaining caller is the restore path. Resigning and agreeing a draw
     * were dropped from the UI: nothing is recorded anywhere, so how a game ends makes
     * no difference - whoever wants to stop simply starts a new one. GameStatus keeps
     * Resigned and AgreedDraw regardless, because a game saved by an older version can
     * still carry those codes and has to load.
     */
    fun restoreResult(status: GameStatus) { this.status = status }

    fun restoreClock(state: ClockState) { clock = state }

    /** The move list in long algebraic notation - the basis of persistence. */
    fun moveList(): List<String> = history.map { LongAlgebraic.of(it.move) }

    fun fen(): String = Fen.serialize(position)

    val fullmoveNumber: Int get() = position.fullmoveNumber

    /** Every legal move in the current position - the heart of the bug report. */
    fun allLegalMoveTexts(): List<String> = allLegalMoves.map { LongAlgebraic.of(it) }.sorted()

    private fun recompute() {
        recomputeMovesOnly()
        if (!status.isOver) {
            status = TerminalDetector.evaluate(position, allLegalMoves, repetition)
        }
    }

    private fun recomputeMovesOnly() {
        allLegalMoves = MoveGenerator.legalMoves(position)
        legalMoves = allLegalMoves.groupBy { it.from }
        inCheck = if (Attacks.isInCheck(position, position.sideToMove)) position.sideToMove else null
    }

    private fun pieceOrder(type: PieceType): Int = when (type) {
        PieceType.QUEEN -> 5
        PieceType.ROOK -> 4
        PieceType.BISHOP -> 3
        PieceType.KNIGHT -> 2
        PieceType.PAWN -> 1
        PieceType.KING -> 6
    }

    companion object {
        fun newGame(clockEnabled: Boolean = true): ChessGame =
            ChessGame(Fen.START, Position.startPosition(), clockEnabled)

        /**
         * Restores a game by REPLAYING it, not from a position snapshot. That makes the
         * undo history and the repetition counter correct by construction.
         */
        fun replay(startFen: String, moves: List<String>, clockEnabled: Boolean): ChessGame {
            val game = ChessGame(startFen, Fen.parse(startFen), clockEnabled)
            for (text in moves) {
                val move = LongAlgebraic.parse(game.position, text) ?: break
                game.apply(move)
            }
            return game
        }
    }
}
