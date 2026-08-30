package name.lechners.chessomnia.game

import name.lechners.chessomnia.game.clock.ClockState
import name.lechners.chessomnia.rules.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The clock is built without any SystemClock dependency - `nowMs` comes from outside.
 * That is exactly why it can be checked here in full against an invented timeline.
 */
class ClockStateTest {

    private val start = ClockState.ZERO

    @Test
    fun startsPausedAtZero() {
        assertFalse(start.isRunning)
        assertEquals(0L, start.elapsed(Side.WHITE, 0L))
        assertEquals(0L, start.elapsed(Side.BLACK, 999_999L))
    }

    @Test
    fun onlyTheRunningSideAccumulates() {
        val running = start.resume(Side.WHITE, 1_000L)
        assertEquals(5_000L, running.elapsed(Side.WHITE, 6_000L))
        assertEquals(0L, running.elapsed(Side.BLACK, 6_000L))
    }

    @Test
    fun switchingFreezesTheFormerSide() {
        val white = start.resume(Side.WHITE, 0L)
        val black = white.switchTo(Side.BLACK, 30_000L)
        // White thought for 30 s and stands still from now on.
        assertEquals("White frozen", 30_000L, black.elapsed(Side.WHITE, 90_000L))
        // Black only counts from the switch at 30 s.
        assertEquals("Black from the switch onwards", 30_000L, black.elapsed(Side.BLACK, 60_000L))
        assertEquals("and onwards", 60_000L, black.elapsed(Side.BLACK, 90_000L))
    }

    @Test
    fun timeAccumulatesAcrossManyTurns() {
        var c = start.resume(Side.WHITE, 0L)
        c = c.switchTo(Side.BLACK, 10_000L)   // Weiss +10 s
        c = c.switchTo(Side.WHITE, 25_000L)   // Schwarz +15 s
        c = c.switchTo(Side.BLACK, 30_000L)   // Weiss +5 s
        assertEquals(15_000L, c.elapsed(Side.WHITE, 30_000L))
        assertEquals(15_000L, c.elapsed(Side.BLACK, 30_000L))
    }

    @Test
    fun pauseAndResumeDoNotLoseOrInventTime() {
        val running = start.resume(Side.WHITE, 0L)
        val paused = running.pause(10_000L)
        assertFalse(paused.isRunning)
        // While paused nothing keeps running, however late it gets.
        assertEquals(10_000L, paused.elapsed(Side.WHITE, 1_000_000L))
        val resumed = paused.resume(Side.WHITE, 1_000_000L)
        assertEquals(15_000L, resumed.elapsed(Side.WHITE, 1_005_000L))
    }

    @Test
    fun pausingTwiceChangesNothing() {
        val paused = start.resume(Side.WHITE, 0L).pause(5_000L)
        assertEquals(paused, paused.pause(60_000L))
    }

    /** The clock has no upper bound - it cannot run out. */
    @Test
    fun timeJustKeepsGrowing() {
        val running = start.resume(Side.WHITE, 0L)
        assertEquals(10_000_000L, running.elapsed(Side.WHITE, 10_000_000L))
    }
}
