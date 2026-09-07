package name.lechners.chessomnia.engine

import name.lechners.chessomnia.rules.Attacks
import name.lechners.chessomnia.rules.Move
import name.lechners.chessomnia.rules.MoveGenerator
import name.lechners.chessomnia.rules.MoveKind
import name.lechners.chessomnia.rules.PieceType
import name.lechners.chessomnia.rules.Position
import name.lechners.chessomnia.rules.RepetitionTracker

/** A root move together with what the search thinks it is worth. */
data class ScoredMove(val move: Move, val score: Int)

/**
 * Alpha-beta search over the existing rule engine.
 *
 * ⚠️ **Nothing in `rules/` is modified or duplicated.** The search walks the very same
 * `makeMove`/`unmakeMove` that perft hammers, so a legal move here is legal by the same
 * code that the board itself uses. Adding an opponent cannot make the app misjudge a
 * mate.
 *
 * One deliberate departure from the UI path: the search calls `pseudoLegalMoves` and
 * does the check test itself. [MoveGenerator.legalMoves] filters by making and unmaking
 * every move - the search would then make and unmake it a second time, doubling the cost
 * of every node. Same rules, same functions, half the work.
 */
class Search(private val now: () -> Long = System::currentTimeMillis) {

    /** Nodes visited in the last [run]. Used by the benchmark, not by the app. */
    var nodes: Long = 0L
        private set

    /** The deepest iteration that finished before the time ran out. */
    var depthReached: Int = 0
        private set

    private var aborted = false
    private var deadline = Long.MAX_VALUE
    private var cancelled: () -> Boolean = { false }

    /**
     * The clock is ignored during the first iteration. Whatever the budget says and
     * however slow the device is, a search that has not finished depth one has no
     * opinion at all - it would hand back a list of zeroes and the engine would play a
     * random legal move. One slow move is a far better failure than a nonsensical one.
     */
    private var enforceDeadline = false

    private val killers = arrayOfNulls<Move>(MAX_PLY * 2)
    private val historyTable = IntArray(128 * 128)

    /**
     * Searches [root] and returns every legal move with its score, best first.
     *
     * The position is copied first: the search runs on a background thread while the UI
     * thread still owns the game's own `Position`. Sharing it would be a data race even
     * though make/unmake restores everything - a half-applied move is visible in between.
     *
     * Every root move is searched with a full window, so the scores are exact and
     * comparable. That is what lets [Level] pick a deliberately imperfect move without a
     * second search pass; it costs depth, which a family-strength opponent can spare.
     */
    fun run(
        root: Position,
        maxDepth: Int,
        budgetMs: Int,
        repetition: RepetitionTracker? = null,
        isCancelled: () -> Boolean = { false },
    ): List<ScoredMove> {
        val pos = root.copy()
        nodes = 0L
        depthReached = 0
        aborted = false
        cancelled = isCancelled
        deadline = now() + budgetMs
        killers.fill(null)
        historyTable.fill(0)

        var ordered = MoveGenerator.legalMoves(pos)
        if (ordered.isEmpty()) return emptyList()

        var best: List<ScoredMove> = ordered.map { ScoredMove(it, 0) }

        for (depth in 1..maxDepth) {
            enforceDeadline = depth > 1
            val scored = searchRoot(pos, ordered, depth, repetition)
            if (aborted) break

            best = scored.sortedByDescending { it.score }
            depthReached = depth
            // Best move first in the next iteration - the single most effective thing
            // one can do for alpha-beta.
            ordered = best.map { it.move }

            // A forced mate is not going to be improved on by looking deeper.
            if (kotlin.math.abs(best[0].score) >= MATE_THRESHOLD) break
        }
        return best
    }

    private fun searchRoot(
        pos: Position,
        rootMoves: List<Move>,
        depth: Int,
        repetition: RepetitionTracker?,
    ): List<ScoredMove> {
        val out = ArrayList<ScoredMove>(rootMoves.size)
        for (m in rootMoves) {
            val undo = pos.makeMove(m)
            // Repetition is only tested at the root, where the actual game history is
            // known. Inside the search it would need a Zobrist hash; the visible problem
            // - a winning engine shuffling into a threefold draw - is solved here.
            val repeats = repetition != null &&
                repetition.count(RepetitionTracker.keyOf(pos)) >= 2
            val score =
                if (repeats) Evaluation.DRAW
                else -negamax(pos, depth - 1, -INFINITY, INFINITY, 1)
            pos.unmakeMove(m, undo)
            if (aborted) return emptyList()
            out.add(ScoredMove(m, score))
        }
        return out
    }

    private fun negamax(pos: Position, depthIn: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        if (checkAbort()) return 0

        // The fifty-move rule is cheap to see coming and stops the engine from grinding
        // on in a position that is already drawn.
        if (pos.halfmoveClock >= 100) return Evaluation.DRAW
        if (ply >= MAX_PLY) return Evaluation.evaluate(pos)

        val us = pos.sideToMove
        val inCheck = Attacks.isInCheck(pos, us)
        // Being in check is never a quiet position: look one further rather than hand a
        // half-finished tactic to the evaluation.
        val depth = if (inCheck) depthIn + 1 else depthIn

        if (depth <= 0) return quiescence(pos, alphaIn, beta, ply)

        nodes++
        var alpha = alphaIn
        var best = -INFINITY
        var legal = 0

        val moves = orderedMoves(pos, ply)
        for (m in moves) {
            val undo = pos.makeMove(m)
            if (Attacks.isInCheck(pos, us)) {
                pos.unmakeMove(m, undo)
                continue
            }
            legal++
            val score = -negamax(pos, depth - 1, -beta, -alpha, ply + 1)
            pos.unmakeMove(m, undo)
            if (aborted) return 0

            if (score > best) best = score
            if (score > alpha) alpha = score
            if (alpha >= beta) {
                remember(m, pos, ply, depth)
                break
            }
        }

        // No legal move at all: mate or stalemate. Counting the ply into the mate score
        // makes the engine prefer the quicker mate and the longer defence.
        if (legal == 0) return if (inCheck) -MATE + ply else Evaluation.DRAW
        return best
    }

    /**
     * Plays out the captures before evaluating.
     *
     * Without it the search would stop in the middle of an exchange and read "a queen
     * up" one ply before the recapture. That single effect is the difference between an
     * opponent that is beatable and one that looks broken, which is why it stays on at
     * every difficulty level.
     */
    private fun quiescence(pos: Position, alphaIn: Int, beta: Int, ply: Int): Int {
        if (checkAbort()) return 0
        nodes++

        val standPat = Evaluation.evaluate(pos)
        if (standPat >= beta) return standPat
        var alpha = if (standPat > alphaIn) standPat else alphaIn
        if (ply >= MAX_PLY - 1) return standPat

        val us = pos.sideToMove
        val moves = orderedMoves(pos, ply, capturesOnly = true)
        for (m in moves) {
            val undo = pos.makeMove(m)
            if (Attacks.isInCheck(pos, us)) {
                pos.unmakeMove(m, undo)
                continue
            }
            val score = -quiescence(pos, -beta, -alpha, ply + 1)
            pos.unmakeMove(m, undo)
            if (aborted) return 0

            if (score >= beta) return score
            if (score > alpha) alpha = score
        }
        return alpha
    }

    // ── Move ordering ───────────────────────────────────────────────────────────

    private fun orderedMoves(pos: Position, ply: Int, capturesOnly: Boolean = false): List<Move> {
        val all = MoveGenerator.pseudoLegalMoves(pos)
        val moves = ArrayList<Move>(all.size)
        for (m in all) {
            if (capturesOnly && !isTactical(pos, m)) continue
            moves.add(m)
        }
        if (moves.size < 2) return moves

        val scores = IntArray(moves.size) { scoreOf(pos, moves[it], ply) }
        // Insertion sort: the lists are short, and this keeps the ordering allocation
        // free apart from the list itself.
        for (i in 1 until moves.size) {
            val move = moves[i]
            val score = scores[i]
            var j = i - 1
            while (j >= 0 && scores[j] < score) {
                moves[j + 1] = moves[j]
                scores[j + 1] = scores[j]
                j--
            }
            moves[j + 1] = move
            scores[j + 1] = score
        }
        return moves
    }

    private fun isTactical(pos: Position, m: Move): Boolean =
        pos.board[m.to.index] != null ||
            m.kind == MoveKind.EN_PASSANT ||
            m.promotion != null

    private fun scoreOf(pos: Position, m: Move, ply: Int): Int {
        val victim = pos.board[m.to.index]
        if (victim != null || m.kind == MoveKind.EN_PASSANT) {
            val victimValue =
                if (victim != null) Evaluation.valueOf(victim.type) else Evaluation.PAWN
            val attacker = pos.board[m.from.index]
            val attackerValue =
                if (attacker != null) Evaluation.valueOf(attacker.type) else 0
            // Most valuable victim, least valuable attacker: take the queen with the
            // pawn before taking the pawn with the queen.
            return CAPTURE_BASE + victimValue * 16 - attackerValue
        }
        if (m.promotion == PieceType.QUEEN) return PROMOTION_BASE
        if (killers[ply * 2] == m) return KILLER_BASE
        if (killers[ply * 2 + 1] == m) return KILLER_BASE - 1
        return historyTable[m.from.index * 128 + m.to.index].coerceAtMost(HISTORY_CAP)
    }

    /** A quiet move that caused a cutoff is worth trying early next time. */
    private fun remember(m: Move, pos: Position, ply: Int, depth: Int) {
        if (pos.board[m.to.index] != null || m.kind == MoveKind.EN_PASSANT) return
        if (killers[ply * 2] != m) {
            killers[ply * 2 + 1] = killers[ply * 2]
            killers[ply * 2] = m
        }
        val i = m.from.index * 128 + m.to.index
        historyTable[i] = (historyTable[i] + depth * depth).coerceAtMost(HISTORY_CAP)
    }

    // ── Time ────────────────────────────────────────────────────────────────────

    /**
     * Checked every 2048 nodes rather than every node: `now()` is a system call, and at
     * a few hundred thousand nodes a second it would show up in the profile.
     */
    private fun checkAbort(): Boolean {
        if (aborted) return true
        if ((nodes and 2047L) == 0L) {
            if (cancelled() || (enforceDeadline && now() >= deadline)) aborted = true
        }
        return aborted
    }

    companion object {
        const val MAX_PLY = 64
        const val INFINITY = 1_000_000
        const val MATE = 30_000

        /** Anything at or beyond this is a forced mate rather than an evaluation. */
        const val MATE_THRESHOLD = MATE - MAX_PLY

        private const val CAPTURE_BASE = 1_000_000
        private const val PROMOTION_BASE = 900_000
        private const val KILLER_BASE = 800_000
        private const val HISTORY_CAP = 700_000
    }
}
