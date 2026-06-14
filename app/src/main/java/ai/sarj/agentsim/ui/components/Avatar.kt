package ai.sarj.agentsim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.ui.theme.AvatarColors
import kotlin.math.abs

/** Persona avatar: a soft gradient (per-persona color), initials, and an optional mood-face badge. */
@Composable
fun Avatar(
    name: String,
    accentColor: Long? = null,
    mood: String? = null,
    size: Int = 48,
    modifier: Modifier = Modifier
) {
    val base = accentColor?.let { Color(it) } ?: AvatarColors[abs(name.hashCode()) % AvatarColors.size]
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(base, base.darken(0.18f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(initials(name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.36f).sp)
        }
        if (mood != null) {
            Box(
                Modifier.size((size * 0.40f).dp).align(Alignment.BottomEnd).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(mood, fontSize = (size * 0.24f).sp)
            }
        }
    }
}

private fun initials(name: String): String =
    name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

private fun Color.darken(f: Float): Color = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
