package ai.sarj.agentsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.config.GameConfig
import ai.sarj.agentsim.model.PlayerProfile
import ai.sarj.agentsim.model.Upgrade
import ai.sarj.agentsim.model.UpgradeCategory
import ai.sarj.agentsim.ui.components.CoinIcon
import ai.sarj.agentsim.ui.components.SectionCard
import ai.sarj.agentsim.ui.components.pressClickable
import ai.sarj.agentsim.ui.theme.Coin
import ai.sarj.agentsim.ui.theme.Good
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary
import ai.sarj.agentsim.ui.theme.SarjSurface
import ai.sarj.agentsim.ui.theme.SarjSurfaceHi

@Composable
fun ShopScreen(profile: PlayerProfile, onBuy: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // --- Top bar: title + wallet ---
        Row(
            Modifier.fillMaxWidth().background(SarjSurface).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Upgrades", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(SarjSurfaceHi).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(18.dp)
                    Spacer(Modifier.width(5.dp))
                    Text("${profile.tokens}", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                "Keep leveling these up — each level pushes the perk further and costs a little more.",
                color = SarjMuted, fontSize = 13.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))
            UpgradeCategory.entries.forEach { cat ->
                val items = GameConfig.UPGRADES.filter { it.category == cat }
                if (items.isEmpty()) return@forEach
                Text(cat.label.uppercase(), color = SarjAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                items.forEach { up ->
                    val level = profile.upgradeLevels[up.id] ?: 0
                    val maxed = level >= up.maxLevel
                    val cost = GameConfig.upgradeCost(up, level)
                    UpgradeCard(
                        up = up, level = level, maxed = maxed, cost = cost,
                        canAfford = !maxed && profile.tokens >= cost,
                        onBuy = { onBuy(up.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Row(Modifier.fillMaxWidth().background(SarjSurface).padding(16.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Done", color = SarjPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun UpgradeCard(up: Upgrade, level: Int, maxed: Boolean, cost: Int, canAfford: Boolean, onBuy: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(up.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(up.title, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Lv $level/${up.maxLevel}", color = SarjAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(2.dp))
                Text(up.desc, color = SarjMuted, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(7.dp))
                LevelBar(level, up.maxLevel)
            }
            Spacer(Modifier.width(12.dp))
            BuyChip(maxed = maxed, canAfford = canAfford, cost = cost, onBuy = onBuy)
        }
    }
}

@Composable
private fun LevelBar(level: Int, maxLevel: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 0 until maxLevel) {
            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i < level) SarjAccent else SarjLine))
        }
    }
}

@Composable
private fun BuyChip(maxed: Boolean, canAfford: Boolean, cost: Int, onBuy: () -> Unit) {
    if (maxed) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(Good.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Check, null, tint = Good, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("MAX", color = Good, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    } else {
        val tint = if (canAfford) SarjPrimary else SarjLine
        val fg = if (canAfford) androidx.compose.ui.graphics.Color.White else SarjMuted
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("UPGRADE", color = SarjMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(tint)
                    .pressClickable(enabled = canAfford) { onBuy() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinIcon(15.dp)
                Spacer(Modifier.width(4.dp))
                Text("$cost", color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
