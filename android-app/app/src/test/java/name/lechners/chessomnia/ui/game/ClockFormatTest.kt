package name.lechners.chessomnia.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatTest {

    @Test
    fun minutesAndSeconds() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:05", formatTime(5_400))
        assertEquals("1:05", formatTime(65_000))
        assertEquals("59:59", formatTime(3_599_000))
    }

    /** From one hour onwards with an hours part - long games are the rule here. */
    @Test
    fun hoursOnceTheGameGetsLong() {
        assertEquals("1:00:00", formatTime(3_600_000))
        assertEquals("2:03:04", formatTime(7_384_000))
    }

    @Test
    fun neverShowsNegativeTime() {
        assertEquals("0:00", formatTime(-5_000))
    }
}
