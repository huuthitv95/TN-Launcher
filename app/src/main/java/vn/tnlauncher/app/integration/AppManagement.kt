package vn.tnlauncher.app.integration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings

data class AppManagementCapability(val details: Boolean, val uninstall: Boolean)
interface AppManagement {
    fun inspect(id: String): AppManagementCapability
    fun details(id: String): LaunchResult
    fun requestUninstall(id: String): LaunchResult
}

object AppManagementRules {
    fun mayUninstall(self: String, pkg: String, system: Boolean): Boolean = !system && pkg != self &&
        !pkg.startsWith("com.syu.") && !pkg.startsWith("com.fyt.") && !pkg.startsWith("com.android.launcher")
}

class AndroidAppManagement(context: Context) : AppManagement {
    private val context = context.applicationContext
    private val launcher = SafeActivityLauncher(this.context)
    private fun packageOf(id: String) = ComponentName.unflattenFromString(id)?.packageName
    override fun inspect(id: String): AppManagementCapability {
        val pkg = packageOf(id) ?: return AppManagementCapability(false, false)
        return try {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            val system = info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            AppManagementCapability(true, AppManagementRules.mayUninstall(context.packageName, pkg, system))
        } catch (_: RuntimeException) { AppManagementCapability(false, false) }
        catch (_: android.content.pm.PackageManager.NameNotFoundException) { AppManagementCapability(false, false) }
    }
    override fun details(id: String): LaunchResult {
        val pkg = packageOf(id) ?: return LaunchResult.Missing
        if (!inspect(id).details) return LaunchResult.Missing
        return launcher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)))
    }
    override fun requestUninstall(id: String): LaunchResult {
        val pkg = packageOf(id) ?: return LaunchResult.Missing
        val capability = inspect(id)
        if (!capability.details) return LaunchResult.Missing
        if (!capability.uninstall) return LaunchResult.Denied
        // The system confirmation screen owns the deletion. Never call PackageInstaller.delete/uninstall.
        return launcher.launch(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null)))
    }
}
