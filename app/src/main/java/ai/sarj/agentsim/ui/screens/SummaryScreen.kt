package ai.sarj.agentsim.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.data.AgentTip
import ai.sarj.agentsim.data.AgentTips
import ai.sarj.agentsim.model.GameState
import ai.sarj.agentsim.ui.components.InfoRow
import ai.sarj.agentsim.ui.components.Mascot
import ai.sarj.agentsim.ui.components.PrimaryButton
import ai.sarj.agentsim.ui.components.medalDrawableRes
import ai.sarj.agentsim.ui.components.RankBadge
import ai.sarj.agentsim.ui.components.SectionCard
import ai.sarj.agentsim.ui.components.StarIcon
import ai.sarj.agentsim.ui.components.StarRow
import ai.sarj.agentsim.ui.components.XpBar
import ai.sarj.agentsim.ui.theme.Coin
import ai.sarj.agentsim.ui.theme.Good
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary

@Composable
fun SummaryScreen(state: GameState, onRestart: () -> Unit, onHome: () -> Unit, onShop: () -> Unit) {
    val report = state.shiftReport
    val rating = report?.avgStars ?: state.shiftRating
    val grade = report?.grade ?: "—"
    val best = state.reviews.maxByOrNull { it.stars }
    val worst = state.reviews.minByOrNull { it.stars }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Shift report", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(Modifier.height(16.dp))

        // --- Grade + rating ---
        SectionCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Your rating", color = SarjMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    StarRow(rating.toInt(), starSize = 22)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if ((report?.served ?: state.servedByAgent) == 0) "no chats handled"
                        else "%.1f · %d served".format(rating, report?.served ?: state.servedByAgent),
                        color = SarjMuted, fontSize = 12.sp
                    )
                }
                val medalRes = medalDrawableRes(grade)
                if (medalRes != null) {
                    Image(painterResource(medalRes), "grade $grade", Modifier.size(74.dp))
                } else {
                    Text(grade, color = SarjPrimary, fontWeight = FontWeight.Bold, fontSize = 48.sp)
                }
            }
        }

        // --- Rank progress (the meta hook) ---
        if (report != null) {
            Spacer(Modifier.height(12.dp))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(report.rankAfter, size = 44)
                    Box(Modifier.padding(start = 12.dp).weight(1f)) {
                        XpBar(report.xpAfter)
                    }
                }
                if (report.rankedUp) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(report.rankAfter.color).copy(alpha = 0.14f)).padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                StarIcon(filled = true, size = 16.dp)
                                Text("RANK UP — ${report.rankAfter.title}!", color = Color(report.rankAfter.color), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            report.unlocked?.let {
                                Spacer(Modifier.height(2.dp))
                                Text("Unlocked: $it", color = SarjOnDark, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- Payout breakdown ---
        Spacer(Modifier.height(12.dp))
        SectionCard {
            InfoRow("Earned this shift", "+${report?.tokensEarnedShift ?: 0}", Coin)
            InfoRow("Shift bonus (grade $grade)", "+${report?.shiftBonus ?: 0}", Coin)
            InfoRow("XP gained", "+${report?.xpGained ?: 0}", SarjAccent)
            if ((report?.abandoned ?: 0) > 0) InfoRow("Customers lost", "${report?.abandoned}", SarjMuted)
            InfoRow("Best combo", "x${state.bestCombo}")
            InfoRow("AI Assistant used", state.sarjUsed.toString())
        }

        // --- Reviews ---
        if (best != null || worst != null) {
            Spacer(Modifier.height(12.dp))
            SectionCard {
                best?.let {
                    Text("Top review", color = Good, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("“${it.text}” — ${it.customerName} (${it.stars}/5)", color = SarjOnDark, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                }
                if (worst != null && worst !== best) {
                    Spacer(Modifier.height(8.dp))
                    Text("Worst review", color = SarjMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("“${worst.text}” — ${worst.customerName} (${worst.stars}/5)", color = SarjOnDark, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                }
            }
        }

        // The "serious game" payoff — a real customer-service lesson from the mentor after every shift.
        Spacer(Modifier.height(12.dp))
        MentorTipCard(remember(report) { AgentTips.ALL.random() })

        Spacer(Modifier.height(16.dp))
        Text(
            "Read them, look it up, reply, keep them happy — one chat at a time. Tap the AI Assistant when the " +
                "queue piles up and watch it clear the floor in seconds.",
            color = SarjMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "NEW SHIFT", onClick = onRestart)
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onShop) {
            Text("🛒  Spend tokens on upgrades", color = SarjPrimary, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onHome) { Text("Back to home", color = SarjMuted) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MentorTipCard(tip: AgentTip) {
    SectionCard {
        Row(verticalAlignment = Alignment.Top) {
            Mascot(64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("💡 Mentor's tip — ${tip.title}", color = SarjPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(tip.body, color = SarjOnDark, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
