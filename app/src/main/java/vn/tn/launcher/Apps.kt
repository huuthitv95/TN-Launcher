package vn.tn.launcher

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(val label: String, val pkg: String, val icon: ImageBitmap)

fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(q, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm).toBitmap(160, 160).asImageBitmap()) }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun launchPkg(ctx: Context, pkg: String?) {
    val i = pkg?.takeIf { it.isNotBlank() }?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
    if (i != null) ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    else Toast.makeText(ctx, "Chưa tìm thấy ứng dụng", Toast.LENGTH_SHORT).show()
}

fun launchIntent(ctx: Context, i: Intent) {
    try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (e: Exception) { Toast.makeText(ctx, "Không mở được", Toast.LENGTH_SHORT).show() }
}

fun pkgForCategory(ctx: Context, category: String): String? {
    val i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)
    return ctx.packageManager.resolveActivity(i, 0)?.activityInfo?.packageName?.takeIf { it != "android" }
}

fun pkgForDial(ctx: Context): String? =
    ctx.packageManager.resolveActivity(Intent(Intent.ACTION_DIAL), 0)?.activityInfo?.packageName?.takeIf { it != "android" }

/** App camera 360 của đầu SID/Winca (đã đọc từ APK: com.syu.fourcamera2, activity ActFourCamera). */
const val CAMERA_360_PKG = "com.syu.fourcamera2"

fun find360(ctx: Context, apps: List<AppInfo>, override: String): String? =
    override.takeIf { it.isNotBlank() }
        ?: CAMERA_360_PKG.takeIf { ctx.packageManager.getLaunchIntentForPackage(it) != null }
        ?: apps.firstOrNull { it.label.contains("360") }?.pkg

private fun List<AppInfo>.byLabel(vararg keys: String): String? =
    firstOrNull { app -> keys.any { app.label.contains(it, ignoreCase = true) } }?.pkg

/** App cho từng nút: ưu tiên người dùng đã gán, sau đó tự nhận diện theo hệ thống hoặc tên ứng dụng. */
fun slotPkg(ctx: Context, apps: List<AppInfo>, p: Prefs, key: String): String? {
    p.slots[key]?.takeIf { it.isNotBlank() }?.let { return it }
    return when (key) {
        "camera" -> find360(ctx, apps, "")
        "adas" -> apps.byLabel("adas", "trợ lái")
        "tpms" -> apps.byLabel("tpms", "áp suất lốp")
        "map" -> pkgForCategory(ctx, Intent.CATEGORY_APP_MAPS) ?: apps.byLabel("vietmap", "maps")
        "music" -> pkgForCategory(ctx, Intent.CATEGORY_APP_MUSIC) ?: apps.byLabel("zing", "nhạc", "music")
        else -> null
    }
}

fun slotTitle(key: String) = when (key) {
    "camera" -> "Camera 360°"
    "adas" -> "Trợ lái"
    "tpms" -> "Áp suất lốp"
    "map" -> "Bản đồ"
    "music" -> "Âm nhạc"
    "dock" -> "Dock"
    else -> key
}

fun defaultDock(ctx: Context, apps: List<AppInfo>, p: Prefs): List<String> =
    listOfNotNull(
        slotPkg(ctx, apps, p, "map"),
        pkgForDial(ctx),
        slotPkg(ctx, apps, p, "music"),
        slotPkg(ctx, apps, p, "camera"),
    ).distinct()
