package ai.sarj.agentsim.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.model.Customer
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark

@Composable
fun CustomerProfileSheet(customer: Customer, onCopyAccount: () -> Unit, onClose: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(customer.name, customer.persona.accentColor, customer.persona.mood, size = 52)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(customer.name, color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(customer.phone, color = SarjMuted, fontSize = 13.sp)
                Text(customer.persona.label, color = SarjAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = SarjMuted.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))
        CopyableRow("Account number", customer.accountDisplay, onCopy = onCopyAccount)
        InfoRow("Card", customer.cardDisplay)
        Spacer(Modifier.height(12.dp))
        ActionButton("Close", onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}
