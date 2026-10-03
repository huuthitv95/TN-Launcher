package vn.tn.launcher

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

enum class Screen { Home, Apps, Info, Settings, Pick }

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
fun HomeScreen(prefs: Prefs, apps: List<AppInfo>, onInfo: () -> Unit, onPick: (String) -> Unit) {
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
                Pill("Camera 360°", Icons.Default.Videocam) { launchPkg(ctx, slotPkg(ctx, apps, prefs, "camera")) }
                Pill("Trợ lái", Icons.Default.DirectionsCar) {
                    val p = slotPkg(ctx, apps, prefs, "adas")
                    if (p == null) onPick("adas") else launchPkg(ctx, p)
                }
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
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val t = LocalTokens.current
    Box(
        Modifier.height(48.dp).clip(RoundedCornerShape(24.dp))
            .then(if (selected) Modifier.background20(t.accent) else Modifier.glass(t, RoundedCornerShape(24.dp)))
            .clickable(onClick = onClick).padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) { T(label, 16, FontWeight.Medium, color = if (selected) Color.White else null) }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    val t = LocalTokens.current
    Row(Modifier.fillMaxWidth().height(56.dp).glass(t, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(120.dp)) { T(label, 16, secondary = true) }
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(color = t.textPrimary, fontSize = 18.sp), modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SettingsScreen(mode: String, onMode: (String) -> Unit, prefs: Prefs, apps: List<AppInfo>, onPick: (String) -> Unit) {
    val t = LocalTokens.current
    val ctx = LocalContext.current
    val cities = listOf(
        "Hà Nội" to (21.0285f to 105.8542f),
        "TP.HCM" to (10.8231f to 106.6297f),
        "Đà Nẵng" to (16.0544f to 108.2022f),
        "Cần Thơ" to (10.0452f to 105.7469f),
        "Hải Phòng" to (20.8449f to 106.6881f),
    )
    Column(
        Modifier.fillMaxSize().card(t, RoundedCornerShape(28.dp)).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        T("Cài đặt", 28, FontWeight.SemiBold)

        T("Giao diện", 16, secondary = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("auto" to "Tự động", "light" to "Sáng", "dark" to "Tối").forEach { (k, v) -> Chip(v, mode == k) { onMode(k) } }
        }

        T("Xe", 16, secondary = true)
        Field("Tên xe", prefs.vehicleName) { prefs.updateVehicle(it, prefs.plate) }
        Field("Biển số", prefs.plate) { prefs.updateVehicle(prefs.vehicleName, it) }

        T("Thành phố (thời tiết)", 16, secondary = true)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cities.forEach { (name, c) ->
                Chip(name, kotlin.math.abs(prefs.lat - c.first) < 0.01f && kotlin.math.abs(prefs.lon - c.second) < 0.01f) { prefs.setCity(c.first, c.second) }
            }
        }

        T("Gán ứng dụng cho các nút", 16, secondary = true)
        listOf("camera", "adas", "tpms", "map", "music", "dock").forEach { k ->
            val cur = if (k == "dock") {
                if (prefs.dock.isEmpty()) "Mặc định" else "${prefs.dock.size} ứng dụng"
            } else {
                val p = slotPkg(ctx, apps, prefs, k)
                apps.firstOrNull { it.pkg == p }?.label ?: "Chưa gán"
            }
            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp)).clickable { onPick(k) }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            ) { T(slotTitle(k), 18); T(cur, 18, secondary = true) }
        }

        T("Hệ thống", 16, secondary = true)
        Pill("Đặt launcher mặc định", Icons.Default.Home) { launchIntent(ctx, Intent(Settings.ACTION_HOME_SETTINGS)) }
        Pill("Cấp quyền đọc thông báo (nhạc)", Icons.Default.MusicNote) { launchIntent(ctx, Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
        Pill("Cài đặt hệ thống", Icons.Default.Settings) { launchIntent(ctx, Intent(Settings.ACTION_SETTINGS)) }
        T("Android ${Build.VERSION.RELEASE} · TN Launcher 0.2.0", 14, secondary = true)
    }
}

@Composable
fun PickScreen(key: String, apps: List<AppInfo>, prefs: Prefs, onDone: () -> Unit) {
    val t = LocalTokens.current
    val isDock = key == "dock"
    Column(Modifier.fillMaxSize().card(t, RoundedCornerShape(28.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        T(if (isDock) "Chọn tối đa 5 ứng dụng cho Dock" else "Chọn ứng dụng cho: ${slotTitle(key)}", 24, FontWeight.SemiBold)
        LazyVerticalGrid(GridCells.Adaptive(120.dp), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps, key = { it.pkg }) { a ->
                val sel = if (isDock) a.pkg in prefs.dock else prefs.slots[key] == a.pkg
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .then(if (sel) Modifier.background(t.accent.copy(alpha = 0.25f)) else Modifier)
                        .clickable {
                            if (isDock) {
                                if (a.pkg in prefs.dock) prefs.dock.remove(a.pkg) else if (prefs.dock.size < 5) prefs.dock.add(a.pkg)
                                prefs.saveDock()
                            } else {
                                prefs.setSlot(key, a.pkg)
                                onDone()
                            }
                        }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(a.icon, a.label, Modifier.size(64.dp).clip(RoundedCornerShape(15.dp)))
                    Spacer(Modifier.height(6.dp))
                    T(a.label, 14, secondary = true)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Dùng mặc định", Icons.Default.Close) {
                if (isDock) { prefs.dock.clear(); prefs.saveDock() } else prefs.setSlot(key, "")
                onDone()
            }
            Pill("Xong", Icons.Default.Check, onDone)
        }
    }
}
