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
 * The ceiling is deliberately a good club amateur, not maximum strength: this is a board
 * for the family table, and an opponent nobody can beat is not a feature.
 */
enum class Level(
    val maxDepth: Int,
    val budgetMs: Int,
    val windowCp: Int,
    val maxCandidates: Int,
) {
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
        val scored = search.run(
            root = position,
            maxDepth = level.maxDepth,
            budgetMs = level.budgetMs,
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
        if (Evaluation.isBareKingEndgame(position)) return best.move

        val cutoff = best.score - level.windowCp
        var count = 0
        while (count < scored.size && count < level.maxCandidates &&
            scored[count].score >= cutoff
        ) {
            count++
        }
        return scored[random.nextInt(count)].move
    }
}
