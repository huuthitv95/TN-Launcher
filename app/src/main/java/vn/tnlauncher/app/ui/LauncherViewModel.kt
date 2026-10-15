package vn.tnlauncher.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.tnlauncher.app.data.*
import vn.tnlauncher.app.integration.*

data class LauncherState(
    val apps: List<LauncherApp> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val capabilities: List<Capability> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val systemCapabilities: List<Capability> = emptyList(),
)
class LauncherViewModel(private val apps: LauncherAppRepository, private val settings: LauncherSettingsRepository,
    private val vendor: WincavnIntegration, private val wallpapers: WallpaperImporter? = null,
    private val system: SystemSettingsIntegration? = null, private val appManagement: AppManagement? = null) : ViewModel() {
    private val mutableState = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = mutableState.asStateFlow()
    private var refreshJob: Job? = null
    init {
        viewModelScope.launch {
            settings.settings.collect { value -> mutableState.update { it.copy(settings = value) } }
        }
        refresh()
    }
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = it.apps.isEmpty(), error = null) }
            try {
                val loaded = apps.load()
                val capabilities = withContext(Dispatchers.IO) { vendor.inspect() }
                val systemCapabilities = withContext(Dispatchers.IO) { system?.inspect().orEmpty() }
                mutableState.update { it.copy(apps = loaded, capabilities = capabilities, systemCapabilities = systemCapabilities, loading = false) }
            } catch (error: kotlinx.coroutines.CancellationException) { throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = "Không đọc được danh sách ứng dụng. Hãy thử lại.") }
            }
        }
    }
    fun orderedApps(): List<LauncherApp> {
        val current = state.value
        val byId = current.apps.associateBy { it.id } + current.settings.folders.associate { it.id to LauncherApp(it.id, it.title, null) }
        return FolderRules.visibleIds(current.apps.map { it.id }, current.settings).mapNotNull(byId::get)
    }
    fun change(onFailure: (() -> Unit)? = null, transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch {
            try {
                settings.update(transform)
                mutableState.update { if (it.error?.startsWith("Không lưu được") == true) it.copy(error = null) else it }
            }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                onFailure?.invoke()
                mutableState.update { it.copy(error = "Không lưu được cài đặt. Dữ liệu cũ được giữ; hãy thử lại.") }
            }
        }
    }
    fun pin(id: String) = change { it.copy(favorites = LauncherRules.pin(it.favorites, id)) }
    fun importWallpaper(uri: android.net.Uri) {
        val importer = wallpapers ?: return
        viewModelScope.launch {
            try {
                val name = importer.importImage(uri)
                // Once imported, finish the transaction even if the Activity is recreated/closed.
                withContext(kotlinx.coroutines.NonCancellable) {
                    try {
                        settings.update { it.copy(wallpaper = name, systemWallpaper = false) }
                        mutableState.update { it.copy(error = null) }
                    } catch (error: Exception) {
                        withContext(Dispatchers.IO) { importer.discard(name) }
                        throw error
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(error = "Không đọc/lưu được ảnh hoặc ảnh lớn hơn 32 MB. Hình nền cũ được giữ.") } }
        }
    }
    fun unpin(id: String) = change { it.copy(favorites = it.favorites - id) }
    fun move(id: String, delta: Int, dock: Boolean) = change { old ->
        if (dock) old.copy(favorites = LauncherRules.move(old.favorites, id, delta))
        else {
            val visible = orderedApps().map { it.id }
            val all = old.homeOrder + visible.filter { it !in old.homeOrder }
            old.copy(homeOrder = LauncherRules.reorderSubset(all, LauncherRules.move(visible, id, delta)))
        }
    }
    fun reorder(ids: List<String>, dock: Boolean) = change { old ->
        if (dock) old.copy(favorites = ids.distinct().take(LauncherRules.MAX_FAVORITES))
        else {
            val all = old.homeOrder + orderedApps().map { it.id }.filter { it !in old.homeOrder }
            old.copy(homeOrder = LauncherRules.reorderSubset(all, ids))
        }
    }
    fun launch(id: String): LaunchResult = apps.launch(id)
    fun openVendor(id: String): LaunchResult = vendor.open(id)
    fun openSystem(id: String, query: String? = null): LaunchResult = system?.open(id, query) ?: LaunchResult.Missing
    fun inspectApp(id: String) = appManagement?.inspect(id) ?: AppManagementCapability(false, false)
    fun appDetails(id: String) = appManagement?.details(id) ?: LaunchResult.Missing
    fun requestUninstall(id: String) = appManagement?.requestUninstall(id) ?: LaunchResult.Missing
    fun saveFolder(folder: LauncherFolder) = change { old ->
        val folders = FolderRules.assign(old.folders, folder)
        old.copy(folders = folders, homeOrder = (old.homeOrder + folder.id).distinct())
    }
    fun deleteFolder(id: String) = change { old -> old.copy(folders = old.folders.filterNot { it.id == id }, homeOrder = old.homeOrder - id) }
}
