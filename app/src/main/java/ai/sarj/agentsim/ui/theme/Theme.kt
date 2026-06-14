package ai.sarj.agentsim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SarjPrimary,
    onPrimary = Color.White,
    secondary = SarjAccent,
    onSecondary = Color.White,
    background = SarjBg,
    onBackground = SarjOnDark,
    surface = SarjSurface,
    onSurface = SarjOnDark,
    surfaceVariant = SarjSurfaceHi,
    onSurfaceVariant = SarjMuted,
    outline = SarjLine,
    error = Bad,
    onError = Color.White
)

@Composable
fun AgentSimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
