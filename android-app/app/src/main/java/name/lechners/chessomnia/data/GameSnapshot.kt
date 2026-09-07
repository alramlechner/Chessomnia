package name.lechners.chessomnia.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import name.lechners.chessomnia.engine.Level
import name.lechners.chessomnia.rules.GameStatus
import name.lechners.chessomnia.rules.Side

/**
 * A saved game.
 *
 * Deliberately STARTING POSITION + MOVE LIST rather than a position snapshot: loading
 * replays the moves, which makes the undo history and the repetition counter correct by
 * construction. A plain FEN snapshot would silently lose both on every process death.
 * Cost: about 5 bytes per move.
 *
 * The outcome has to travel separately: resignation, an agreed draw and a timeout do NOT
 * follow from the move list.
 */
@Serializable
data class GameSnapshot(
    /**
     * 1 = a counting-down clock with a base time (up to v1.0.4). Its readings mean the
     * opposite of today's and are discarded on load.
     * 2 = thinking time counting upward.
     */
    val v: Int = CURRENT,
    @SerialName("start_fen") val startFen: String,
    val moves: List<String>,
    @SerialName("clock_enabled") val clockEnabled: Boolean,
    @SerialName("white_ms") val whiteElapsedMs: Long,
    @SerialName("black_ms") val blackElapsedMs: Long,
    /** See [encodeResult]; null = game running, or the outcome follows from the position. */
    val result: String? = null,
    /**
     * The opponent, if this game has one. Two nullable fields rather than a nested
     * object: both default to null, so a game saved before there was an opponent loads
     * as the two-player game it was, with no migration step.
     */
    @SerialName("opponent_level") val opponentLevel: String? = null,
    @SerialName("engine_side") val engineSide: String? = null,
) {
    companion object {
        const val CURRENT = 2

        fun encodeOpponent(config: OpponentConfig?): Pair<String?, String?> =
            config?.let { it.level.name to it.enginePlays.name } ?: (null to null)

        /**
         * Unknown values decode to "no opponent" rather than throwing: a level that a
         * later version removed must not make the saved game unreadable.
         */
        fun decodeOpponent(level: String?, side: String?): OpponentConfig? {
            if (level == null || side == null) return null
            val parsedLevel = Level.entries.firstOrNull { it.name == level } ?: return null
            val parsedSide = Side.entries.firstOrNull { it.name == side } ?: return null
            return OpponentConfig(parsedLevel, parsedSide)
        }

        fun encodeResult(status: GameStatus): String? = when (status) {
            is GameStatus.Resigned -> "RESIGNED_${status.winner}"
            is GameStatus.AgreedDraw -> "AGREED_DRAW"
            // Mate, stalemate, the fifty-move rule, repetition and dead material all
            // re-emerge from the replay and need not be stored.
            else -> null
        }

        fun decodeResult(code: String?): GameStatus? = when {
            code == null -> null
            code == "AGREED_DRAW" -> GameStatus.AgreedDraw
            code.startsWith("RESIGNED_") -> GameStatus.Resigned(sideOf(code, "RESIGNED_"))
            // Old wins on time from v1: the clock no longer ends a game, so a game
            // saved that way simply continues.
            code.startsWith("TIMEOUT_") -> null
            else -> null
        }

        private fun sideOf(code: String, prefix: String): Side =
            if (code.removePrefix(prefix) == Side.WHITE.name) Side.WHITE else Side.BLACK
    }
}
