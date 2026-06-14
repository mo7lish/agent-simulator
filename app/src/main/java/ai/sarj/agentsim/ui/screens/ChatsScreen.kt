package ai.sarj.agentsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.sarj.agentsim.game.AppViewModel
import ai.sarj.agentsim.model.Conversation
import ai.sarj.agentsim.model.ConversationStatus
import ai.sarj.agentsim.model.GameState
import ai.sarj.agentsim.model.GateKind
import ai.sarj.agentsim.model.Intent
import ai.sarj.agentsim.model.PlayerProfile
import ai.sarj.agentsim.ui.components.Avatar
import ai.sarj.agentsim.ui.components.ChatBubble
import ai.sarj.agentsim.ui.components.CustomerProfileSheet
import ai.sarj.agentsim.ui.components.Mascot
import ai.sarj.agentsim.ui.components.StarRow
import ai.sarj.agentsim.ui.components.pressClickable
import ai.sarj.agentsim.ui.theme.Bad
import ai.sarj.agentsim.ui.theme.Good
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.Warn
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary
import ai.sarj.agentsim.ui.theme.SarjSurface
import ai.sarj.agentsim.ui.theme.SarjSurfaceHi

@Composable
fun ChatsScreen(state: GameState, vm: AppViewModel, profile: PlayerProfile) {
    val open = state.openConversation
    if (open != null) ConversationView(open, state, vm, profile) else ConversationList(state, vm)
}

fun intentLabel(i: Intent): String = when (i) {
    Intent.BALANCE -> "Balance inquiry"
    Intent.STATEMENT -> "Statement request"
    Intent.FREEZE_CARD -> "Freeze card"
    Intent.RESCHEDULE_BOOKING -> "Reschedule booking"
    Intent.MAKE_PAYMENT -> "Process payment"
    Intent.COMPLAINT -> "Complaint"
    Intent.DISPUTE_CHARGE -> "Dispute a charge"
    Intent.REPORT_FRAUD -> "Report fraud"
    Intent.REPLACE_CARD -> "Replace card"
}

@Composable
private fun ConversationList(state: GameState, vm: AppViewModel) {
    if (state.conversations.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Mascot(140.dp)
            Spacer(Modifier.height(8.dp))
            Text("All caught up — a new customer will message you soon.", color = SarjMuted, fontSize = 14.sp)
        }
        return
    }
    val ordered = state.conversations.sortedBy { if (it.status == ConversationStatus.ONGOING) 0 else 1 }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ordered, key = { it.id }) { convo ->
            ConversationRow(convo, modifier = Modifier.animateItem()) { vm.openConversation(convo.id) }
        }
    }
}

@Composable
private fun ConversationRow(convo: Conversation, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val resolved = convo.status == ConversationStatus.RESOLVED
    Row(
        modifier.fillMaxWidth()
            .alpha(if (resolved) 0.72f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(SarjSurface)
            .border(1.dp, SarjLine, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(convo.customer.name, convo.customer.persona.accentColor, convo.customer.persona.mood)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(convo.customer.name, color = SarjOnDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    intentLabel(convo.intent),
                    color = SarjAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(SarjAccent.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                convo.lastMessage?.text?.ifBlank { "…" } ?: "…",
                color = SarjMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        if (resolved) {
            StarRow(convo.stars ?: 0, starSize = 13)
        } else {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { convo.patience },
                    modifier = Modifier.size(34.dp),
                    color = patienceColor(convo.patience),
                    trackColor = SarjLine,
                    strokeWidth = 3.dp
                )
                if (convo.unread) Box(Modifier.size(8.dp).clip(CircleShape).background(SarjPrimary))
            }
        }
    }
}

private fun patienceColor(p: Float) = when {
    p > 0.5f -> Good
    p > 0.25f -> Warn
    else -> Bad
}

@Composable
private fun ConversationView(convo: Conversation, state: GameState, vm: AppViewModel, profile: PlayerProfile) {
    var showProfile by remember { mutableStateOf(false) }
    var input by remember(convo.id) { mutableStateOf("") }
    val resolved = convo.status == ConversationStatus.RESOLVED
    val canSend = !resolved && !convo.customerTyping && !state.sarjActive

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(SarjSurface).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = vm::closeConversation) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SarjOnDark)
            }
            Avatar(convo.customer.name, convo.customer.persona.accentColor, convo.customer.persona.mood, size = 38)
            Spacer(Modifier.width(10.dp))
            Column(
                Modifier.weight(1f).clickable { showProfile = true }
            ) {
                Text(convo.customer.name, color = SarjOnDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${intentLabel(convo.intent)} · tap for details", color = SarjMuted, fontSize = 11.sp)
            }
        }
        HorizontalDivider(color = SarjMuted.copy(alpha = 0.15f))

        // First time you meet a request type (each unlocks at a new tier), the mentor walks you through it.
        if (convo.intent.name !in profile.seenIntents) {
            IntentCoachCard(convo.intent) { vm.markIntentSeen(convo.intent) }
        }

        // Messages
        val listState = rememberLazyListState()
        androidx.compose.runtime.LaunchedEffect(convo.messages.size, convo.lastMessage?.text?.length) {
            if (convo.messages.isNotEmpty()) listState.scrollToItem(convo.messages.size - 1)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(convo.messages, key = { it.id }) { ChatBubble(it) }
        }

        if (resolved) {
            val abandoned = convo.abandoned
            val tint = if (abandoned) Bad else Good
            Row(
                Modifier.fillMaxWidth().background(tint.copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (abandoned) Icons.Filled.Cancel else Icons.Filled.CheckCircle, null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (abandoned) "Customer walked out" else "Chat closed", color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                StarRow(convo.stars ?: 0, starSize = 16)
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                when {
                    convo.gate == GateKind.VERIFY_ID && !convo.gateCleared ->
                        VerifyPanel(convo, enabled = !state.sarjActive, onAsk = { vm.askVerify(convo.id) }, onConfirm = { vm.confirmVerify(convo.id) })
                    convo.gate != GateKind.NONE && convo.gateCleared -> VerifiedChip()
                }
                if (convo.intent == Intent.COMPLAINT) ComplaintHint()
                QuickReplyRow(convo.intent, enabled = canSend) { vm.sendAgentMessage(it) }
                InputRow(
                    value = input,
                    onValueChange = { input = it },
                    enabled = canSend,
                    onSend = {
                        if (input.isNotBlank()) { vm.sendAgentMessage(input); input = "" }
                    }
                )
            }
        }
    }

    if (showProfile) {
        Dialog(onDismissRequest = { showProfile = false }) {
            CustomerProfileSheet(
                customer = convo.customer,
                onCopyAccount = { vm.copyToClipboard(convo.customer.accountNumber) },
                onClose = { showProfile = false }
            )
        }
    }
}

private fun quickRepliesFor(intent: Intent): List<String> {
    val open = "Hi! 👋 Happy to help."
    val check = "Let me check that for you."
    val close = "Anything else I can help with?"
    return when (intent) {
        Intent.COMPLAINT -> listOf("I'm so sorry — that shouldn't have happened. 🙏", "I completely understand. Let me make this right for you.", "Is there anything else I can do?")
        Intent.FREEZE_CARD -> listOf(open, "I'll secure your card right away.", close)
        Intent.MAKE_PAYMENT -> listOf(open, "I'll process that payment now.", close)
        Intent.BALANCE -> listOf(open, check, close)
        Intent.STATEMENT -> listOf(open, "I'll send your statement now.", close)
        Intent.RESCHEDULE_BOOKING -> listOf(open, "I'll move your appointment.", close)
        Intent.DISPUTE_CHARGE -> listOf(open, "I'll raise a dispute on that charge.", close)
        Intent.REPORT_FRAUD -> listOf("That does look suspicious — let me check.", "I'll flag that as fraud right away.", close)
        Intent.REPLACE_CARD -> listOf(open, "I'll order you a replacement card.", close)
    }
}

@Composable
private fun QuickReplyRow(intent: Intent, enabled: Boolean, onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickRepliesFor(intent).forEach { chip ->
            Text(
                chip,
                color = if (enabled) SarjPrimary else SarjMuted,
                fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(SarjSurfaceHi).pressClickable(enabled = enabled) { onPick(chip) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun VerifyPanel(convo: Conversation, enabled: Boolean, onAsk: () -> Unit, onConfirm: () -> Unit) {
    val first = convo.customer.name.substringBefore(' ')
    Column(Modifier.fillMaxWidth().background(Warn.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, null, tint = Warn, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (!convo.verifyAsked) "Verify $first's identity before you action this."
                else "$first confirmed their card — verify it to proceed.",
                color = SarjOnDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        if (!convo.verifyAsked) {
            VerifyActionChip("🔒  Ask $first to confirm their card", enabled, onAsk)
        } else {
            VerifyActionChip("✓  Confirm identity & proceed", enabled, onConfirm)
        }
    }
}

@Composable
private fun VerifyActionChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, color = if (enabled) SarjPrimary else SarjMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SarjSurface)
            .border(1.dp, SarjLine, RoundedCornerShape(10.dp))
            .pressClickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun VerifiedChip() {
    Row(
        Modifier.fillMaxWidth().background(Good.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = Good, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("Identity verified", color = Good, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun intentHowTo(intent: Intent): List<String> = when (intent) {
    Intent.BALANCE -> listOf("Tap the customer's name to copy their account number.", "Open the Accounts tab and paste it to see their balance.", "Tell them the figure to resolve.")
    Intent.STATEMENT -> listOf("Copy their account number (tap their name).", "In the Accounts tab, paste it and tap ‘Send statement’.", "Confirm it's on the way.")
    Intent.FREEZE_CARD -> listOf("Verify identity first — tap the 🔒 prompt and confirm their card.", "Open the Cards tab, paste their account, tap ‘Freeze card’.", "Reassure them it's frozen.")
    Intent.RESCHEDULE_BOOKING -> listOf("Note the new date they asked for.", "Open the Bookings tab, paste their account, pick that date.", "Confirm the new date.")
    Intent.MAKE_PAYMENT -> listOf("Verify identity first (the 🔒 prompt).", "Open the Payments tab, paste their account, tap ‘Process payment’.", "Confirm it's done.")
    Intent.COMPLAINT -> listOf("Nothing to look up here.", "Reply with a sincere apology to acknowledge them.", "Then reassure them you'll fix it — that resolves it.")
    Intent.DISPUTE_CHARGE -> listOf("Note the charge they named (merchant + amount).", "Open Accounts, paste their account, find that exact transaction.", "Tap ‘Dispute’ on the matching row.")
    Intent.REPORT_FRAUD -> listOf("Open Accounts and paste their account.", "Scan the transactions for the suspicious one.", "Tap ‘Report fraud’ on it.")
    Intent.REPLACE_CARD -> listOf("Verify identity first (the 🔒 prompt).", "Open the Cards tab, paste their account.", "Tap ‘Reissue (replace) card’.")
}

/** First-time, in-context walkthrough for a request type — the mentor explains exactly what to do. */
@Composable
private fun IntentCoachCard(intent: Intent, onGotIt: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp)).background(SarjPrimary.copy(alpha = 0.08f)).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Mascot(52.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("New request — how to: ${intentLabel(intent)}", color = SarjPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                intentHowTo(intent).forEachIndexed { i, step ->
                    Text("${i + 1}.  $step", color = SarjOnDark, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Got it 👍", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(10.dp)).background(SarjPrimary)
                .pressClickable { onGotIt() }.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ComplaintHint() {
    Row(
        Modifier.fillMaxWidth().background(SarjAccent.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💬", fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            "They're upset — nothing to look up here. Calm them with a genuine apology, then reassure them you'll fix it.",
            color = SarjOnDark, fontSize = 11.sp, lineHeight = 15.sp
        )
    }
}

@Composable
private fun InputRow(value: String, onValueChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(SarjSurface).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = { Text(if (enabled) "Type your reply…" else "Customer is typing…", color = SarjMuted) },
            modifier = Modifier.weight(1f),
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send, "Send",
                tint = if (enabled && value.isNotBlank()) SarjPrimary else SarjMuted,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
