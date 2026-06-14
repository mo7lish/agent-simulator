package ai.sarj.agentsim.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import ai.sarj.agentsim.ui.theme.Bad
import ai.sarj.agentsim.ui.theme.Coin
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjPrimary
import ai.sarj.agentsim.ui.theme.Star
import kotlin.math.cos
import kotlin.math.sin

/**
 * A one-shot celebratory confetti burst, driven by a single animated progress value.
 * Particle motion is derived deterministically from the index (no RNG needed), so it's
 * cheap and replayable — wrap in `key(id) { ConfettiBurst() }` to retrigger per event.
 */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, particleCount: Int = 44) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1500, easing = LinearEasing)) }
    val colors = listOf(Star, SarjPrimary, SarjAccent, Coin, Bad)

    Canvas(modifier) {
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.40f
        val alpha = (1f - p).coerceIn(0f, 1f)
        for (i in 0 until particleCount) {
            val ang = (i * 137.5f) * (3.14159f / 180f)        // golden-angle fan-out
            val speed = 0.45f + ((i * 53) % 100) / 100f * 0.95f
            val dx = cos(ang) * speed * w * 0.55f * p
            val dy = sin(ang) * speed * h * 0.32f * p + h * 0.85f * p * p   // + gravity
            val s = 7f + ((i * 29) % 6)
            drawRect(
                color = colors[i % colors.size].copy(alpha = alpha),
                topLeft = Offset(cx + dx - s / 2f, cy + dy - s / 2f),
                size = Size(s, s * 1.7f)
            )
        }
    }
}
