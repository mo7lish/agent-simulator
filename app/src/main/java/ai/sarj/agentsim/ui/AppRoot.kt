package ai.sarj.agentsim.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.sarj.agentsim.game.AppViewModel
import ai.sarj.agentsim.model.Screen
import ai.sarj.agentsim.ui.screens.AppScaffold
import ai.sarj.agentsim.ui.screens.DownloadScreen
import ai.sarj.agentsim.ui.screens.GateScreen
import ai.sarj.agentsim.ui.screens.HomeScreen
import ai.sarj.agentsim.ui.screens.LoadingScreen
import ai.sarj.agentsim.ui.screens.ShopScreen
import ai.sarj.agentsim.ui.screens.SplashScreen
import ai.sarj.agentsim.ui.screens.SummaryScreen

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.boot(context) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))) {
            when (state.screen) {
                Screen.SPLASH -> SplashScreen()
                Screen.GATE -> GateScreen(reason = state.gateReason)
                Screen.DOWNLOAD -> DownloadScreen(state = state, vm = vm)
                Screen.LOADING -> LoadingScreen(loadError = state.loadError, onRetry = { vm.retryLoad(context) })
                Screen.HOME -> HomeScreen(profile = profile, onStart = vm::startShift, onShop = vm::goShop, onReroll = vm::rerollObjective, onTutorialDone = vm::markTutorialSeen)
                Screen.PLAYING -> AppScaffold(state = state, vm = vm, profile = profile)
                Screen.SUMMARY -> SummaryScreen(state = state, onRestart = vm::startShift, onHome = vm::goHome, onShop = vm::goShop)
                Screen.SHOP -> ShopScreen(profile = profile, onBuy = vm::buyUpgrade, onBack = vm::goHome)
            }
        }
    }
}
