package ai.sarj.agentsim.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ai.sarj.agentsim.ui.theme.SarjMuted

@Composable
fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row {
        repeat(3) { i ->
            val a by transition.animateFloat(
                initialValue = 0.3f, targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.3f at 0
                        1f at 300 + i * 120
                        0.3f at 600 + i * 120
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot$i"
            )
            androidx.compose.foundation.layout.Box(
                Modifier.alpha(a).size(7.dp).clip(CircleShape).background(SarjMuted)
            )
            if (i < 2) Spacer(Modifier.width(4.dp))
        }
    }
}
