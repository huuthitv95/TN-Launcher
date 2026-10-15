package vn.tnlauncher.app.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import vn.tnlauncher.app.LauncherApplication
import vn.tnlauncher.app.R
import vn.tnlauncher.app.data.*
import kotlin.math.max

/** A regular, user-consented Android AppWidgetHost. No privileged vendor binding. */
class WidgetsActivity : AppCompatActivity() {
    private val repository by lazy { LauncherWidgetRepository(applicationContext) }
    private val manager by lazy { AppWidgetManager.getInstance(applicationContext) }
    private val host by lazy { AppWidgetHost(applicationContext, LauncherWidgetRepository.HOST_ID) }
    private val typography by lazy { LauncherTypography(this) }
    private val transactions = Mutex()
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var add: Button
    private lateinit var cancel: Button
    private var state = LauncherWidgets()
    private var busy = true
    private var listening = false
    private var ready = false
    private var dark = false
    private var notice: String? = null
    private var allocatedPendingId: Int? = null
    private var chooser: AlertDialog? = null
    private data class Choice(val info: AppWidgetProviderInfo, val label: String, val icon: Drawable?)
    private data class DisplayWidget(val widget: HostedWidget, val info: AppWidgetProviderInfo?, val label: String)
    private data class WidgetSize(val width: Int, val height: Int)

    private val bindingResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        perform(queued = true) { finishBinding(result.resultCode, result.data) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(12), dp(24), dp(12)) }
        val heading = TextView(this).apply { text = "Tiện ích"; textSize = 28f; typeface = typography.heading }
        root.addView(heading)
        val controls = LinearLayout(this)
        controls.addView(button("Quay lại") { closeWidgets() })
        add = button("Thêm tiện ích") { chooseProvider() }.also(controls::addView)
        controls.addView(button("Thử lại") { retry() })
        root.addView(controls)
        status = TextView(this).apply { textSize = 18f; typeface = typography.body; setPadding(0, dp(8), 0, dp(8)) }
        root.addView(status)
        cancel = button("Hủy thêm tiện ích") { perform { cancelPending(); notice = "Đã hủy thêm tiện ích." } }
        root.addView(cancel)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(24) + bars.left, dp(12) + bars.top, dp(24) + bars.right, dp(12) + bars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeWidgets()
        })
        perform(queued = true) {
            dark = when ((application as LauncherApplication).settings.settings.first().theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
            state = repository.read() // A damaged record never causes automatic data deletion.
            allocatedPendingId = state.pending?.widget?.id
            if (savedInstanceState == null) {
                if (state.pending != null) { cancelPending(); notice = "Lần thêm trước chưa hoàn tất và đã được hủy." }
                val owned = state.widgets.map { it.id }.toSet()
                // An unmatched ID may be a user widget whose bookkeeping was lost. Preserve it.
                val unknown = withContext(Dispatchers.IO) { host.appWidgetIds.any { it !in owned } }
                if (unknown) notice = "Có tiện ích chưa khôi phục được. Dữ liệu được giữ; hãy thử lại."
            }
            ready = true
        }
    }
    override fun onStart() {
        super.onStart()
        try { host.startListening(); listening = true }
        catch (_: RuntimeException) { notice = "Dịch vụ tiện ích chưa khả dụng. Hãy thử lại."; listening = false }
        ViewCompat.requestApplyInsets(root)
        if (ready) perform(queued = true) { }
    }
    override fun onStop() {
        chooser?.dismiss(); chooser = null
        if (listening) runCatching { host.stopListening() }
        listening = false
        super.onStop()
    }
    override fun onDestroy() {
        if (isFinishing) allocatedPendingId?.let { id ->
            val cleanupHost = host
            val cleanupRepository = repository
            // Home can finish this Activity while a consent/configuration window is open.
            // Only this flow's allocation is released; another flow's pending ID is preserved.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    cleanupRepository.discardIncomplete(id, cleanupHost::deleteAppWidgetId)
                }
            }
        }
        super.onDestroy()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 18f; isAllCaps = false; minHeight = dp(56); typeface = typography.body
        setOnClickListener { action() }
    }
    private fun dialogBuilder(): AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper(this,
        if (dark) R.style.Theme_TNLauncher_Dialog_Dark else R.style.Theme_TNLauncher_Dialog_Light))
    private fun perform(queued: Boolean = false, action: suspend () -> Unit) {
        if (busy && !queued) return
        busy = true
        if (::add.isInitialized) updateControls()
        lifecycleScope.launch {
            transactions.withLock {
                busy = true
                updateControls()
                try { action(); reload() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { notice = "Không đọc/lưu hoặc mở được tiện ích. Dữ liệu hiện có được giữ; hãy thử lại." }
                finally { busy = false; if (!isFinishing) updateControls() }
            }
        }
    }
    private suspend fun reload() {
        state = repository.read()
        val widgets = withContext(Dispatchers.IO) { state.widgets.map { widget ->
            val info = manager.getAppWidgetInfo(widget.id)?.takeIf { it.provider == widget.provider }
            DisplayWidget(widget, info, info?.let { runCatching { it.loadLabel(packageManager) }.getOrNull() }
                ?: widget.provider.packageName)
        } }
        root.setBackgroundColor(if (dark) Color.rgb(17, 26, 43) else Color.rgb(241, 245, 250))
        val foreground = if (dark) Color.rgb(245, 247, 250) else Color.rgb(20, 32, 51)
        fun colors(view: View) {
            if (view is TextView) view.setTextColor(foreground)
            if (view is Button) {
                view.backgroundTintList = ColorStateList.valueOf(if (dark) Color.rgb(35, 48, 71) else Color.rgb(233, 239, 248))
                view.setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                    intArrayOf(androidx.core.graphics.ColorUtils.setAlphaComponent(foreground, 100), foreground)))
            }
            if (view is ViewGroup) for (index in 0 until view.childCount) colors(view.getChildAt(index))
        }
        content.removeAllViews()
        widgets.forEach { display ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, dp(16)) }
            card.addView(TextView(this).apply { text = display.label; textSize = 20f; typeface = typography.heading })
            val info = display.info
            val size = info?.let(::widgetSize)
            if (info == null || size == null) {
                card.addView(TextView(this).apply {
                    text = if (info == null) "Nhà cung cấp chưa khả dụng. Cấu hình vẫn được giữ."
                    else "Tiện ích lớn hơn diện tích hiện tại. Hãy dùng màn hình/chữ nhỏ hơn hoặc xóa tiện ích."
                    textSize = 18f; typeface = typography.body
                })
            } else {
                try {
                    val view = host.createView(this, display.widget.id, info)
                    card.addView(view, LinearLayout.LayoutParams(-1, size.height))
                    view.updateAppWidgetSize(Bundle(), pxToDp(size.width), pxToDp(size.height), pxToDp(size.width), pxToDp(size.height))
                } catch (_: RuntimeException) {
                    card.addView(TextView(this).apply { text = "Không hiển thị được tiện ích này. Hãy thử lại."; textSize = 18f })
                }
            }
            card.addView(button("Xóa tiện ích") {
                dialogBuilder().setTitle("Xóa ${display.label}?").setMessage("Chỉ xóa bản tiện ích khỏi TN Launcher.")
                    .setNegativeButton("Giữ lại", null).setPositiveButton("Xóa") { _, _ -> perform {
                        withContext(Dispatchers.IO) { host.deleteAppWidgetId(display.widget.id) }
                        repository.remove(display.widget.id)
                        notice = "Đã xóa tiện ích."
                    } }.showLauncherDialog()
            }.apply { contentDescription = "Xóa tiện ích ${display.widget.id}" })
            content.addView(card)
        }
        // Widget RemoteViews retain provider typography/colors; style only the host chrome.
        root.getChildAt(0).let(::colors)
        root.getChildAt(1).let(::colors)
        status.setTextColor(foreground)
        colors(cancel)
        for (index in 0 until content.childCount) {
            val card = content.getChildAt(index) as ViewGroup
            for (child in 0 until card.childCount) if (card.getChildAt(child) !is AppWidgetHostView) colors(card.getChildAt(child))
        }
    }
    private fun updateControls() {
        add.isEnabled = !busy && ready && listening && state.pending == null && state.widgets.size < LauncherWidgetRepository.MAX_WIDGETS
        cancel.visibility = if (state.pending == null) View.GONE else View.VISIBLE
        cancel.isEnabled = !busy
        status.text = when {
            notice != null -> notice
            busy -> "Đang đọc tiện ích…"
            state.pending != null -> "Đang chờ bạn cho phép hoặc cấu hình tiện ích. Có thể hủy để giải phóng bản đang thêm."
            state.widgets.isEmpty() -> "Chưa có tiện ích. Chọn nhà cung cấp đã cài và xác nhận trong hộp thoại Android."
            else -> "${state.widgets.size}/${LauncherWidgetRepository.MAX_WIDGETS} tiện ích. Nội dung và nút trong tiện ích do ứng dụng gốc cung cấp."
        }
    }
    private fun pxToDp(px: Int) = (px / resources.displayMetrics.density).toInt()
    private fun widgetSize(info: AppWidgetProviderInfo): WidgetSize? {
        val padding = AppWidgetHostView.getDefaultPaddingForWidget(this, info.provider, null)
        val width = (root.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels) - root.paddingLeft - root.paddingRight
        val availableHeight = (root.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels) - root.paddingTop - root.paddingBottom - dp(100)
        val height = max(info.minHeight + padding.top + padding.bottom, dp((112 * max(1f, resources.configuration.fontScale)).toInt()))
        return if (width < info.minWidth + padding.left + padding.right || height > availableHeight || width <= 0) null
        else WidgetSize(width, height)
    }
    private fun options(info: AppWidgetProviderInfo): Bundle {
        val size = checkNotNull(widgetSize(info))
        return Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, pxToDp(size.width))
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, pxToDp(size.width))
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, pxToDp(size.height))
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, pxToDp(size.height))
        }
    }
    private fun chooseProvider() = perform {
        notice = null
        val providers = withContext(Dispatchers.IO) {
            manager.installedProviders.filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
                .map { info -> Choice(info, runCatching { info.loadLabel(packageManager) }.getOrNull() ?: info.provider.packageName,
                    runCatching { info.loadIcon(applicationContext, resources.displayMetrics.densityDpi) }.getOrNull()) }
                .sortedBy { LauncherRules.searchKey(it.label) }
        }
        if (providers.isEmpty()) { notice = "Không có nhà cung cấp tiện ích đã cài. Hãy cài ứng dụng hỗ trợ App Widget rồi thử lại."; return@perform }
        val adapter = object : BaseAdapter() {
            override fun getCount() = providers.size
            override fun getItem(position: Int) = providers[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = providers[position]
                return LinearLayout(parent.context).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL; minimumHeight = dp(56); setPadding(dp(12), dp(8), dp(12), dp(8))
                    addView(ImageView(context).apply { setImageDrawable(item.icon); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(40), dp(40)))
                    addView(TextView(context).apply { text = item.label; textSize = 18f; typeface = typography.body; setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
                }
            }
        }
        chooser = dialogBuilder().setTitle("Chọn tiện ích đã cài").setAdapter(adapter) { _, index ->
            perform { beginAdd(providers[index].info) }
        }.setNegativeButton("Đóng", null).showLauncherDialog()
    }
    private suspend fun beginAdd(info: AppWidgetProviderInfo) {
        notice = null
        if (widgetSize(info) == null) { notice = "Tiện ích này không vừa diện tích màn hình hiện tại."; return }
        val widget = repository.allocatePending(info.provider, host::allocateAppWidgetId, host::deleteAppWidgetId)
        val id = widget.id
        allocatedPendingId = id
        try {
            val widgetOptions = options(info)
            val allowed = withContext(Dispatchers.IO) { manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, widgetOptions) }
            if (allowed) afterBound(widget)
            else bindingResult.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, widgetOptions))
        } catch (error: Exception) { cancelPending(); throw error }
    }
    private suspend fun finishBinding(resultCode: Int, result: Intent?) {
        val pending = repository.read().pending ?: return
        if (pending.stage != WidgetAddStage.BIND) return
        val returnedId = result?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pending.widget.id) ?: pending.widget.id
        if (resultCode != Activity.RESULT_OK || returnedId != pending.widget.id) {
            cancelPending(); notice = "Đã hủy thêm tiện ích."; return
        }
        try { afterBound(pending.widget) } catch (error: Exception) { cancelPending(); throw error }
    }
    private suspend fun afterBound(widget: HostedWidget) {
        val info = checkNotNull(withContext(Dispatchers.IO) { manager.getAppWidgetInfo(widget.id) })
        check(info.provider == widget.provider) { "Widget provider changed during binding" }
        if (info.configure != null) {
            repository.configuring(widget.id)
            // Public API21+ supports provider configuration, including profile boundaries.
            host.startAppWidgetConfigureActivityForResult(this, widget.id, 0, CONFIGURE_REQUEST, null)
        } else { repository.complete(widget); allocatedPendingId = null }
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIGURE_REQUEST) perform(queued = true) {
            val pending = repository.read().pending ?: return@perform
            if (pending.stage != WidgetAddStage.CONFIGURE) return@perform
            val returned = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pending.widget.id) ?: pending.widget.id
            val valid = withContext(Dispatchers.IO) { manager.getAppWidgetInfo(pending.widget.id)?.provider == pending.widget.provider }
            if (resultCode == Activity.RESULT_OK && returned == pending.widget.id && valid) {
                repository.complete(pending.widget); allocatedPendingId = null
            }
            else { cancelPending(); notice = "Đã hủy cấu hình tiện ích." }
        }
    }
    private suspend fun cancelPending() {
        repository.read().pending?.let {
            withContext(NonCancellable) { repository.discardIncomplete(it.widget.id, host::deleteAppWidgetId) }
            allocatedPendingId = null
        }
        state = repository.read()
    }
    private fun closeWidgets() = perform { try { cancelPending() } finally { finish() } }
    private fun retry() {
        notice = null
        if (!listening) try { host.startListening(); listening = true }
        catch (_: RuntimeException) { notice = "Dịch vụ tiện ích chưa khả dụng. Hãy thử lại." }
        perform { ready = true }
    }
    companion object { private const val CONFIGURE_REQUEST = 3101 }
}
