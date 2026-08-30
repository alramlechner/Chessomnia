package name.lechners.chessomnia.game.clock

import name.lechners.chessomnia.rules.Side

/**
 * The thinking clock - it COUNTS UPWARD and NEVER ends the game.
 *
 * What it measures is how long each player has thought across all of their moves. There
 * is no base time, no expiry, no win on time. That is deliberate: at a home board a clock
 * should inform, not adjudicate.
 *
 * A monotonic base rather than an incrementing counter: elapsed time is always COMPUTED
 * as `accumulated + (now - startedAt)`. Doze, app switching and rotation are therefore
 * correct for free; the UI's 200 ms tick only triggers a redraw.
 *
 * Deliberately free of `SystemClock`: every method takes `nowMs`. That is what makes the
 * clock fully testable against a fake time source.
 */
data class ClockState(
    val whiteElapsedMs: Long,
    val blackElapsedMs: Long,
    /** null means paused - including at the start of a game and when the clock is off. */
    val runningFor: Side?,
    val runningSinceMs: Long,
) {
    val isRunning: Boolean get() = runningFor != null

    private fun accumulated(side: Side): Long =
        if (side == Side.WHITE) whiteElapsedMs else blackElapsedMs

    fun elapsed(side: Side, nowMs: Long): Long {
        val base = accumulated(side)
        if (side != runningFor) return base
        return base + (nowMs - runningSinceMs).coerceAtLeast(0L)
    }

    /** Freezes the running time and starts `side` - or pauses when given null. */
    fun switchTo(side: Side?, nowMs: Long): ClockState = ClockState(
        whiteElapsedMs = elapsed(Side.WHITE, nowMs),
        blackElapsedMs = elapsed(Side.BLACK, nowMs),
        runningFor = side,
        runningSinceMs = nowMs,
    )

    fun pause(nowMs: Long): ClockState = if (isRunning) switchTo(null, nowMs) else this

    fun resume(side: Side, nowMs: Long): ClockState = switchTo(side, nowMs)

    companion object {
        val ZERO = ClockState(0L, 0L, runningFor = null, runningSinceMs = 0L)
    }
}
