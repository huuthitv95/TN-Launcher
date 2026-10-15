package vn.tnlauncher.app

import android.app.Application
import vn.tnlauncher.app.data.AndroidLauncherAppRepository
import vn.tnlauncher.app.data.DataStoreLauncherSettingsRepository
import vn.tnlauncher.app.integration.AndroidWincavnIntegration
import vn.tnlauncher.app.data.WallpaperImporter
import vn.tnlauncher.app.integration.AndroidSystemSettingsIntegration
import vn.tnlauncher.app.integration.AndroidAppManagement

class LauncherApplication : Application() {
    val apps by lazy { AndroidLauncherAppRepository(this) }
    val settings by lazy { DataStoreLauncherSettingsRepository(this) }
    val wincavn by lazy { AndroidWincavnIntegration(this) }
    val wallpapers by lazy { WallpaperImporter(this) }
    val systemSettings by lazy { AndroidSystemSettingsIntegration(this) }
    val appManagement by lazy { AndroidAppManagement(this) }
}
