package vn.tn.launcher

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen { Home, Apps, Info, Settings }

@Composable
fun T(text: String, size: Int = 16, weight: FontWeight = FontWeight.Normal, secondary: Boolean = false, color: Color? = null) {
    val t = LocalTokens.current
    BasicText(text, style = TextStyle(color = color ?: if (secondary) t.textSecondary else t.textPrimary, fontSize = size.sp, fontWeight = weight))
}

@Composable
fun Sidebar(current: Screen, onHome: () -> Unit, onApps: () -> Unit, onSettings: () -> Unit, onMap: () -> Unit, onBt: () -> Unit, onMusic: () -> Unit) {
    val t = LocalTokens.current
    Column(
        Modifier.width(220.dp).fillMaxHeight().glass(t, RoundedCornerShape(32.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SideItem("Trang chủ", Icons.Default.Home, current == Screen.Home, onHome)
        SideItem("Bản đồ", Icons.Default.Place, false, onMap)
        SideItem("Bluetooth", Icons.Default.Bluetooth, false, onBt)
        SideItem("Âm nhạc", Icons.Default.MusicNote, false, onMusic)
        SideItem("Ứng dụng", Icons.Default.Apps, current == Screen.Apps, onApps)
        Spacer(Modifier.weight(1f))
        SideItem("Cài đặt", Icons.Default.Settings, current == Screen.Settings, onSettings)
    }
}

@Composable
private fun SideItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val t = LocalTokens.current
    val fg = if (selected) Color.White else t.textPrimary
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp))
            .then(if (selected) Modifier.background20(t.accent) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        androidx.compose.foundation.Image(
            imageVector = icon, contentDescription = label,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(fg), modifier = Modifier.size(26.dp),
        )
        T(label, 18, FontWeight.Medium, color = fg)
    }
}

private fun Modifier.background20(c: Color) = this.background(c, RoundedCornerShape(20.dp))

@Composable
fun Dock(pkgs: List<String>, apps: List<AppInfo>, onAll: () -> Unit) {
    val t = LocalTokens.current
    val ctx = LocalContext.current
    Row(
        Modifier.glass(t, RoundedCornerShape(32.dp)).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        pkgs.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }.forEach { a ->
            Column(Modifier.clickable { launchPkg(ctx, a.pkg) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Image(a.icon, a.label, Modifier.size(64.dp).clip(RoundedCornerShape(15.dp)))
                T(a.label, 14, secondary = true)
            }
        }
        Column(Modifier.clickable(onClick = onAll), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).card(t, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Image(Icons.Default.Apps, "Ứng dụng", Modifier.size(32.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(t.textPrimary))
            }
            T("Ứng dụng", 14, secondary = true)
        }
    }
}

@Composable
fun AppsScreen(apps: List<AppInfo>) {
    val t = LocalTokens.current
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val shown = apps.filter { it.label.contains(q, ignoreCase = true) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp).card(t, RoundedCornerShape(28.dp)).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(Icons.Default.Search, null, Modifier.size(24.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(t.textSecondary))
            androidx.compose.foundation.text.BasicTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                textStyle = TextStyle(color = t.textPrimary, fontSize = 18.sp), modifier = Modifier.weight(1f),
                decorationBox = { inner -> Box { if (q.isEmpty()) T("Tìm kiếm ứng dụng…", 18, secondary = true); inner() } },
            )
        }
        LazyVerticalGrid(GridCells.Adaptive(120.dp), Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.pkg }) { a ->
                Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable { launchPkg(ctx, a.pkg) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(a.icon, a.label, Modifier.size(72.dp).clip(RoundedCornerShape(17.dp)))
                    Spacer(Modifier.height(6.dp))
                    T(a.label, 14, secondary = true)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(prefs: Prefs, apps: List<AppInfo>, onInfo: () -> Unit) {
    val t = LocalTokens.current
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); kotlinx.coroutines.delay(10_000) } }
    var weather by remember { mutableStateOf<Weather?>(null) }
    LaunchedEffect(prefs.lat, prefs.lon) { while (true) { weather = fetchWeather(prefs.lat, prefs.lon) ?: weather; kotlinx.coroutines.delay(30 * 60_000L) } }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi")).format(now)

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1.6f).fillMaxHeight().card(t, RoundedCornerShape(28.dp)).padding(24.dp)) {
            T(prefs.vehicleName, 28, FontWeight.SemiBold)
            if (prefs.plate.isNotBlank()) T(prefs.plate, 20, secondary = true)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill("Camera 360°", Icons.Default.Videocam) { launchPkg(ctx, find360(ctx, apps, prefs.cameraPkg)) }
                Pill("Trợ lái", Icons.Default.DirectionsCar) { launchPkg(ctx, prefs.adasPkg) }
                Pill("Thông tin xe", Icons.Default.Info, onInfo)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth().card(t, RoundedCornerShape(28.dp)).padding(24.dp)) {
                T(time, 56, FontWeight.SemiBold)
                T(date.replaceFirstChar { it.uppercase() }, 16, secondary = true)
                Spacer(Modifier.height(8.dp))
                T(weather?.let { "${it.tempC}°C · ${it.text}" } ?: "Chưa có dữ liệu thời tiết", 18)
            }
            MusicCard(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
fun MusicCard(modifier: Modifier) {
    val t = LocalTokens.current
    val ctx = LocalContext.current
    var np by remember { mutableStateOf<NowPlaying?>(null) }
    LaunchedEffect(Unit) { while (true) { np = currentMedia(ctx); kotlinx.coroutines.delay(2000) } }
    Column(modifier.card(t, RoundedCornerShape(28.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        T("Đang phát", 16, secondary = true)
        val cur = np
        if (cur == null) {
            T("Chưa có bài hát", 20, FontWeight.Medium)
            T("Cài đặt → Cấp quyền đọc thông báo (nhạc)", 14, secondary = true)
        } else {
            T(cur.title.ifBlank { "Không rõ tên bài" }, 20, FontWeight.Medium)
            if (cur.artist.isNotBlank()) T(cur.artist, 16, secondary = true)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MediaBtn(Icons.Default.SkipPrevious) { cur.controller.transportControls.skipToPrevious() }
                MediaBtn(if (cur.playing) Icons.Default.Pause else Icons.Default.PlayArrow) {
                    if (cur.playing) cur.controller.transportControls.pause() else cur.controller.transportControls.play()
                }
                MediaBtn(Icons.Default.SkipNext) { cur.controller.transportControls.skipToNext() }
            }
        }
    }
}

@Composable
private fun MediaBtn(icon: ImageVector, onClick: () -> Unit) {
    val t = LocalTokens.current
    Box(Modifier.size(56.dp).glass(t, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Image(icon, null, Modifier.size(28.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(t.textPrimary))
    }
}

@Composable
fun InfoScreen(apps: List<AppInfo>) {
    val t = LocalTokens.current
    val ctx = LocalContext.current
    val dm = ctx.resources.displayMetrics
    val cam = try {
        val pi = ctx.packageManager.getPackageInfo(CAMERA_360_PKG, 0)
        "${pi.versionName ?: "?"} (${pi.versionCode})"
    } catch (e: Exception) { "Chưa cài" }
    val rows = listOf(
        "Thiết bị" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Màn hình" to "${dm.widthPixels}×${dm.heightPixels} · ${dm.densityDpi} dpi",
        "App camera 360" to cam,
        "Số ứng dụng" to apps.size.toString(),
    )
    Column(Modifier.fillMaxSize().card(t, RoundedCornerShape(28.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        T("Thông tin xe", 28, FontWeight.SemiBold)
        rows.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                T(k, 18, secondary = true); T(v, 18, FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Pill(label: String, icon: ImageVector, onClick: () -> Unit) {
    val t = LocalTokens.current
    Row(
        Modifier.height(56.dp).glass(t, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(icon, label, Modifier.size(24.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(t.textPrimary))
        T(label, 16, FontWeight.Medium)
    }
}

@Composable
fun SettingsScreen(mode: String, onMode: (String) -> Unit) {
    val t = LocalTokens.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().card(t, RoundedCornerShape(28.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        T("Cài đặt", 28, FontWeight.SemiBold)
        T("Giao diện", 16, secondary = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("auto" to "Tự động", "light" to "Sáng", "dark" to "Tối").forEach { (k, v) ->
                val sel = mode == k
                Box(
                    Modifier.height(56.dp).clip(RoundedCornerShape(28.dp))
                        .then(if (sel) Modifier.background20(t.accent) else Modifier.glass(t, RoundedCornerShape(28.dp)))
                        .clickable { onMode(k) }.padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center,
                ) { T(v, 18, FontWeight.Medium, color = if (sel) Color.White else null) }
            }
        }
        Pill("Đặt launcher mặc định", Icons.Default.Home) { launchIntent(ctx, Intent(Settings.ACTION_HOME_SETTINGS)) }
        Pill("Cấp quyền đọc thông báo (nhạc)", Icons.Default.MusicNote) { launchIntent(ctx, Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
        Pill("Cài đặt hệ thống", Icons.Default.Settings) { launchIntent(ctx, Intent(Settings.ACTION_SETTINGS)) }
        Spacer(Modifier.weight(1f))
        T("Android ${Build.VERSION.RELEASE} · TN Launcher ${"0.1.0"}", 14, secondary = true)
    }
}
