package ai.sarj.agentsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.data.ConversationSeeds
import ai.sarj.agentsim.game.AppViewModel
import ai.sarj.agentsim.model.CardStatus
import ai.sarj.agentsim.model.Customer
import ai.sarj.agentsim.model.GameState
import ai.sarj.agentsim.model.Txn
import ai.sarj.agentsim.model.TxnFlag
import ai.sarj.agentsim.ui.components.ActionButton
import ai.sarj.agentsim.ui.components.InfoRow
import ai.sarj.agentsim.ui.components.LookupField
import ai.sarj.agentsim.ui.components.SectionCard
import ai.sarj.agentsim.ui.components.pressClickable
import ai.sarj.agentsim.ui.theme.Bad
import ai.sarj.agentsim.ui.theme.Good
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.Warn

/** Shared scaffold for the four tool tabs: a paste-account lookup + a per-tab result panel. */
@Composable
private fun ToolLookup(
    state: GameState,
    title: String,
    instruction: String,
    result: @Composable (Customer) -> Unit
) {
    var input by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(title, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(instruction, color = SarjMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(14.dp))
        LookupField(
            value = input,
            onValueChange = { input = it },
            onPaste = { state.clipboard?.let { input = it } },
            placeholder = "Paste account number",
            pasteEnabled = state.clipboard != null
        )
        Spacer(Modifier.height(16.dp))
        val norm = input.uppercase().filter { !it.isWhitespace() }
        val customer = if (norm.isEmpty()) null
            else state.roster.firstOrNull { it.accountNumber.equals(norm, ignoreCase = true) }
        when {
            input.isBlank() -> Hint("Open a chat, tap the customer's name to copy their account number, then paste it here.")
            customer == null -> Hint("No account matches “$input”. Check the number and try again.")
            else -> result(customer)
        }
    }
}

@Composable
private fun Hint(text: String) {
    SectionCard { Text(text, color = SarjMuted, fontSize = 14.sp, lineHeight = 20.sp) }
}

@Composable
fun AccountsScreen(state: GameState, vm: AppViewModel) {
    ToolLookup(state, "Accounts", "Look up a customer's balance and statement.") { customer ->
        val acct = state.bank.accounts[customer.accountNumber] ?: return@ToolLookup
        SectionCard {
            Text(customer.name, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text("Current balance", color = SarjMuted, fontSize = 12.sp)
            Text(acct.balanceDisplay, color = Good, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = SarjMuted.copy(alpha = 0.15f))
            Spacer(Modifier.height(6.dp))
            Text("Recent transactions", color = SarjMuted, fontSize = 12.sp)
            acct.transactions.forEachIndexed { i, txn ->
                TxnRow(customer.accountNumber, i, txn, vm)
            }
            Spacer(Modifier.height(12.dp))
            if (acct.statementSent) {
                Text("Statement sent ✓", color = Good, fontWeight = FontWeight.SemiBold)
            } else {
                ActionButton("Send statement", onClick = { vm.sendStatement(customer.accountNumber) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TxnRow(accountNumber: String, index: Int, txn: Txn, vm: AppViewModel) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(txn.desc, color = SarjOnDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                if (txn.amount < 0) "-%,d".format(-txn.amount) else "+%,d".format(txn.amount),
                color = if (txn.amount < 0) Bad else Good, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        when (txn.flag) {
            TxnFlag.NONE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxnChip("Dispute", SarjAccent) { vm.disputeTransaction(accountNumber, index) }
                TxnChip("Report fraud", Bad) { vm.reportFraud(accountNumber, index) }
            }
            TxnFlag.DISPUTED -> FlagTag("Disputed ✓", Warn)
            TxnFlag.FRAUD -> FlagTag("Reported as fraud ✓", Bad)
        }
    }
}

@Composable
private fun TxnChip(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.14f))
            .pressClickable { onClick() }.padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun FlagTag(label: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun CardsScreen(state: GameState, vm: AppViewModel) {
    ToolLookup(state, "Cards", "Freeze, unfreeze, or reissue a customer's card.") { customer ->
        val card = state.bank.cards[customer.cardNumber] ?: return@ToolLookup
        val status = card.status
        SectionCard {
            Text(customer.name, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            InfoRow("Card", "•••• ${card.last4}")
            InfoRow(
                "Status",
                when (status) { CardStatus.ACTIVE -> "ACTIVE"; CardStatus.FROZEN -> "FROZEN"; CardStatus.REPLACED -> "REPLACED" },
                when (status) { CardStatus.ACTIVE -> Good; CardStatus.FROZEN -> Warn; CardStatus.REPLACED -> SarjAccent }
            )
            Spacer(Modifier.height(12.dp))
            if (status == CardStatus.REPLACED) {
                Text("A replacement card has been issued ✓", color = Good, fontWeight = FontWeight.SemiBold)
            } else {
                ActionButton(
                    if (status == CardStatus.FROZEN) "Unfreeze card" else "Freeze card",
                    onClick = { vm.setCardStatus(customer.cardNumber, if (status == CardStatus.FROZEN) CardStatus.ACTIVE else CardStatus.FROZEN) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                ActionButton("Reissue (replace) card", onClick = { vm.replaceCard(customer.cardNumber) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun BookingsScreen(state: GameState, vm: AppViewModel) {
    ToolLookup(state, "Bookings", "Reschedule a customer's appointment to the date they asked for.") { customer ->
        val booking = state.bank.bookings[customer.id] ?: return@ToolLookup
        SectionCard {
            Text(customer.name, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            InfoRow("Appointment", booking.type)
            InfoRow("Current date", booking.date, SarjAccent)
            Spacer(Modifier.height(12.dp))
            Text("Move to:", color = SarjMuted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConversationSeeds.rescheduleDates.forEach { date ->
                    val selected = booking.date == date
                    ActionButton(
                        date,
                        enabled = !selected,
                        onClick = { vm.reschedule(customer.id, date) }
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentsScreen(state: GameState, vm: AppViewModel) {
    ToolLookup(state, "Payments", "Process a customer's pending payment.") { customer ->
        val payment = state.bank.paymentFor(customer.id) ?: return@ToolLookup
        SectionCard {
            Text(customer.name, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            InfoRow("Payee", payment.payee)
            InfoRow("Amount", "%,d SAR".format(payment.amount))
            InfoRow("Status", if (payment.processed) "PROCESSED" else "PENDING", if (payment.processed) Good else Warn)
            Spacer(Modifier.height(12.dp))
            if (payment.processed) {
                Text("Payment processed ✓", color = Good, fontWeight = FontWeight.SemiBold)
            } else {
                ActionButton("Process payment", onClick = { vm.processPayment(payment.id) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
