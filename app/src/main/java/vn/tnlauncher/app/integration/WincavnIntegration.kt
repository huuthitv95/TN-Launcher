package vn.tnlauncher.app.integration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo

sealed interface LaunchResult {
    data object Opened : LaunchResult
    data object Missing : LaunchResult
    data object Denied : LaunchResult
    data class Error(val reason: String) : LaunchResult
}
enum class Availability { AVAILABLE, PACKAGE_MISSING, COMPONENT_UNAVAILABLE, PERMISSION_REQUIRED, UNVERIFIED }
data class Capability(val id: String, val title: String, val availability: Availability, val component: String?, val note: String)

object ActivityAccess {
    fun availability(enabled: Boolean, exported: Boolean, permissionGranted: Boolean): Availability = when {
        !enabled || !exported -> Availability.COMPONENT_UNAVAILABLE
        !permissionGranted -> Availability.PERMISSION_REQUIRED
        else -> Availability.AVAILABLE
    }
    fun runStart(start: () -> Unit): LaunchResult = try {
        start(); LaunchResult.Opened
    } catch (_: android.content.ActivityNotFoundException) {
        LaunchResult.Missing
    } catch (_: SecurityException) {
        LaunchResult.Denied
    } catch (_: RuntimeException) {
        LaunchResult.Error("Không mở được ứng dụng.")
    }
}

class SafeActivityLauncher(private val context: Context) {
    data class ResolvedTarget(val component: ComponentName?, val availability: Availability)

    fun resolve(intent: Intent): ResolvedTarget = try {
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
        val component = info?.let { ComponentName(it.packageName, it.name) }
        ResolvedTarget(component, component?.let(::inspect) ?: Availability.COMPONENT_UNAVAILABLE)
    } catch (_: SecurityException) {
        ResolvedTarget(null, Availability.PERMISSION_REQUIRED)
    } catch (_: RuntimeException) {
        ResolvedTarget(null, Availability.UNVERIFIED)
    }

    fun inspect(component: ComponentName): Availability {
        return try {
            val pm = context.packageManager
            val info = pm.getActivityInfo(component, 0)
            val permissionGranted = info.permission.isNullOrEmpty() ||
                context.checkSelfPermission(info.permission) == PackageManager.PERMISSION_GRANTED
            fun enabled(setting: Int, manifestEnabled: Boolean): Boolean = when (setting) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> manifestEnabled
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                else -> false
            }
            val enabled = enabled(pm.getComponentEnabledSetting(component), info.enabled) &&
                enabled(pm.getApplicationEnabledSetting(component.packageName), info.applicationInfo.enabled) &&
                info.applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED == 0
            ActivityAccess.availability(enabled, info.exported, permissionGranted)
        } catch (_: PackageManager.NameNotFoundException) {
            return Availability.COMPONENT_UNAVAILABLE
        } catch (_: SecurityException) { Availability.PERMISSION_REQUIRED }
        catch (_: RuntimeException) { Availability.UNVERIFIED }
    }
    fun launch(component: ComponentName): LaunchResult = when (inspect(component)) {
        Availability.AVAILABLE -> ActivityAccess.runStart {
            context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        }
        Availability.PERMISSION_REQUIRED -> LaunchResult.Denied
        Availability.UNVERIFIED -> LaunchResult.Error("Chưa xác minh được ứng dụng.")
        else -> LaunchResult.Missing
    }

    // Keep action/data/extras for public Android settings screens; never substitute MAIN.
    fun launch(intent: Intent): LaunchResult {
        val target = resolve(intent)
        return when (target.availability) {
            Availability.AVAILABLE -> ActivityAccess.runStart {
                context.startActivity(Intent(intent).setComponent(target.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            Availability.PERMISSION_REQUIRED -> LaunchResult.Denied
            Availability.UNVERIFIED -> LaunchResult.Error("Chưa xác minh được màn hình hệ thống.")
            else -> LaunchResult.Missing
        }
    }
}

interface DeviceCapabilities { fun inspect(): List<Capability> }
interface WincavnIntegration : DeviceCapabilities { fun open(id: String): LaunchResult }
interface CameraIntegration { fun openManual(): LaunchResult }

data class VendorActivityEntry(val id: String, val title: String, val pkg: String, val activity: String,
    val usePackageLauncher: Boolean = true, val action: String? = null)

class AndroidWincavnIntegration(private val context: Context,
    private val entries: List<VendorActivityEntry> = DEFAULT_ENTRIES) : WincavnIntegration, CameraIntegration {
    companion object {
    // Only exported activities evidenced in supplied APK Manifests. No broadcasts, Binder or services.
    private val DEFAULT_ENTRIES = listOf(
        VendorActivityEntry("camera", "Camera 360 · mở thủ công", "com.syu.fourcamera2", "com.syu.fourcamera.act.ActFourCamera"),
        VendorActivityEntry("music", "Nhạc Wincavn", "com.syu.music", "com.syu.app.Activity_All"),
        VendorActivityEntry("bluetooth", "Bluetooth Wincavn", "com.syu.bt", "com.syu.bt.BtAct"),
        VendorActivityEntry("settings", "Cài đặt Wincavn", "com.syu.settings", "com.syu.settings.MainActivity"),
        VendorActivityEntry("radio", "Radio Wincavn", "com.syu.radio", "com.syu.radio.Launch"),
        VendorActivityEntry("video", "Video Wincavn", "com.syu.video", "com.syu.video.main.VideoListActivity"),
        VendorActivityEntry("equalizer", "Equalizer Wincavn", "com.syu.eq", "com.syu.eq.activity.MainActivity"),
        VendorActivityEntry("files", "Quản lý tệp Wincavn", "com.syu.filemanager", "com.syu.filemanager.FileManagerActivity"),
        VendorActivityEntry("aux", "AUX Wincavn", "com.syu.av", "com.syu.av.ActivityLaunch"),
        VendorActivityEntry("guide", "Hướng dẫn Wincavn", "com.syu.guide", "com.syu.guide.GuideActivity"),
        // A different screen in the Bluetooth package: its launcher opens BtAct, not BT audio.
        VendorActivityEntry("bluetooth_audio", "Âm thanh Bluetooth Wincavn", "com.syu.bt", "com.syu.bt.act.ActBtAvStart", false),
        VendorActivityEntry("navigation", "Yêu cầu điều hướng Wincavn", "com.syu.onekeynavi", "com.syu.onekeynavi.LaunchNavi", false, "com.syu.onekeynavi"),
    )
    }
    private fun capability(entry: VendorActivityEntry): Capability {
        val pm = context.packageManager
        try { pm.getApplicationInfo(entry.pkg, 0) } catch (_: PackageManager.NameNotFoundException) {
            return Capability(entry.id, entry.title, Availability.PACKAGE_MISSING, null, "Chưa cài ứng dụng gốc.")
        } catch (_: RuntimeException) {
            return Capability(entry.id, entry.title, Availability.UNVERIFIED, null, "Không đọc được thông tin package.")
        }
        val launcher = try { pm.getLaunchIntentForPackage(entry.pkg)?.component?.takeIf { it.packageName == entry.pkg } }
        catch (_: RuntimeException) { null }
        val candidates = (if (entry.usePackageLauncher) listOfNotNull(launcher, ComponentName(entry.pkg, entry.activity))
            else listOf(ComponentName(entry.pkg, entry.activity))).distinct()
            .map { it to SafeActivityLauncher(context).inspect(it) }
        val (component, availability) = candidates.firstOrNull { it.second == Availability.AVAILABLE }
            ?: candidates.firstOrNull { it.second == Availability.PERMISSION_REQUIRED }
            ?: candidates.firstOrNull { it.second == Availability.UNVERIFIED } ?: candidates.first()
        return Capability(entry.id, entry.title, availability, component.flattenToString(),
            if (availability == Availability.AVAILABLE) "Entry point khả dụng; chức năng phần cứng chưa kiểm chứng trên xe."
            else "Component không khả dụng hoặc cần quyền vendor.")
    }
    override fun inspect(): List<Capability> = entries.map { entry ->
        runCatching { capability(entry) }.getOrElse {
            Capability(entry.id, entry.title, Availability.UNVERIFIED, null, "Không đọc được trạng thái vendor.")
        }
    }
    override fun open(id: String): LaunchResult {
        val entry = entries.firstOrNull { it.id == id } ?: return LaunchResult.Missing
        val cap = runCatching { capability(entry) }.getOrNull() ?: return LaunchResult.Error("Không đọc được trạng thái vendor.")
        if (cap.availability == Availability.PERMISSION_REQUIRED) return LaunchResult.Denied
        if (cap.availability == Availability.UNVERIFIED) return LaunchResult.Error("Chưa xác minh được ứng dụng.")
        if (cap.availability != Availability.AVAILABLE) return LaunchResult.Missing
        return cap.component?.let(ComponentName::unflattenFromString)?.let {
            if (entry.usePackageLauncher) SafeActivityLauncher(context).launch(it)
            else SafeActivityLauncher(context).launch(Intent().apply { action = entry.action; component = it })
        } ?: LaunchResult.Missing
    }
    override fun openManual(): LaunchResult = open("camera")
}
