package ai.sarj.agentsim.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.sarj.agentsim.R
import ai.sarj.agentsim.config.GameConfig
import ai.sarj.agentsim.model.ObjectiveKind
import ai.sarj.agentsim.model.PlayerProfile
import ai.sarj.agentsim.model.Rank
import ai.sarj.agentsim.ui.components.ActionButton
import ai.sarj.agentsim.ui.components.BrandLogo
import ai.sarj.agentsim.ui.components.DailyObjectivesCard
import ai.sarj.agentsim.ui.components.Mascot
import ai.sarj.agentsim.ui.components.PrimaryButton
import ai.sarj.agentsim.ui.components.RankBadge
import ai.sarj.agentsim.ui.components.SectionCard
import ai.sarj.agentsim.ui.components.XpBar
import ai.sarj.agentsim.ui.components.pressClickable
import ai.sarj.agentsim.ui.theme.Bad
import ai.sarj.agentsim.ui.theme.Coin
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary
import ai.sarj.agentsim.ui.theme.SarjSurfaceHi
import ai.sarj.agentsim.ui.theme.Warn
import kotlinx.coroutines.delay

@Composable
private fun BrandMark(size: Int = 88, pulse: Boolean = false) {
    val scale = if (pulse) {
        val t = rememberInfiniteTransition(label = "brand")
        t.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s").value
    } else 1f
    Image(
        painter = painterResource(R.drawable.logo_mark),
        contentDescription = "Agent Simulator",
        modifier = Modifier.size(size.dp).scale(scale)
    )
}

/** Brief brand hold; the ViewModel auto-advances to the gate/download/loading. */
@Composable
fun SplashScreen() {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        BrandMark()
        Spacer(Modifier.height(18.dp))
        Text("Agent Simulator", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 30.sp)
        Spacer(Modifier.height(4.dp))
        Text("the support-desk game", color = SarjAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun GateScreen(reason: String?) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Block, null, tint = SarjMuted, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("This phone can't run Agent Simulator", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(reason ?: "Your device doesn't meet the requirements.", color = SarjMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("It needs a 64-bit chip and about 6 GB of RAM to run its on-device AI.", color = SarjMuted.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun LoadingScreen(loadError: String?, onRetry: () -> Unit) {
    val messages = listOf("Waking up the AI…", "Loading customer personas…", "Almost there…")
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); idx = (idx + 1) % messages.size } }

    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        BrandMark(pulse = loadError == null)
        Spacer(Modifier.height(22.dp))
        if (loadError == null) {
            CircularProgressIndicator(color = SarjPrimary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(14.dp))
            Text(messages[idx], color = SarjMuted, fontSize = 14.sp)
        } else {
            Text(loadError, color = Bad, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(16.dp))
            ActionButton("Try again", onClick = onRetry)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun HomeScreen(profile: PlayerProfile, onStart: () -> Unit, onShop: () -> Unit, onReroll: (ObjectiveKind) -> Unit, onTutorialDone: () -> Unit) {
    val rank = GameConfig.rankForXp(profile.xp)
    var showAbout by remember { mutableStateOf(false) }
    var showTiers by remember { mutableStateOf(false) }
    // The real onboarding is the in-context, per-request-type coaching during play; the generic
    // overview stays available on demand via the "How to play" button rather than auto-popping.
    var showTutorial by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Mascot(106.dp)
        Text("Agent Simulator", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Text("the support-desk game", color = SarjAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))

        // Rank card — tap to open the full tier map.
        SectionCard(Modifier.pressClickable { showTiers = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank, size = 56)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    XpBar(profile.xp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("View all tiers & unlocks →", color = SarjAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatPill("${profile.tokens}", "tokens", Coin, R.drawable.icon_coin)
                StatPill("${profile.streakDays}", "day streak", Warn, R.drawable.icon_combo_flame)
                StatPill("${profile.lifetimeServed}", "served", SarjAccent, null)
            }
        }

        Spacer(Modifier.height(12.dp))
        DailyObjectivesCard(profile.daily, profile.tokens, onReroll)

        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "START SHIFT", onClick = onStart)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onShop) {
            Text("🛒  Upgrades", color = SarjPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { showTutorial = true }) {
                Text("How to play", color = SarjMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            TextButton(onClick = { showAbout = true }) {
                Text("About", color = SarjMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showAbout) AboutDialog { showAbout = false }
    if (showTiers) TierMapDialog(currentRankIndex = rank.index) { showTiers = false }
    if (showTutorial) TutorialDialog(
        onDismiss = { showTutorial = false },
        onFinish = { showTutorial = false; onTutorialDone() }
    )
}

@Composable
private fun StatPill(value: String, label: String, color: Color, iconRes: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (iconRes != null) Image(painterResource(iconRes), null, Modifier.size(22.dp))
        else Text("🎧", fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = SarjMuted, fontSize = 11.sp)
    }
}

@Composable
private fun AboutDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        SectionCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandLogo(58.dp)
                Spacer(Modifier.height(10.dp))
                Text("Agent Simulator", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("the support-desk game", color = SarjAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = SarjLine)
                Spacer(Modifier.height(14.dp))
                Text("A university project submitted by", color = SarjMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(GameConfig.STUDENT_NAME, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(2.dp))
                Text("Student ID  ${GameConfig.MATRIC}", color = SarjMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                ActionButton("Close", onClick = onClose, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TierMapDialog(currentRankIndex: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        SectionCard {
            Text("Career tiers", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text("Earn XP by serving customers to climb. Each tier unlocks more.", color = SarjMuted, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                GameConfig.RANKS.forEach { r ->
                    TierRow(r, isCurrent = r.index == currentRankIndex)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            ActionButton("Close", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TierRow(rank: Rank, isCurrent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RankBadge(rank, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rank.title, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (rank.xpThreshold == 0L) "start" else "${rank.xpThreshold} XP",
                    color = SarjMuted, fontSize = 11.sp
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "YOU", color = SarjAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SarjSurfaceHi).padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                if (rank.unlockLabel.isBlank()) "Your starting tier." else "Unlocks: ${rank.unlockLabel}",
                color = SarjMuted, fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}
