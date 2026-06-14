package ai.sarj.agentsim.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.config.GameConfig
import ai.sarj.agentsim.model.Rank
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import androidx.compose.runtime.LaunchedEffect

/** The rank emblem — the generated crest badge art for this rank. */
@Composable
fun RankBadge(rank: Rank, size: Int = 64) {
    Image(
        painter = painterResource(rankDrawableRes(rank.id)),
        contentDescription = rank.title,
        modifier = Modifier.size(size.dp)
    )
}

/**
 * Rank title + an animated XP progress bar toward the next rank.
 * Pass [progressOverride] (0..1) to drive the fill explicitly (e.g. the Summary reveal);
 * otherwise it derives from [xp].
 */
@Composable
fun XpBar(xp: Long, modifier: Modifier = Modifier, progressOverride: Float? = null) {
    val rank = GameConfig.rankForXp(xp)
    val next = GameConfig.nextRank(rank)
    val target = progressOverride ?: GameConfig.rankProgress(xp)

    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) { animTarget = target }
    val fill by animateFloatAsState(animTarget, tween(800), label = "xpfill")

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(rank.title, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                if (next == null) "MAX RANK" else "${xp - rank.xpThreshold} / ${next.xpThreshold - rank.xpThreshold} XP",
                color = SarjMuted, fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(SarjLine)) {
            Box(
                Modifier.fillMaxWidth(fill).fillMaxHeight().clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(Color(rank.color), SarjAccent)))
            )
        }
        if (next != null) {
            Spacer(Modifier.height(5.dp))
            Text("Next: ${next.title}", color = SarjMuted, fontSize = 11.sp)
        }
    }
}
