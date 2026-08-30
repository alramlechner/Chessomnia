package name.lechners.chessomnia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = LogoBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = LogoBlue,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = AppOutline,
)

private val LightColors = lightColorScheme(
    primary = LogoBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = LogoNavy,
)

@Composable
fun ChessomniaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
