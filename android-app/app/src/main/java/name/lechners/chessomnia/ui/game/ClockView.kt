package name.lechners.chessomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.ui.theme.LogoBlue
import java.util.Locale

/**
 * The clock display collects the tick flow ITSELF - deliberately as deep in the tree as
 * possible. If the game screen read it, the entire board would redraw five times a
 * second.
 */
@Composable
fun ClockView(
    side: Side,
    clockFlow: StateFlow<ClockUiState>,
    modifier: Modifier = Modifier,
) {
    val clock by clockFlow.collectAsState()
    if (!clock.enabled) return

    val ms = if (side == Side.WHITE) clock.whiteMs else clock.blackMs
    val running = clock.runningFor == side

    Text(
        text = formatTime(ms),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        // No warning colour: the clock cannot run out, so there is nothing to warn about.
        color = if (running) LogoBlue else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (running) Color(0x223193C6) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/**
 * Accumulated thinking time as m:ss, and as h:mm:ss from one hour onwards.
 *
 * Deliberately without tenths of a second: those would only matter in time trouble, and
 * there is no such thing here any more.
 */
internal fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    // Locale.ROOT: the clock must show Latin digits even when the device's
    // formatting locale uses another numbering system, because the surrounding
    // UI has already fallen back to English in that case.
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
