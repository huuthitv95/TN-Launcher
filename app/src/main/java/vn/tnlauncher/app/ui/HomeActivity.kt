package vn.tnlauncher.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.tnlauncher.app.BuildConfig
import vn.tnlauncher.app.LauncherApplication
import vn.tnlauncher.app.R
import vn.tnlauncher.app.data.*
import vn.tnlauncher.app.databinding.ActivityHomeBinding
import vn.tnlauncher.app.integration.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var model: LauncherViewModel
    private lateinit var gridAdapter: AppAdapter
    private lateinit var dockAdapter: AppAdapter
    private val typography by lazy { LauncherTypography(this) }
    private val folderIcon by lazy { androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.ic_folder) }
    private var screen = "home"
    private var page = 0
    private var capacity = 18
    private var columns = 6
    private var foreground = Color.WHITE
    private var surface = Color.DKGRAY
    private var appliedDark: Boolean? = null
    private var appliedSystemWallpaper: Boolean? = null
    private var lastWallpaper: String? = "initial"
    private var wallpaperJob: kotlinx.coroutines.Job? = null
    private var popup: AlertDialog? = null
    private data class SettingsContent(val settings: LauncherSettings, val apps: List<Pair<String, String>>,
        val capabilities: List<Capability>, val systemCapabilities: List<Capability>, val error: String?, val foreground: Int)
    private var settingsContent: SettingsContent? = null
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { model.refresh() }
    }
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { updateClock() }
    }
    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.importWallpaper(uri)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.clock.typeface = typography.heading
        listOf(binding.pageLabel, binding.message, binding.search).forEach { it.typeface = typography.body }
        val app = application as LauncherApplication
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LauncherViewModel(app.apps, app.settings, app.wincavn, app.wallpapers, app.systemSettings, app.appManagement) as T
        })[LauncherViewModel::class.java]
        screen = savedInstanceState?.getString("screen") ?: "home"
        page = savedInstanceState?.getInt("page") ?: 0
        gridAdapter = AppAdapter(false, { item ->
            val folder = model.state.value.settings.folders.firstOrNull { it.id == item.id }
            if (folder != null) showFolder(folder) else showLaunch(model.launch(item.id))
        }, { appItem ->
            val folder = model.state.value.settings.folders.firstOrNull { it.id == appItem.id }
            if (folder != null) folderMenu(folder) else appMenu(appItem, false)
        }, { model.reorder(it, false) }, typography)
        dockAdapter = AppAdapter(true, { appItem ->
            if (appItem.icon == null && model.state.value.apps.none { it.id == appItem.id }) toast("Ứng dụng hiện không có trên máy. Nhấn giữ để bỏ ghim.")
            else showLaunch(model.launch(appItem.id))
        }, { appItem -> appMenu(appItem, true) }, { model.reorder(it, true) }, typography)
        binding.grid.layoutManager = GridLayoutManager(this, columns)
        binding.grid.adapter = gridAdapter; gridAdapter.attachDrag(binding.grid)
        binding.favorites.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.favorites.adapter = dockAdapter; dockAdapter.attachDrag(binding.favorites)
        binding.appsButton.setOnClickListener { showScreen("drawer") }
        binding.settingsButton.setOnClickListener { showScreen("settings") }
        binding.homeButton.setOnClickListener { resetHome() }
        binding.retry.setOnClickListener { model.refresh() }
        binding.previous.setOnClickListener { page--; render(model.state.value) }
        binding.next.setOnClickListener { page++; render(model.state.value) }
        binding.search.setText(savedInstanceState?.getString("query").orEmpty())
        binding.search.doAfterTextChanged { if (screen == "drawer") render(model.state.value) }
        binding.search.setOnEditorActionListener { _, _, _ -> hideKeyboard(); true }
        binding.root.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) binding.grid.post { updateGeometry() }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(system.left, system.top, system.right, system.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (ViewCompat.getRootWindowInsets(binding.root)?.isVisible(WindowInsetsCompat.Type.ime()) == true) hideKeyboard()
                else if (screen == "drawer" && !binding.search.text.isNullOrEmpty()) binding.search.setText("")
                else resetHome()
            }
        })
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { model.state.collect(::render) } }
        updateClock()
    }
    override fun onStart() {
        super.onStart()
        updateClock()
        ViewCompat.requestApplyInsets(binding.root)
        androidx.core.content.ContextCompat.registerReceiver(this, packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package")
            }, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
        androidx.core.content.ContextCompat.registerReceiver(this, timeReceiver,
            IntentFilter().apply { addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED) },
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        model.refresh()
    }
    override fun onStop() {
        popup?.dismiss(); popup = null
        unregisterReceiver(packageReceiver); unregisterReceiver(timeReceiver)
        super.onStop()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); resetHome() }
    override fun onSearchRequested(): Boolean {
        if (!::model.isInitialized) return false
        val result = model.openSystem("global_search")
        showLaunch(result)
        return result == LaunchResult.Opened
    }
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        val character = event.unicodeChar
        if (screen == "home" && popup?.isShowing != true && event.repeatCount == 0 &&
            !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed &&
            Character.isValidCodePoint(character) && character > 0 && !Character.isISOControl(character) && !Character.isWhitespace(character)) {
            val query = String(Character.toChars(character))
            if (model.openSystem("global_search", query) != LaunchResult.Opened) {
                showScreen("drawer"); binding.search.setText(query); binding.search.setSelection(query.length); binding.search.requestFocus()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen", screen); outState.putInt("page", page); outState.putString("query", binding.search.text.toString())
        super.onSaveInstanceState(outState)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun hideKeyboard() { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.root.windowToken, 0); binding.search.clearFocus() }
    private fun showScreen(value: String) { hideKeyboard(); screen = value; render(model.state.value); binding.grid.scrollToPosition(0) }
    private fun resetHome() { popup?.dismiss(); binding.search.setText(""); page = 0; showScreen("home") }
    private fun updateClock() { binding.clock.text = SimpleDateFormat("HH:mm · E, dd/MM", Locale.forLanguageTag("vi-VN")).format(Date()) }
    private fun updateGeometry() {
        val scale = maxOf(1f, resources.configuration.fontScale)
        val newColumns = (binding.grid.width / dp((96 * scale).toInt())).coerceIn(1, 8)
        val rows = ((binding.grid.height - binding.grid.paddingTop - binding.grid.paddingBottom) / dp((112 * scale).toInt())).coerceAtLeast(1)
        if (newColumns != columns || capacity != rows * newColumns) {
            columns = newColumns; capacity = rows * columns
            (binding.grid.layoutManager as GridLayoutManager).spanCount = columns
            render(model.state.value)
        }
    }
    private fun panelBackground(radius: Int = 24) = GradientDrawable().apply { setColor(surface); cornerRadius = dp(radius).toFloat() }
    private fun render(state: LauncherState) {
        val dark = when (state.settings.theme) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        if (appliedDark != dark || appliedSystemWallpaper != state.settings.systemWallpaper) {
            appliedDark = dark
            appliedSystemWallpaper = state.settings.systemWallpaper
            foreground = Color.parseColor(if (dark) "#F5F7FA" else "#142033")
            surface = Color.parseColor(if (dark) "#EE233047" else "#EEE9EFF8")
            binding.root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                if (dark) intArrayOf(Color.parseColor("#111A2B"), Color.parseColor("#30455D")) else intArrayOf(Color.parseColor("#F1F5FA"), Color.parseColor("#B8CBDF"))).apply {
                alpha = if (state.settings.systemWallpaper) 215 else 255
            }
            if (state.settings.systemWallpaper) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            binding.wallpaper.visibility = if (state.settings.systemWallpaper) View.GONE else View.VISIBLE
            binding.wallpaper.alpha = if (dark) .22f else .16f
            window.statusBarColor = if (dark) Color.parseColor("#111A2B") else Color.parseColor("#F1F5FA")
            window.navigationBarColor = window.statusBarColor
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            listOf(binding.clock, binding.title, binding.pageLabel, binding.message, binding.search).forEach { it.setTextColor(foreground) }
            binding.search.setHintTextColor(if (dark) Color.LTGRAY else Color.DKGRAY)
            binding.search.background = panelBackground(16)
            binding.dock.background = panelBackground(30)
            listOf(binding.homeButton, binding.previous, binding.next, binding.retry, binding.appsButton, binding.settingsButton).forEach(::styleButton)
        }
        listOf(binding.grid, binding.favorites).forEach { recycler ->
            if (state.settings.reducedMotion && recycler.itemAnimator != null) recycler.itemAnimator = null
            else if (!state.settings.reducedMotion && recycler.itemAnimator == null) recycler.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }
        val dockHeight = dp((112 * maxOf(1f, resources.configuration.fontScale)).toInt())
        if (binding.dockScroll.layoutParams.height != dockHeight) binding.dockScroll.layoutParams = binding.dockScroll.layoutParams.apply { height = dockHeight }
        val fontScale = maxOf(1f, resources.configuration.fontScale)
        listOf(binding.appsButton, binding.settingsButton).forEach { button ->
            val width = dp((96 * fontScale).toInt()); val height = dp((72 * fontScale).toInt())
            if (button.layoutParams.width != width || button.layoutParams.height != height) {
                button.layoutParams = button.layoutParams.apply { this.width = width; this.height = height }
            }
        }
        if (lastWallpaper != state.settings.wallpaper) {
            lastWallpaper = state.settings.wallpaper
            wallpaperJob?.cancel()
            val name = state.settings.wallpaper
            if (name == null) binding.wallpaper.setImageDrawable(null)
            else wallpaperJob = lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    // Accept only an app-owned basename, never an external path from preferences.
                    if (File(name).name != name || !name.startsWith("wallpaper-")) null else BitmapFactory.decodeFile(File(filesDir, name).path)
                }
                binding.wallpaper.setImageBitmap(bitmap)
                if (bitmap == null) toast("Hình nền không còn đọc được. Đang dùng nền mặc định.")
            }
        }
        binding.mainContent.visibility = if (screen == "settings") View.GONE else View.VISIBLE
        binding.settingsPanel.visibility = if (screen == "settings") View.VISIBLE else View.GONE
        if (screen == "settings") { renderSettings(state); return }
        binding.title.setText(if (screen == "drawer") R.string.apps else R.string.app_name)
        binding.title.typeface = if (screen == "home") typography.brand else typography.heading
        binding.search.visibility = if (screen == "drawer") View.VISIBLE else View.GONE
        binding.paging.visibility = if (screen == "home") View.VISIBLE else View.GONE
        val all = if (screen == "drawer") state.apps.filter {
            LauncherRules.searchKey(it.label).contains(LauncherRules.searchKey(binding.search.text.toString().trim()))
        } else model.orderedApps()
        val pages = maxOf(1, (all.size + capacity - 1) / capacity)
        page = page.coerceIn(0, pages - 1)
        gridAdapter.draggingEnabled = screen == "home"
        val displayed = if (screen == "home") all.drop(page * capacity).take(capacity).map {
            if (FolderRules.isFolderId(it.id)) it.copy(icon = folderIcon) else it
        } else all
        gridAdapter.submit(displayed, foreground)
        val byId = state.apps.associateBy { it.id }
        dockAdapter.submit(state.settings.favorites.map { id -> byId[id] ?: LauncherApp(id, "Không có trên máy", null) }, foreground)
        binding.pageLabel.text = "${page + 1} / $pages"
        binding.previous.isEnabled = page > 0; binding.next.isEnabled = page < pages - 1
        val notice = state.error ?: state.settings.recoveryNotice ?: when {
            state.loading && state.apps.isEmpty() -> getString(R.string.loading)
            all.isEmpty() -> getString(if (screen == "drawer" && binding.search.text.isNotEmpty()) R.string.empty_search else R.string.empty_apps)
            else -> null
        }
        binding.message.text = notice
        binding.message.visibility = if (notice == null) View.GONE else View.VISIBLE
        binding.retry.visibility = if (state.error != null || (!state.loading && state.apps.isEmpty())) View.VISIBLE else View.GONE
        binding.grid.post { updateGeometry() }
    }
    private fun renderSettings(state: LauncherState) {
        val contentState = SettingsContent(state.settings, state.apps.map { it.id to it.label }, state.capabilities, state.systemCapabilities, state.error, foreground)
        if (settingsContent == contentState) return
        settingsContent = contentState
        val content = binding.settingsContent
        content.removeAllViews(); content.setBackgroundColor(if (foreground == Color.parseColor("#F5F7FA")) Color.parseColor("#111A2B") else Color.parseColor("#F1F5FA"))
        fun text(value: String, size: Float = 18f) = TextView(this).apply {
            text = value; textSize = size
            typeface = if (size >= 22f) typography.heading else typography.body
            setTextColor(this@HomeActivity.foreground); setPadding(0, dp(12), 0, dp(8)); content.addView(this)
        }
        fun button(value: String, action: () -> Unit) = Button(this).apply {
            text = value; isAllCaps = false; textSize = 18f; minHeight = dp(56)
            content.addView(this, LinearLayout.LayoutParams(-1, -2)); setOnClickListener { action() }
            styleButton(this)
        }
        text("Cài đặt TN Launcher", 28f)
        button("Về màn hình chính") { resetHome() }
        state.error?.let { text(it) }
        state.settings.recoveryNotice?.let { text(it); button("Đã hiểu thông báo phục hồi") { model.change { it.copy(recoveryNotice = null) } } }
        text("Giao diện", 22f)
        val modes = listOf("Theo hệ thống" to ThemeMode.SYSTEM, "Sáng" to ThemeMode.LIGHT, "Tối" to ThemeMode.DARK)
        val group = RadioGroup(this)
        modes.forEach { (label, mode) ->
            group.addView(RadioButton(this).apply {
                id = View.generateViewId(); text = label; textSize = 18f; minHeight = dp(56); setTextColor(this@HomeActivity.foreground)
                typeface = typography.body
                isChecked = state.settings.theme == mode
                setOnClickListener { model.change { it.copy(theme = mode) } }
            })
        }
        content.addView(group)
        content.addView(Switch(this).apply {
            text = "Giảm chuyển động"; textSize = 18f; minHeight = dp(56); setTextColor(this@HomeActivity.foreground)
            typeface = typography.body
            isChecked = state.settings.reducedMotion
            setOnCheckedChangeListener { _, checked -> model.change { it.copy(reducedMotion = checked) } }
        })
        button("Chọn hình nền riêng") { wallpaperPicker.launch(arrayOf("image/*")) }
        button("Dùng hình nền mặc định") { model.change { it.copy(wallpaper = null, systemWallpaper = false) } }
        button("Dùng hình nền hệ thống") { model.change { it.copy(systemWallpaper = true) } }
        text("Bố cục và Dock", 22f)
        text("Home hiển thị ứng dụng thật. Giữ rồi kéo để đổi thứ tự; nhấn giữ để mở menu di chuyển/ghim. Dock chứa tối đa 6 ứng dụng, Apps và Cài đặt luôn có sẵn.")
        button("Sắp xếp Home theo tên") { model.change { it.copy(homeOrder = emptyList()) } }
        button("Tạo thư mục") { editFolder(null) }
        button("Tiện ích Android") { startActivity(Intent(this, WidgetsActivity::class.java)) }
        if (state.settings.favorites.isEmpty()) text("Chưa ghim ứng dụng vào Dock.")
        state.settings.favorites.forEach { id ->
            val app = state.apps.firstOrNull { it.id == id } ?: LauncherApp(id, "Ứng dụng không có trên máy", null)
            button("Chỉnh Dock: ${app.label}") { appMenu(app, true) }
        }
        text("Cài đặt Android", 22f)
        state.systemCapabilities.forEach { capability ->
            if (capability.id == "live_wallpaper") text("Chọn hình nền trong Android, sau đó dùng nút Dùng hình nền hệ thống để hiển thị trên Home.", 16f)
            if (capability.availability == Availability.AVAILABLE) button("Mở ${capability.title}") {
                val result = model.openSystem(capability.id)
                showLaunch(result)
            }
            else text("${capability.title}: không có màn hình cài đặt khả dụng trên thiết bị.")
        }
        text("Tích hợp Wincavn", 22f)
        text("Chỉ mở ứng dụng gốc theo thao tác của bạn. Camera tự bật khi lùi, overlay và tương thích xe chưa được kiểm chứng.")
        state.capabilities.forEach { capability ->
            val status = when (capability.availability) {
                Availability.AVAILABLE -> "Có entry point"
                Availability.PACKAGE_MISSING -> "Chưa cài"
                Availability.COMPONENT_UNAVAILABLE -> "Component không khả dụng"
                Availability.PERMISSION_REQUIRED -> "Cần quyền vendor"
                Availability.UNVERIFIED -> "Chưa xác minh"
            }
            text("${capability.title}: $status\n${capability.note}")
            if (capability.availability == Availability.AVAILABLE) button("Mở ${capability.title}") { showLaunch(model.openVendor(capability.id)) }
        }
        button("Kiểm tra lại ứng dụng và tích hợp") { model.refresh() }
        text("TN Launcher ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n${BuildConfig.APPLICATION_ID}\nAndroid ${android.os.Build.VERSION.RELEASE} · API ${android.os.Build.VERSION.SDK_INT}\nKhông yêu cầu quyền hệ thống, camera hoặc overlay.")
        text("Tài nguyên: biểu tượng TN do người dùng cung cấp; icon ứng dụng lấy từ package đã cài. Icon Android Settings: MacTahoe / Vince Liuice, GPL-3.0; source và giấy phép đi kèm APK. Không đóng gói Apple UI Kit.", 16f)
    }
    private fun appMenu(app: LauncherApp, dock: Boolean) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) || !binding.root.hasWindowFocus()) return
        val pinned = app.id in model.state.value.settings.favorites
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        actions += "Mở ứng dụng" to { showLaunch(model.launch(app.id)) }
        val management = model.inspectApp(app.id)
        if (management.details) actions += "Thông tin ứng dụng" to { showLaunch(model.appDetails(app.id)) }
        if (management.uninstall) actions += "Gỡ ứng dụng…" to { showLaunch(model.requestUninstall(app.id)) }
        if (pinned) actions += "Bỏ ghim khỏi Dock" to { model.unpin(app.id) }
        else if (model.state.value.settings.favorites.size < LauncherRules.MAX_FAVORITES) actions += "Ghim vào Dock" to { model.pin(app.id) }
        if (screen != "drawer" || dock) {
            actions += "Di chuyển trước" to { model.move(app.id, -1, dock) }
            actions += "Di chuyển sau" to { model.move(app.id, 1, dock) }
        }
        if (model.state.value.settings.folders.size < FolderRules.MAX_FOLDERS) actions += "Tạo thư mục với ứng dụng này" to { editFolder(null, app.id) }
        val containingFolder = model.state.value.settings.folders.firstOrNull { app.id in it.appIds }
        if (containingFolder != null) actions += "Đưa ra khỏi thư mục" to {
            model.saveFolder(containingFolder.copy(appIds = containingFolder.appIds - app.id))
        }
        popup?.dismiss()
        val dialogContext = android.view.ContextThemeWrapper(this, if (foreground == Color.parseColor("#F5F7FA")) R.style.Theme_TNLauncher_Dialog_Dark else R.style.Theme_TNLauncher_Dialog_Light)
        popup = AlertDialog.Builder(dialogContext).setTitle(app.label).setItems(actions.map { it.first }.toTypedArray()) { _, index -> actions[index].second() }
            .setNegativeButton("Đóng", null).showLauncherDialog()
    }
    private fun showFolder(folder: LauncherFolder) {
        val apps = model.state.value.apps.associateBy { it.id }
        val labels = folder.appIds.map { apps[it]?.label ?: "Ứng dụng không có trên máy" }
        popup?.dismiss()
        popup = AlertDialog.Builder(dialogContext()).setTitle(folder.title)
            .setItems(labels.toTypedArray()) { _, index -> showLaunch(model.launch(folder.appIds[index])) }
            .setMessage(if (labels.isEmpty()) "Thư mục trống. Chọn Chỉnh sửa để thêm ứng dụng." else null)
            .setNeutralButton("Chỉnh sửa") { _, _ -> editFolder(folder) }
            .setNegativeButton("Đóng", null).showLauncherDialog()
    }
    private fun folderMenu(folder: LauncherFolder) {
        val actions = listOf<Pair<String, () -> Unit>>(
            "Mở thư mục" to { showFolder(folder) },
            "Chỉnh sửa thư mục" to { editFolder(folder) },
            "Di chuyển trước" to { model.move(folder.id, -1, false) },
            "Di chuyển sau" to { model.move(folder.id, 1, false) },
            "Xóa thư mục · đưa ứng dụng về Home" to { model.deleteFolder(folder.id) },
        )
        popup?.dismiss()
        popup = AlertDialog.Builder(dialogContext()).setTitle(folder.title)
            .setItems(actions.map { it.first }.toTypedArray()) { _, index -> actions[index].second() }
            .setNegativeButton("Đóng", null).showLauncherDialog()
    }
    private fun dialogContext() = android.view.ContextThemeWrapper(this,
        if (foreground == Color.parseColor("#F5F7FA")) R.style.Theme_TNLauncher_Dialog_Dark else R.style.Theme_TNLauncher_Dialog_Light)

    private fun editFolder(folder: LauncherFolder?, seed: String? = null) {
        if (folder == null && model.state.value.settings.folders.size >= FolderRules.MAX_FOLDERS) {
            toast("Đã có tối đa ${FolderRules.MAX_FOLDERS} thư mục."); return
        }
        val selected = (folder?.appIds ?: listOfNotNull(seed)).toMutableSet()
        val content = LinearLayout(dialogContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val name = EditText(content.context).apply {
            hint = "Tên thư mục"; setSingleLine(); minHeight = dp(56); typeface = typography.body
            filters = arrayOf(android.text.InputFilter.LengthFilter(80)); setText(folder?.title ?: "Thư mục")
        }
        content.addView(name)
        content.addView(TextView(content.context).apply { text = "Chọn ứng dụng. Ứng dụng được chọn sẽ chuyển vào thư mục này."; typeface = typography.body })
        val list = LinearLayout(content.context).apply { orientation = LinearLayout.VERTICAL }
        val apps = model.state.value.apps
        val byId = apps.associateBy { it.id }
        (apps.map { it.id } + selected.filter { it !in byId }).forEach { id ->
            list.addView(CheckBox(content.context).apply {
                text = byId[id]?.label ?: "Ứng dụng tạm thiếu"; typeface = typography.body; textSize = 16f; minHeight = dp(56)
                isChecked = id in selected
                setOnCheckedChangeListener { _, checked -> if (checked) selected.add(id) else selected.remove(id) }
            })
        }
        content.addView(ScrollView(content.context).apply { addView(list) },
            LinearLayout.LayoutParams(-1, dp(200)))
        popup?.dismiss()
        popup = AlertDialog.Builder(dialogContext()).setTitle(if (folder == null) "Tạo thư mục" else "Chỉnh sửa thư mục")
            .setView(content).setPositiveButton("Lưu") { _, _ ->
                model.saveFolder(LauncherFolder(folder?.id ?: "folder:${java.util.UUID.randomUUID()}", name.text.toString(), selected.toList()))
            }.setNegativeButton("Hủy", null).showLauncherDialog()
    }
    private fun showLaunch(result: LaunchResult) { when (result) {
        LaunchResult.Opened -> hideKeyboard()
        LaunchResult.Missing -> { toast("Ứng dụng không còn khả dụng. Hãy kiểm tra lại."); model.refresh() }
        LaunchResult.Denied -> toast("Không có quyền mở component này.")
        is LaunchResult.Error -> toast(result.reason)
    } }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun styleButton(button: Button) {
        button.typeface = typography.body
        button.setTextColor(android.content.res.ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(androidx.core.graphics.ColorUtils.setAlphaComponent(foreground, 100), foreground)))
        button.elevation = 0f
        button.isAllCaps = false
        button.setPadding(dp(12), dp(6), dp(12), dp(6))
        button.backgroundTintList = null
        button.background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(foreground, 25)), panelBackground(16), null)
    }
}
