package name.lechners.chessomnia.engine

import name.lechners.chessomnia.rules.Move
import name.lechners.chessomnia.rules.Position
import name.lechners.chessomnia.rules.RepetitionTracker
import kotlin.random.Random

/**
 * How strong the opponent plays.
 *
 * ⚠️ Strength is **not** set by search depth alone. A depth-two engine does see two
 * moves perfectly and then hangs a rook for no reason, which reads as broken rather than
 * as beatable. The honest knob is [windowCp]: the search scores every root move exactly,
 * and the engine then picks at random among those within that many centipawns of the
 * best. A wide window means it regularly settles for the second-best idea - the way a
 * human does - while never playing something it can see is losing outright.
 *
 * ⚠️ A window around *the best move* cannot go below a certain strength, however wide it
 * is opened, and that turned out to matter. Whatever the tolerance, a free queen is worth
 * nine pawns more than every alternative, so the window never reaches the second move and
 * the engine takes it every single time. Against a nine-year-old, who leaves a piece
 * hanging several times a game, that alone decides the game - the opponent need not play
 * well, it merely has to accept the presents. [takesWhatIsOffered] is the knob for that
 * and nothing else; see [Engine.chooseMove].
 *
 * The ceiling is deliberately a good club amateur, not maximum strength: this is a board
 * for the family table, and an opponent nobody can beat is not a feature.
 */
enum class Level(
    val maxDepth: Int,
    val budgetMs: Int,
    val windowCp: Int,
    val maxCandidates: Int,
    /**
     * Whether the tolerance is measured from the best move found ( `true` ) or from the
     * position the engine is standing in anyway ( `false` ).
     *
     * `false` is what makes a level weak enough for a child: the engine then plays any
     * move that does not leave it worse off than it already is, which is a different
     * demand from playing the best one. It stops short of throwing pieces away, and it
     * stops looking for the ones being thrown at it.
     */
    val takesWhatIsOffered: Boolean = true,
) {
    /**
     * For a child who has just learned how the pieces move. It keeps its own position
     * together and overlooks nearly everything the other side leaves hanging.
     *
     * 250 cp is the tolerance that lets a pawn go and trades a knight for a pawn, while
     * a rook for nothing - 500 - stays out of reach of the dice. There is deliberately
     * no cap on the number of candidates: with the reference at the standing evaluation
     * the tolerance alone is the whole mechanism, and a cap would quietly turn it back
     * into "play one of the best few moves", which is a much stronger opponent.
     */
    LEARNING(
        maxDepth = 2,
        budgetMs = 200,
        windowCp = 250,
        maxCandidates = Int.MAX_VALUE,
        takesWhatIsOffered = false,
    ),

    /** Sees the immediate tactic, misses the rest. Beatable by a child. */
    BEGINNER(maxDepth = 2, budgetMs = 200, windowCp = 130, maxCandidates = 5),

    /** Punishes a hanging piece, still walks into the odd fork. */
    CASUAL(maxDepth = 4, budgetMs = 500, windowCp = 60, maxCandidates = 3),

    /** Plays a tidy game and mates cleanly. The intended ceiling. */
    CLUB(maxDepth = 6, budgetMs = 1200, windowCp = 15, maxCandidates = 2),
}

/**
 * The virtual opponent.
 *
 * Pure Kotlin with no Android imports, like `rules/` - it runs as a plain JVM unit test,
 * which is the only practical way to check that an opponent actually mates.
 *
 * Threading is the caller's business: [chooseMove] blocks, and the ViewModel is expected
 * to run it off the main thread and pass an [isCancelled] that goes true when the game is
 * taken back or restarted. The engine copies the position before touching it.
 */
class Engine(
    private val level: Level,
    private val random: Random = Random.Default,
    now: () -> Long = System::currentTimeMillis,
) {
    private val search = Search(now)

    /** Diagnostics for the bug report and the benchmark; not shown during a game. */
    val nodes: Long get() = search.nodes
    val depthReached: Int get() = search.depthReached

    /**
     * The move the opponent wants to play, or `null` if there is none - which means the
     * game is already over and the caller should not have asked.
     *
     * @param repetition the game's repetition history, so the engine does not shuffle a
     *   won position into a threefold draw.
     */
    fun chooseMove(
        position: Position,
        repetition: RepetitionTracker? = null,
        isCancelled: () -> Boolean = { false },
    ): Move? {
        // ⚠️ A bare-king ending is searched deeper than the level would otherwise allow.
        // Playing the best move there is not enough on its own: at depth two the strong
        // side follows the mating gradient, runs out of sight one move before the net
        // closes, and shuffles the queen until the position repeats - `BEGINNER` drew
        // king and queen against a bare king every single time before this. The ending
        // is nearly branchless, so the extra plies cost a few milliseconds, and a
        // difficulty setting has no business deciding whether the app can finish a game
        // it has already won.
        val bareKing = Evaluation.isBareKingEndgame(position)

        val scored = search.run(
            root = position,
            maxDepth = if (bareKing) maxOf(level.maxDepth, MATING_DEPTH) else level.maxDepth,
            budgetMs = if (bareKing) maxOf(level.budgetMs, MATING_BUDGET_MS) else level.budgetMs,
            repetition = repetition,
            isCancelled = isCancelled,
        )
        if (scored.isEmpty()) return null

        val best = scored[0]

        // A forced mate - given or received - is never thrown away for the sake of
        // variety. Losing a mate in one to a dice roll is the kind of thing that makes
        // an opponent feel random rather than weak.
        if (kotlin.math.abs(best.score) >= Search.MATE_THRESHOLD) return best.move

        // ⚠️ Converting a bare-king endgame is played at full strength, whatever the
        // level says. Down there every move looks alike to material and tables, so the
        // tolerance below would swamp the only thing that still points anywhere - the
        // term that walks the lone king to the edge - and the engine would shuffle until
        // the fifty-move rule. A beginner should lose to a mate; being handed a draw
        // because the opponent could not finish reads as a broken app, not a weak one.
        if (bareKing) return best.move

        // What the tolerance is measured from. For the upper levels that is the best
        // move: they settle for a slightly worse idea, never for a worse position.
        //
        // ⚠️ The weakest level measures from the *standing* evaluation instead - what the
        // position is worth before anybody does anything clever. The difference is only
        // visible when something is going begging: with a free queen on the board the
        // best move is nine pawns clear of every other, so a window around it admits
        // nothing else however wide it is opened, and the engine takes the queen every
        // time. Measured
        // from the standing evaluation the same window admits every quiet move, because
        // none of them makes the engine's own position any worse - it simply does not
        // notice the present. That is the whole difference between an opponent a child
        // can beat and one that only has to accept what it is given.
        //
        // The minimum is what keeps it honest in the other direction. When every move is
        // bad - a trapped queen, a threat that has to be answered - the standing
        // evaluation is above them all, and taking it as the reference would leave no
        // candidate at all. Falling back to the best score there means the engine still
        // defends as well as it can see; it is careless, not suicidal.
        val reference =
            if (level.takesWhatIsOffered) best.score
            else minOf(best.score, Evaluation.evaluate(position))

        val cutoff = reference - level.windowCp
        var count = 0
        while (count < scored.size && count < level.maxCandidates &&
            scored[count].score >= cutoff
        ) {
            count++
        }
        return scored[random.nextInt(count)].move
    }

    private companion object {
        /**
         * What it takes to actually mate with a lone queen or rook rather than to walk
         * towards it. Four plies is where king and queen stopped repeating; the ending
         * has so few moves that the search finishes in a few milliseconds anyway.
         */
        const val MATING_DEPTH = 4
        const val MATING_BUDGET_MS = 800
    }
}
