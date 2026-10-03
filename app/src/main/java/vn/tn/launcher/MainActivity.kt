package vn.tn.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TnApp(resumeTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
fun TnApp(resumeTick: Int) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var mode by remember { mutableStateOf(prefs.themeMode) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var pickKey by remember { mutableStateOf("") }
    val openPick: (String) -> Unit = { pickKey = it; screen = Screen.Pick }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    LaunchedEffect(resumeTick) { apps = loadApps(ctx) }

    // Tự động: tối từ 18h đến 6h.
    val hour = remember(resumeTick) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> hour >= 18 || hour < 6 }
    val t = if (dark) DarkTokens else LightTokens

    BackHandler(enabled = screen != Screen.Home) { screen = if (screen == Screen.Pick) Screen.Settings else Screen.Home }

    CompositionLocalProvider(LocalTokens provides t) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(t.wallTop, t.wallBottom)))) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Sidebar(
                    current = screen,
                    onHome = { screen = Screen.Home },
                    onApps = { screen = Screen.Apps },
                    onSettings = { screen = Screen.Settings },
                    onMap = { launchPkg(ctx, slotPkg(ctx, apps, prefs, "map")) },
                    onBt = { launchIntent(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    onMusic = { launchPkg(ctx, slotPkg(ctx, apps, prefs, "music")) },
                )
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (screen) {
                            Screen.Home -> HomeScreen(prefs, apps, { screen = Screen.Info }, openPick)
                            Screen.Apps -> AppsScreen(apps)
                            Screen.Info -> InfoScreen(apps)
                            Screen.Settings -> SettingsScreen(mode, { mode = it; prefs.themeMode = it }, prefs, apps, openPick)
                            Screen.Pick -> PickScreen(pickKey, apps, prefs) { screen = Screen.Settings }
                        }
                    }
                    if (screen == Screen.Home) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Dock(if (prefs.dock.isNotEmpty()) prefs.dock.toList() else defaultDock(ctx, apps, prefs), apps) { screen = Screen.Apps }
                        }
                    }
                }
            }
        }
    }
}
