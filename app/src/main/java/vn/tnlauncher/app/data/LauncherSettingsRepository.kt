package vn.tnlauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

interface LauncherSettingsRepository {
    val settings: Flow<LauncherSettings>
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings)
}

class DataStoreLauncherSettingsRepository(context: Context, storeName: String = "launcher",
    scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())) : LauncherSettingsRepository {
    companion object {
        val HOME = stringPreferencesKey("home_order")
        val DOCK = stringPreferencesKey("favorites")
        val THEME = stringPreferencesKey("theme")
        val WALLPAPER = stringPreferencesKey("wallpaper")
        val MOTION = booleanPreferencesKey("reduced_motion")
        val NOTICE = stringPreferencesKey("recovery_notice")
        val FOLDERS = stringPreferencesKey("folders")
        val SYSTEM_WALLPAPER = booleanPreferencesKey("system_wallpaper")
    }
    private val source = context.preferencesDataStoreFile(storeName)
    private val recoveryDir = File(context.filesDir, "recovery")
    private val filesDir = context.filesDir
    private val updates = Mutex()
    @Volatile
    private var readFailed = false
    private fun preserveOriginal(): File {
        if (!recoveryDir.isDirectory && !recoveryDir.mkdirs()) throw IOException("Không tạo được thư mục phục hồi.")
        val backup = File.createTempFile("launcher-", ".preferences_pb", recoveryDir)
        // Do not replace the settings file unless its original bytes were preserved.
        return source.copyTo(backup, overwrite = true)
    }
    private fun validWallpaper(name: String): Boolean = name.matches(Regex("wallpaper-[A-Za-z0-9_-]+\\.jpg"))
    private fun hasInvalidValues(prefs: Preferences): Boolean {
        val values = prefs.asMap()
        fun invalidIds(key: Preferences.Key<String>, limit: Int = Int.MAX_VALUE): Boolean {
            val value = values[key] ?: return false
            if (value !is String) return true
            return value != (if (key == HOME) LauncherRules.validHomeIds(value) else LauncherRules.validIds(value)).take(limit).joinToString("\n")
        }
        return invalidIds(HOME) || invalidIds(DOCK, LauncherRules.MAX_FAVORITES) ||
            values[THEME]?.let { it !is String || ThemeMode.entries.none { mode -> mode.name == it } } == true ||
            values[WALLPAPER]?.let { it !is String || !validWallpaper(it) } == true ||
            values[MOTION]?.let { it !is Boolean } == true ||
            values[NOTICE]?.let { it !is String } == true
            || values[FOLDERS]?.let { it !is String || FolderCodec.encode(FolderCodec.decode(it)) != it } == true
            || values[SYSTEM_WALLPAPER]?.let { it !is Boolean } == true
    }
    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler {
            preserveOriginal()
            androidx.datastore.preferences.core.mutablePreferencesOf(
                NOTICE to "Dữ liệu lỗi đã được giữ trong bộ nhớ riêng; bố cục mặc định được phục hồi."
            )
        },
        migrations = listOf(object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) = hasInvalidValues(currentData)
            override suspend fun migrate(currentData: Preferences): Preferences {
                preserveOriginal()
                return currentData.toMutablePreferences().apply {
                    write(decode(currentData).copy(recoveryNotice = "Cài đặt không hợp lệ đã được phục hồi; dữ liệu gốc được giữ trong bộ nhớ riêng."))
                }
            }
            override suspend fun cleanUp() = Unit
        }),
        produceFile = { source }, scope = scope,
    )
    private fun decode(p: Preferences): LauncherSettings {
        // A protobuf can be readable while a value has the wrong preference type.
        val values = p.asMap()
        return LauncherSettings(
            homeOrder = LauncherRules.validHomeIds((values[HOME] as? String).orEmpty()),
            favorites = LauncherRules.validIds((values[DOCK] as? String).orEmpty()).take(LauncherRules.MAX_FAVORITES),
            theme = ThemeMode.entries.firstOrNull { it.name == values[THEME] } ?: ThemeMode.SYSTEM,
            wallpaper = (values[WALLPAPER] as? String)?.takeIf(::validWallpaper),
            reducedMotion = values[MOTION] as? Boolean ?: false,
            recoveryNotice = values[NOTICE] as? String,
            folders = FolderCodec.decode((values[FOLDERS] as? String).orEmpty()),
            systemWallpaper = values[SYSTEM_WALLPAPER] as? Boolean ?: false,
        )
    }
    private fun MutablePreferences.write(value: LauncherSettings) {
        this[HOME] = LauncherRules.validHomeIds(value.homeOrder.joinToString("\n")).joinToString("\n")
        this[DOCK] = LauncherRules.validIds(value.favorites.joinToString("\n")).take(LauncherRules.MAX_FAVORITES).joinToString("\n")
        this[THEME] = value.theme.name
        this[MOTION] = value.reducedMotion
        this[FOLDERS] = FolderCodec.encode(value.folders)
        this[SYSTEM_WALLPAPER] = value.systemWallpaper
        if (value.wallpaper == null) remove(WALLPAPER) else {
            require(validWallpaper(value.wallpaper)) { "Tên hình nền không hợp lệ." }
            this[WALLPAPER] = value.wallpaper
        }
        if (value.recoveryNotice == null) remove(NOTICE) else this[NOTICE] = value.recoveryNotice
    }
    override val settings = store.data.catch { error ->
        if (error !is IOException) throw error
        readFailed = true
        emit(androidx.datastore.preferences.core.mutablePreferencesOf(NOTICE to "Không đọc được cài đặt. Dữ liệu gốc được giữ; hãy khởi động lại ứng dụng."))
    }.map(::decode)
    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) = updates.withLock {
        check(!readFailed) { "Không thể ghi khi cài đặt chưa đọc được." }
        var oldWallpaper: String? = null
        var newWallpaper: String? = null
        store.edit { prefs ->
            val previous = decode(prefs)
            val value = transform(previous)
            prefs.write(value)
            oldWallpaper = previous.wallpaper
            newWallpaper = value.wallpaper
        }
        // Only the previously referenced, app-generated file is eligible. Never sweep filesDir:
        // another image may be in the middle of import, and recovery/user files must stay intact.
        val obsoleteWallpaper = oldWallpaper?.takeIf { it != newWallpaper && it.matches(Regex("wallpaper-[0-9]+\\.jpg")) }
        if (obsoleteWallpaper != null) {
            // Finishing post-commit cleanup must not make a committed update look unsuccessful.
            withContext(NonCancellable) {
                withContext(Dispatchers.IO) {
                    try {
                        val previous = File(filesDir, obsoleteWallpaper)
                        if (previous.canonicalFile.parentFile == filesDir.canonicalFile && previous.isFile) previous.delete()
                    } catch (_: IOException) { /* A cleanup failure must not invalidate committed settings. */ }
                    catch (_: SecurityException) { /* Preserve the file when access is uncertain. */ }
                }
            }
        }
    }
}
