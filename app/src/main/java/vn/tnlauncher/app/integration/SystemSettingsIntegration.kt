package vn.tnlauncher.app.integration

import android.content.Context
import android.content.Intent
import android.provider.Settings

interface SystemSettingsIntegration : DeviceCapabilities {
    fun open(id: String, query: String? = null): LaunchResult
}

class AndroidSystemSettingsIntegration(context: Context) : SystemSettingsIntegration {
    private val launcher = SafeActivityLauncher(context.applicationContext)
    private data class Entry(val id: String, val title: String, val action: String)
    private val entries = listOf(
        Entry("wifi", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
        Entry("date", "Ngày và giờ", Settings.ACTION_DATE_SETTINGS),
        Entry("global_search", "Tìm kiếm hệ thống", android.app.SearchManager.INTENT_ACTION_GLOBAL_SEARCH),
        Entry("live_wallpaper", "Hình nền động Android", android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
    )

    override fun inspect(): List<Capability> = entries.map { entry ->
        val target = launcher.resolve(Intent(entry.action))
        Capability(entry.id, entry.title, target.availability, target.component?.flattenToString(),
            "Mở trang cài đặt Android; TN Launcher không tự thay đổi thiết lập.")
    }

    override fun open(id: String, query: String?): LaunchResult = entries.firstOrNull { it.id == id }
        ?.let { entry -> launcher.launch(Intent(entry.action).apply {
            if (id == "global_search" && !query.isNullOrBlank()) {
                putExtra(android.app.SearchManager.QUERY, query)
                putExtra(android.app.SearchManager.EXTRA_SELECT_QUERY, true)
            }
        }) } ?: LaunchResult.Missing
}
