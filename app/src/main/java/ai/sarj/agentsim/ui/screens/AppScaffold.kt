package ai.sarj.agentsim.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.game.AppViewModel
import ai.sarj.agentsim.model.GameState
import ai.sarj.agentsim.model.PlayerProfile
import ai.sarj.agentsim.model.Tab
import ai.sarj.agentsim.ui.components.CoinIcon
import ai.sarj.agentsim.ui.components.ComboFlameIcon
import ai.sarj.agentsim.ui.components.ConfettiBurst
import ai.sarj.agentsim.ui.components.RatingChip
import ai.sarj.agentsim.ui.components.ReviewReveal
import ai.sarj.agentsim.ui.components.ReviewsTicker
import ai.sarj.agentsim.ui.components.SummonSarjButton
import ai.sarj.agentsim.ui.theme.Bad
import ai.sarj.agentsim.ui.theme.Coin
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary
import ai.sarj.agentsim.ui.theme.SarjSurface
import ai.sarj.agentsim.ui.theme.SarjSurfaceHi
import ai.sarj.agentsim.ui.theme.Warn

@Composable
fun AppScaffold(state: GameState, vm: AppViewModel, profile: PlayerProfile) {
    Column(Modifier.fillMaxSize()) {
        TopBar(state, vm, profile)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (state.tab) {
                Tab.CHATS -> ChatsScreen(state, vm, profile)
                Tab.ACCOUNTS -> AccountsScreen(state, vm)
                Tab.CARDS -> CardsScreen(state, vm)
                Tab.BOOKINGS -> BookingsScreen(state, vm)
                Tab.PAYMENTS -> PaymentsScreen(state, vm)
            }
            if (state.sarjActive) SarjBanner(Modifier.align(Alignment.TopCenter))
            state.pendingReview?.let { rev ->
                if (rev.stars >= 5) key(rev.id) { ConfettiBurst(Modifier.fillMaxSize()) }
                ReviewReveal(rev)
            }
        }
        BottomNav(current = state.tab, enabled = !state.sarjActive, onSelect = vm::selectTab)
    }
}

@Composable
private fun TopBar(state: GameState, vm: AppViewModel, profile: PlayerProfile) {
    Column(Modifier.fillMaxWidth().background(SarjSurface).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RatingChip(state.shiftRating, state.shiftStars.size)
            if (state.combo >= 2) { Spacer(Modifier.width(8.dp)); ComboChip(state.combo) }
            if (state.strikes > 0) { Spacer(Modifier.width(8.dp)); StrikeDots(state.strikes, state.maxStrikes) }
            Spacer(Modifier.weight(1f))
            CoinIcon(18.dp)
            Spacer(Modifier.width(4.dp))
            val animTokens by animateIntAsState(targetValue = state.tokens, animationSpec = tween(450), label = "tokens")
            Text("$animTokens", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            SummonSarjButton(
                enabled = state.canSummonSarj && state.tokens >= state.sarjCost,
                cost = state.sarjCost,
                onClick = vm::summonSarj
            )
            IconButton(onClick = vm::toggleMute, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (profile.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    "Mute", tint = SarjMuted, modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = vm::endShift, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Home, "End shift & go home", tint = SarjMuted, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.size(4.dp))
        ReviewsTicker(state.reviews)
        Spacer(Modifier.size(6.dp))
        ShiftProgressBar(state.servedByAgent, state.targetServed, state.shiftStartMs)
    }
}

/** A running shift clock + progress: at served == target the shift ends with your report. */
@Composable
private fun ShiftProgressBar(served: Int, target: Int, shiftStartMs: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shiftStartMs) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val elapsed = if (shiftStartMs > 0L) ((now - shiftStartMs) / 1000L).coerceAtLeast(0L) else 0L
    val frac = (served.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Schedule, null, tint = SarjMuted, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text("%d:%02d".format(elapsed / 60, elapsed % 60), color = SarjMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(SarjLine)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(SarjAccent))
        }
        Spacer(Modifier.width(8.dp))
        Text("shift $served/$target", color = SarjMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ComboChip(combo: Int) {
    val t = rememberInfiniteTransition(label = "combo")
    val pulse by t.animateFloat(1f, 1.12f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "comboPulse")
    val color = when {
        combo >= 6 -> Bad
        combo >= 4 -> Warn
        else -> SarjAccent
    }
    Row(
        Modifier.scale(pulse).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.16f)).padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ComboFlameIcon(13.dp)
        Spacer(Modifier.width(3.dp))
        Text("x$combo", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StrikeDots(strikes: Int, maxStrikes: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until maxStrikes) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (i < strikes) Bad else SarjLine))
        }
    }
}

@Composable
private fun SarjBanner(modifier: Modifier = Modifier) {
    Row(
        modifier.padding(10.dp).clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(SarjPrimary, SarjAccent)))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("AI Assistant is clearing your queue…", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun BottomNav(current: Tab, enabled: Boolean, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = SarjSurface) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = current == tab,
                onClick = { if (enabled) onSelect(tab) },
                enabled = enabled,
                icon = { Icon(iconFor(tab), contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                label = { Text(tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SarjPrimary,
                    selectedTextColor = SarjPrimary,
                    indicatorColor = SarjSurfaceHi,
                    unselectedIconColor = SarjMuted,
                    unselectedTextColor = SarjMuted
                )
            )
        }
    }
}

private fun iconFor(tab: Tab): ImageVector = when (tab) {
    Tab.CHATS -> Icons.Filled.Forum
    Tab.ACCOUNTS -> Icons.Filled.AccountBalance
    Tab.CARDS -> Icons.Filled.CreditCard
    Tab.BOOKINGS -> Icons.Filled.CalendarMonth
    Tab.PAYMENTS -> Icons.Filled.Payments
}
