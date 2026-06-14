package ai.sarj.agentsim.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.game.AppViewModel
import ai.sarj.agentsim.model.GameState
import ai.sarj.agentsim.ui.components.ActionButton
import ai.sarj.agentsim.ui.components.SectionCard
import ai.sarj.agentsim.ui.theme.Bad
import ai.sarj.agentsim.ui.theme.SarjAccent
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.SarjPrimary

/** Mandatory first-run model setup. On success the ViewModel auto-advances to LOADING → HOME. */
@Composable
fun DownloadScreen(state: GameState, vm: AppViewModel) {
    val context = LocalContext.current
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModel(context, uri)
    }
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Text("Set up your AI customers", color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text(
                "Agent Simulator runs a private AI brain right on your phone — no account, and fully offline " +
                    "after this. One-time ~1.6 GB download.",
                color = SarjMuted, fontSize = 14.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(14.dp))
            if (state.downloading) {
                Text("Setting up… ${(state.downloadProgress * 100).toInt()}%", color = SarjOnDark, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { state.downloadProgress }, modifier = Modifier.fillMaxWidth(), color = SarjPrimary)
            } else {
                ActionButton("Download AI (~1.6 GB)", onClick = { vm.downloadModel(context) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { importer.launch(arrayOf("*/*")) }) {
                    Text("Or import a .task file you already have", color = SarjAccent)
                }
                if (state.loadError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.loadError, color = Bad, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
