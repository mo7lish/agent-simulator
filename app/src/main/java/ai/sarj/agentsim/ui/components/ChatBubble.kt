package ai.sarj.agentsim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.model.ChatMessage
import ai.sarj.agentsim.model.Sender
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary
import ai.sarj.agentsim.ui.theme.SarjSurfaceHi

@Composable
fun ChatBubble(message: ChatMessage) {
    if (message.sender == Sender.SYSTEM) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
            Text(message.text, color = SarjMuted, fontSize = 12.sp)
        }
        return
    }
    val isAgent = message.sender == Sender.AGENT
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = if (isAgent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isAgent) 16.dp else 4.dp,
                        bottomEnd = if (isAgent) 4.dp else 16.dp
                    )
                )
                .background(if (isAgent) SarjPrimary else SarjSurfaceHi)
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            if (message.streaming && message.text.isBlank()) {
                TypingDots()
            } else {
                Text(
                    text = message.text + if (message.streaming) " ▌" else "",
                    color = if (isAgent) Color.White else SarjOnDark,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
