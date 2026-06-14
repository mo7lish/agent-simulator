package ai.sarj.agentsim.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjPrimary
import ai.sarj.agentsim.ui.theme.SarjSurfaceHi

@Composable
fun SummonSarjButton(enabled: Boolean, cost: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "summonGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse"
    )
    val bg = if (enabled) Brush.horizontalGradient(listOf(SarjPrimary, SarjAccent))
    else Brush.horizontalGradient(listOf(SarjSurfaceHi, SarjSurfaceHi))
    Row(
        modifier
            .alpha(if (enabled) pulse else 0.5f)
            .pressClickable(enabled = enabled) { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fg = if (enabled) Color.White else SarjMuted
        Icon(Icons.Filled.AutoAwesome, null, tint = fg, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text("AI Assistant", color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        // Make the price unmistakably a coin cost.
        CoinIcon(14.dp)
        Spacer(Modifier.width(2.dp))
        Text("$cost", color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
