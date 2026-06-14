package ai.sarj.agentsim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.config.GameConfig
import ai.sarj.agentsim.model.DailyObjective
import ai.sarj.agentsim.model.ObjectiveKind
import ai.sarj.agentsim.ui.theme.Coin
import ai.sarj.agentsim.ui.theme.Good
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjSurfaceHi

@Composable
fun DailyObjectivesCard(daily: List<DailyObjective>, tokens: Int, onReroll: (ObjectiveKind) -> Unit) {
    if (daily.isEmpty()) return
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 Today's goals", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            if (daily.all { it.done }) Text("All done! 🎉", color = Good, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        daily.forEach { o ->
            Spacer(Modifier.height(12.dp))
            ObjectiveRow(o, canReroll = tokens >= GameConfig.DAILY_REROLL_COST, onReroll = { onReroll(o.kind) })
        }
    }
}

@Composable
private fun ObjectiveRow(o: DailyObjective, canReroll: Boolean, onReroll: () -> Unit) {
    val frac = (o.progress.toFloat() / o.kind.target).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(o.kind.text, color = SarjOnDark, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (o.done) "✓ +${o.kind.reward}" else "${o.progress}/${o.kind.target} · +${o.kind.reward}",
                    color = if (o.done) Good else Coin, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(SarjLine)) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (o.done) Good else SarjAccent))
            }
        }
        if (!o.done) {
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(SarjSurfaceHi)
                    .clickable(enabled = canReroll) { onReroll() }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Refresh, "Reroll goal", tint = if (canReroll) SarjMuted else SarjLine, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("${GameConfig.DAILY_REROLL_COST}", color = if (canReroll) SarjMuted else SarjLine, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
