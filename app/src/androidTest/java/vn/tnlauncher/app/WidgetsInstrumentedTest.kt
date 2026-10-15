package vn.tnlauncher.app

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.tnlauncher.app.data.LauncherWidgetRepository
import vn.tnlauncher.app.data.LauncherWidgets
import vn.tnlauncher.app.data.WidgetAddStage
import vn.tnlauncher.app.ui.WidgetsActivity

@RunWith(AndroidJUnit4::class)
class WidgetsInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val repository = LauncherWidgetRepository(context)
    private lateinit var host: AppWidgetHost
    private var originalIds = emptySet<Int>()

    @Before fun emulatorOnlyAndBaseline() = runBlocking {
        assertEquals("29", device.executeShellCommand("getprop ro.build.version.sdk").trim())
        assertEquals("1", device.executeShellCommand("getprop ro.kernel.qemu").trim())
        instrumentation.runOnMainSync { host = AppWidgetHost(context, LauncherWidgetRepository.HOST_ID) }
        val state = repository.read()
        assertNull("Do not interrupt an existing widget-add flow", state.pending)
        assertTrue("A slot is required for the test-owned widget", state.widgets.size < LauncherWidgetRepository.MAX_WIDGETS)
        originalIds = host.appWidgetIds.toSet()
        val providers = AppWidgetManager.getInstance(context).installedProviders
        listOf(WidgetFixtureProvider::class.java to "Tiện ích mẫu cấu hình",
            SimpleWidgetFixtureProvider::class.java to "Tiện ích mẫu trực tiếp").forEach { (provider, label) ->
            val actual = providers.singleOrNull { it.provider == ComponentName(instrumentation.context, provider) }
            assertNotNull("Actual test widget provider must be installed: ${provider.name}", actual)
            assertEquals(label, actual!!.loadLabel(context.packageManager))
        }
    }
    @After fun restoreOnlyTestOwnedAllocations() = runBlocking {
        if (!::host.isInitialized) return@runBlocking
        val newIds = host.appWidgetIds.filterNot(originalIds::contains)
        val current = repository.read()
        current.pending?.takeIf { it.widget.id in newIds }?.let {
            host.deleteAppWidgetId(it.widget.id); repository.clearPending(it.widget.id)
        }
        newIds.forEach { id -> host.deleteAppWidgetId(id); repository.remove(id) }
        device.pressHome()
    }
    private fun screen() = ActivityScenario.launch<WidgetsActivity>(Intent(context, WidgetsActivity::class.java))
    private fun node(text: String): UiObject2 = device.wait(Until.findObject(By.text(text)), 7000)
        ?: error("UI text not found: $text")
    private fun awaitState(predicate: (LauncherWidgets) -> Boolean): LauncherWidgets {
        val end = android.os.SystemClock.uptimeMillis() + 7000
        while (android.os.SystemClock.uptimeMillis() < end) {
            val state = runBlocking { repository.read() }
            if (predicate(state)) return state
            android.os.SystemClock.sleep(100)
        }
        error("Widget state did not reach the expected transition")
    }
    private fun select(label: String) {
        val add = node("Thêm tiện ích")
        assertTrue(device.wait(Until.hasObject(By.text("Thêm tiện ích").enabled(true)), 7000))
        add.click()
        node("Chọn tiện ích đã cài")
        repeat(12) {
            device.findObject(By.text(label))?.let { choice -> choice.click(); return }
            val list = device.findObject(By.clazz("android.widget.ListView"))
                ?: error("Actual widget provider chooser list was not shown")
            if (!list.scroll(Direction.DOWN, .75f)) {
                (device.findObject(By.text(label)) ?: error("Installed provider not found after scrolling chooser: $label")).click()
                return
            }
        }
        error("Installed provider was not reached in the chooser: $label")
    }
    private fun consent(allow: Boolean) {
        val confirm = device.wait(Until.findObject(By.res("android", "button1")), 7000)
            ?: error("Actual Android widget binding consent was not shown")
        assertNotEquals("Binding consent must belong to the system", context.packageName, device.currentPackageName)
        // Exercise one binding, never grant the optional persistent 'always allow' choice.
        device.findObject(By.clazz("android.widget.CheckBox"))?.let { if (it.isChecked) it.click() }
        if (allow) confirm.click()
        else (device.findObject(By.res("android", "button2")) ?: error("Consent cancel button missing")).click()
    }
    @Test fun bindingAndConfigurationCancellationReleaseAllocatedIds() {
        screen().use { scenario ->
            select("Tiện ích mẫu cấu hình")
            consent(allow = false)
            awaitState { it.pending == null }
            assertEquals(originalIds, host.appWidgetIds.toSet())

            select("Tiện ích mẫu cấu hình")
            consent(allow = true)
            node("Cấu hình tiện ích mẫu")
            val pending = awaitState { it.pending?.stage == WidgetAddStage.CONFIGURE }.pending!!
            assertEquals("Pending allocation must survive a new repository instance", pending,
                runBlocking { LauncherWidgetRepository(context).read() }.pending)
            var originalHost: WidgetsActivity? = null
            // ActivityScenario.recreate() first resumes the host, disturbing the open
            // external configuration Activity. Recreate the stopped host directly.
            scenario.onActivity { activity -> originalHost = activity; activity.recreate() }
            assertEquals(pending, runBlocking { repository.read() }.pending)
            node("Hủy cấu hình mẫu").click()
            awaitState { it.pending == null }
            scenario.onActivity { assertNotSame("Host must be recreated across the pending configuration", originalHost, it) }
            assertEquals(originalIds, host.appWidgetIds.toSet())
        }
    }
    @Test fun providerWithoutConfigurationPersistsAfterClosingAndReopeningHost() {
        val added = screen().use {
            select("Tiện ích mẫu trực tiếp")
            consent(allow = true)
            val widget = awaitState { state -> state.pending == null && state.widgets.any { it.id !in originalIds } }
                .widgets.single { it.id !in originalIds }
            assertEquals(ComponentName(instrumentation.context, SimpleWidgetFixtureProvider::class.java), widget.provider)
            node("Tiện ích mẫu thật · 0")
            widget
        }
        screen().use {
            node("Tiện ích mẫu thật · 0")
            assertTrue(runBlocking { LauncherWidgetRepository(context).read() }.widgets.contains(added))
        }
    }
    @Test fun missingProviderIsAnErrorStateAndNeverErasesTheSavedWidget() = runBlocking {
        // Test-only damaged/missing-provider record; no fake widget is rendered in the product.
        val record = vn.tnlauncher.app.data.HostedWidget(Int.MAX_VALUE - 7,
            ComponentName("vn.tnlauncher.missingfixture", "vn.tnlauncher.missingfixture.Provider"))
        assertFalse(record.id in originalIds)
        repository.begin(record); repository.complete(record)
        try {
            screen().use {
                node("Nhà cung cấp chưa khả dụng. Cấu hình vẫn được giữ.")
                assertTrue(repository.read().widgets.contains(record))
                assertEquals(originalIds, host.appWidgetIds.toSet())
            }
        } finally { repository.remove(record.id) }
    }
    @Test fun actualWidgetConfigureUpdateClickRecreateAndRemove() {
        screen().use { scenario ->
            select("Tiện ích mẫu cấu hình")
            consent(allow = true)
            node("Lưu tiện ích mẫu").click()
            val added = awaitState { it.pending == null && it.widgets.any { widget -> widget.id !in originalIds } }
                .widgets.single { it.id !in originalIds }
            assertEquals(ComponentName(instrumentation.context, WidgetFixtureProvider::class.java), added.provider)
            node("Tiện ích mẫu thật · 0")
            scenario.recreate()
            node("Tiện ích mẫu thật · 0")
            assertTrue(runBlocking { LauncherWidgetRepository(context).read() }.widgets.contains(added))
            scenario.onActivity { activity ->
                fun findHost(view: View): AppWidgetHostView? {
                    if (view is AppWidgetHostView && view.appWidgetId == added.id) return view
                    if (view is ViewGroup) for (index in 0 until view.childCount) findHost(view.getChildAt(index))?.let { return it }
                    return null
                }
                val widget = findHost(activity.window.decorView)
                assertNotNull("Real AppWidgetHostView must be rendered", widget)
                assertTrue(widget!!.width > 0 && widget.height > 0)
                assertTrue(widget.width <= activity.resources.displayMetrics.widthPixels)
            }
            node("Cập nhật mẫu").click()
            node("Tiện ích mẫu thật · 1")
            node("Mở ứng dụng mẫu từ tiện ích").click()
            node("Ứng dụng mẫu A đang chạy")
            assertEquals(instrumentation.context.packageName, device.currentPackageName)
            // System Home owns task return after an external app. Reopen through the actual entry.
            device.pressHome()
            val settings = device.wait(Until.findObject(By.res(context.packageName, "settings_button")), 7000)!!
            settings.click()
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Tiện ích Android"))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo(), androidx.test.espresso.action.ViewActions.click())
            node("Tiện ích mẫu thật · 1")
            (device.wait(Until.findObject(By.desc("Xóa tiện ích ${added.id}")), 5000)
                ?: error("Widget removal action missing")).click()
            (device.wait(Until.findObject(By.res("android", "button1")), 5000)
                ?: error("Widget removal confirmation missing")).click()
            awaitState { state -> state.widgets.none { it.id == added.id } }
            assertEquals(originalIds, host.appWidgetIds.toSet())
        }
    }
}
