package ai.sarj.agentsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.sarj.agentsim.ui.components.ActionButton
import ai.sarj.agentsim.ui.components.Mascot
import ai.sarj.agentsim.ui.components.PrimaryButton
import ai.sarj.agentsim.ui.components.SectionCard
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary

private data class TPage(val emoji: String, val title: String, val body: String)

private val tutorialPages = listOf(
    TPage("", "Welcome, agent!",
        "You run a bank's support desk. Real customers message you with problems — your job is to help them fast and keep your star rating high."),
    TPage("🔎", "Read & look it up",
        "Open a chat to see what they need. Tap the customer's name for their details — copy their account number and paste it into the Accounts, Cards, Bookings or Payments tab to find or change what they're asking about."),
    TPage("💬", "Reply & resolve",
        "Reply by typing or tapping a quick-reply chip. Some requests need an extra step — verify identity, dispute a charge, report fraud. Do what they asked and they'll rate you 1–5 stars."),
    TPage("⏳", "Don't fall behind",
        "Customers left waiting get impatient and walk out angry — that's a strike. Tap the AI Assistant in the top bar to instantly clear a pile-up (for a price)."),
    TPage("📈", "Grow your career",
        "Earn tokens & XP every shift. Spend tokens in the Shop to upgrade, climb the career tiers to unlock tougher customers, and chase your daily goals. Good luck!")
)

/**
 * First-run guided tutorial (also reopenable from Home).
 * [onDismiss] just closes (tapping outside on first-run won't mark it seen, so it gently shows again);
 * [onFinish] (Skip / Let's go) marks it seen and closes.
 */
@Composable
fun TutorialDialog(onDismiss: () -> Unit, onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val p = tutorialPages[page]
    val last = page == tutorialPages.lastIndex

    Dialog(onDismissRequest = onDismiss) {
        SectionCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (page == 0) Mascot(96.dp) else Text(p.emoji, fontSize = 44.sp)
                Spacer(Modifier.height(12.dp))
                Text(p.title, color = SarjOnDark, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(p.body, color = SarjMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tutorialPages.indices.forEach { i ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == page) SarjPrimary else SarjLine))
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (last) {
                    PrimaryButton(text = "Let's go!", onClick = onFinish, modifier = Modifier.fillMaxWidth())
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onFinish) { Text("Skip", color = SarjMuted) }
                        ActionButton("Next", onClick = { page++ })
                    }
                }
            }
        }
    }
}
