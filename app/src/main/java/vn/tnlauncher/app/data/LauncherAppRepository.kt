package vn.tnlauncher.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import vn.tnlauncher.app.integration.Availability
import vn.tnlauncher.app.integration.LaunchResult
import vn.tnlauncher.app.integration.SafeActivityLauncher
import vn.tnlauncher.app.R

data class LauncherApp(val id: String, val label: String, val icon: Drawable?)
interface LauncherAppRepository {
    suspend fun load(): List<LauncherApp>
    fun launch(id: String): LaunchResult
}

class AndroidLauncherAppRepository(context: Context) : LauncherAppRepository {
    private val context = context.applicationContext
    private val launcher = SafeActivityLauncher(this.context)
    private data class AppToken(val updatedAt: Long, val sourceDir: String?, val uid: Int,
        val configuration: Configuration)
    private data class CachedApp(val token: AppToken, val app: LauncherApp)
    private val loading = Mutex()
    private var cache = emptyMap<String, CachedApp>()

    override suspend fun load(): List<LauncherApp> = withContext(Dispatchers.IO) {
        loading.withLock {
            val pm = context.packageManager
            // A copy includes locale, density and UI mode without retaining mutable Resources state.
            val configuration = Configuration(context.resources.configuration)
            val updatedPackages = mutableMapOf<String, Long?>()
            val nextCache = mutableMapOf<String, CachedApp>()
            val loaded = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .mapNotNull { result ->
                    coroutineContext.ensureActive()
                    val info = result.activityInfo ?: return@mapNotNull null
                    val component = ComponentName(info.packageName, info.name)
                    // Cached resources never replace current availability/permission checks.
                    if (info.packageName == context.packageName || launcher.inspect(component) != Availability.AVAILABLE) return@mapNotNull null
                    val id = component.flattenToString()
                    if (!updatedPackages.containsKey(info.packageName)) {
                        updatedPackages[info.packageName] = try {
                            pm.getPackageInfo(info.packageName, 0).lastUpdateTime
                        } catch (_: android.content.pm.PackageManager.NameNotFoundException) { null }
                        catch (_: RuntimeException) { null }
                    }
                    val token = updatedPackages[info.packageName]?.let {
                        AppToken(it, info.applicationInfo.sourceDir, info.applicationInfo.uid, configuration)
                    }
                    cache[id]?.takeIf { token != null && it.token == token }?.let { cached ->
                        nextCache[id] = cached
                        return@mapNotNull cached.app
                    }
                    val label = try { result.loadLabel(pm)?.toString()?.takeIf(String::isNotBlank) }
                        catch (_: RuntimeException) { null }
                    val icon = try {
                        if (info.packageName == "com.android.settings") context.getDrawable(R.drawable.mactahoe_settings)
                        else result.loadIcon(pm)
                    } catch (_: RuntimeException) { null }
                    val app = LauncherApp(id, label ?: info.packageName, icon)
                    // Retry a transient metadata/resource failure on the next refresh.
                    if (token != null && label != null && icon != null) nextCache[id] = CachedApp(token, app)
                    app
                }.distinctBy { it.id }
                .sortedWith(compareBy<LauncherApp> { LauncherRules.searchKey(it.label) }.thenBy { it.id })
            coroutineContext.ensureActive()
            // Drop removed/disabled entries; publish only after a complete, uncancelled load.
            cache = nextCache
            loaded
        }
    }
    override fun launch(id: String): LaunchResult {
        val component = ComponentName.unflattenFromString(id) ?: return LaunchResult.Missing
        return launcher.launch(component)
    }
}
