package vn.tnlauncher.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.CheckBox
import android.widget.EditText
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.tnlauncher.app.data.*
import vn.tnlauncher.app.integration.*
import vn.tnlauncher.app.ui.HomeActivity

@RunWith(AndroidJUnit4::class)
class ReconstructionInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val fixture = InstrumentationRegistry.getInstrumentation().context.packageName
    private fun home() = ActivityScenario.launch<HomeActivity>(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        .setComponent(ComponentName(context, HomeActivity::class.java)))
    @Before fun emulatorOnly() {
        assertEquals("1", device.executeShellCommand("getprop ro.kernel.qemu").trim())
        assertEquals("29", device.executeShellCommand("getprop ro.build.version.sdk").trim())
        device.executeShellCommand("cmd package set-home-activity vn.tnlauncher.app/.ui.HomeActivity")
    }
    @Test fun publicSettingsActionsOpenActualSystemScreensAndReturn() {
        val system = AndroidSystemSettingsIntegration(context)
        assertEquals(setOf("wifi", "date", "global_search", "live_wallpaper"), system.inspect().map { it.id }.toSet())
        assertTrue(system.inspect().filter { it.id in setOf("wifi", "date") }.all { it.availability == Availability.AVAILABLE })
        home().use {
            onView(withId(R.id.settings_button)).perform(click())
            onView(withText("Mở Wi-Fi")).perform(scrollTo(), click())
            assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 12000))
            device.pressHome()
            assertTrue(device.wait(Until.hasObject(By.res(context.packageName, "grid")), 5000))
            assertEquals(LaunchResult.Opened, system.open("date"))
            assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 12000))
            device.pressHome()
            assertEquals(LaunchResult.Missing, system.open("unknown"))
        }
    }
    @Test fun clockChangesAtTheRealMinuteWhileHomeRemainsVisible() {
        home().use {
            val clock = device.wait(Until.findObject(By.res(context.packageName, "clock")), 5000)!!
            val initial = clock.text
            val changed = java.util.regex.Pattern.compile("(?!${java.util.regex.Pattern.quote(initial)}$).+")
            assertTrue("A visible Home must receive the real system minute tick without polling",
                device.wait(Until.hasObject(By.res(context.packageName, "clock").text(changed)), 65000))
            assertEquals(context.packageName, device.currentPackageName)
        }
    }
    @Test fun optionalSystemRequestsReportAvailabilityAndCancellingWallpaperKeepsHomeSettings() = runBlocking {
        val app = context as LauncherApplication
        val previous = app.settings.settings.first()
        app.settings.update { it.copy(systemWallpaper = false) }
        try {
            home().use {
                val system = AndroidSystemSettingsIntegration(context)
                val search = system.inspect().single { it.id == "global_search" }
                device.pressKeyCode(android.view.KeyEvent.KEYCODE_T)
                if (search.availability == Availability.AVAILABLE) {
                    val pkg = ComponentName.unflattenFromString(search.component!!)!!.packageName
                    assertTrue(device.wait(Until.hasObject(By.pkg(pkg)), 12000))
                    device.pressHome()
                    device.waitForIdle()
                } else {
                    assertTrue(system.open("global_search") != LaunchResult.Opened)
                    assertTrue(device.wait(Until.hasObject(By.res(context.packageName, "grid")), 5000))
                }
                val wallpaper = system.inspect().single { it.id == "live_wallpaper" }
                device.wait(Until.findObject(By.res(context.packageName, "settings_button")), 5000)!!.click()
                if (wallpaper.availability == Availability.AVAILABLE) {
                    device.waitForIdle()
                    val panel = device.wait(Until.findObject(By.res(context.packageName, "settings_panel")), 5000)!!
                    repeat(12) {
                        if (!device.hasObject(By.text("Mở Hình nền động Android"))) {
                            panel.scroll(androidx.test.uiautomator.Direction.DOWN, .75f)
                        }
                    }
                    device.wait(Until.findObject(By.text("Mở Hình nền động Android")), 5000)!!.click()
                    val pkg = ComponentName.unflattenFromString(wallpaper.component!!)!!.packageName
                    assertTrue(device.wait(Until.hasObject(By.pkg(pkg)), 12000))
                    device.pressBack()
                } else {
                    assertTrue(system.open("live_wallpaper") != LaunchResult.Opened)
                }
                val current = app.settings.settings.first()
                assertFalse("Opening and cancelling the system picker is not a committed wallpaper change", current.systemWallpaper)
                assertEquals(previous.wallpaper, current.wallpaper)
            }
        } finally { app.settings.update { previous } }
    }
    @Test fun systemWallpaperIsActuallyShownAndPersistsAcrossRecreation() = runBlocking {
        val app = context as LauncherApplication
        val previous = app.settings.settings.first()
        val previousProvider = android.app.WallpaperManager.getInstance(context).wallpaperInfo?.component
        try {
            home().use { scenario ->
                onView(withId(R.id.settings_button)).perform(click())
                onView(withText("Dùng hình nền hệ thống")).perform(scrollTo(), click())
                withTimeout(5000) { app.settings.settings.first { it.systemWallpaper } }
                device.pressHome()
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertTrue(activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER != 0)
                    assertEquals(android.view.View.GONE, activity.findViewById<android.view.View>(R.id.wallpaper).visibility)
                    assertTrue(activity.findViewById<android.view.View>(R.id.root).background.alpha < 255)
                }
                // Selection belongs to Android; this mode also displays an existing static system wallpaper.
                assertEquals(previousProvider, android.app.WallpaperManager.getInstance(context).wallpaperInfo?.component)
            }
        } finally { app.settings.update { previous } }
    }
    @Test fun vendorFixturePreservesPackageLauncherPreferenceAndExplicitSubscreen() {
        val normal = AndroidWincavnIntegration(context, listOf(VendorActivityEntry("sample", "Fixture only", fixture,
            PrivateFixtureActivity::class.java.name)))
        assertEquals(ComponentName(fixture, FixtureActivity::class.java.name).flattenToString(), normal.inspect().single().component)
        val explicit = AndroidWincavnIntegration(context, listOf(VendorActivityEntry("sample", "Fixture only", fixture,
            FallbackFixtureActivity::class.java.name, false)))
        assertEquals(ComponentName(fixture, FallbackFixtureActivity::class.java.name).flattenToString(), explicit.inspect().single().component)
        try {
            assertEquals(LaunchResult.Opened, explicit.open("sample"))
            assertTrue(device.wait(Until.hasObject(By.text("Màn hình phụ mẫu đang chạy")), 5000))
        } finally { device.pressHome() }
        val protected = AndroidWincavnIntegration(context, listOf(VendorActivityEntry("sample", "Fixture only", fixture,
            ProtectedFixtureActivity::class.java.name, false)))
        assertEquals(Availability.PERMISSION_REQUIRED, protected.inspect().single().availability)
        assertEquals(LaunchResult.Denied, protected.open("sample"))
        assertEquals(12, AndroidWincavnIntegration(context).inspect().size)
        assertTrue(AndroidWincavnIntegration(context).inspect().all { it.availability == Availability.PACKAGE_MISSING })
    }
    @Test fun appDetailsAndSystemUninstallConfirmationCanBeCancelledWithoutDeletingFixture() {
        val manager = AndroidAppManagement(context)
        val id = ComponentName(fixture, FixtureActivity::class.java.name).flattenToString()
        assertTrue(manager.inspect(id).uninstall)
        assertFalse(manager.inspect("com.android.settings/com.android.settings.Settings").uninstall)
        assertEquals(LaunchResult.Denied, manager.requestUninstall("com.android.settings/com.android.settings.Settings"))
        try {
            assertEquals(LaunchResult.Opened, manager.details(id))
            assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 12000))
            device.pressHome()
            assertEquals(LaunchResult.Opened, manager.requestUninstall(id))
            assertTrue(device.wait(Until.hasObject(By.res("android", "button2")), 12000))
            device.findObject(By.res("android", "button2")).click()
            assertNotNull(context.packageManager.getApplicationInfo(fixture, 0))
        } finally { device.pressHome() }
        assertEquals(LaunchResult.Missing, manager.details("missing/missing.Main"))
    }
    @Test fun folderCreateOpenEditRenameAndDeleteRestoreRealHomeApps() = runBlocking {
        val app = context as LauncherApplication
        val previous = app.settings.settings.first()
        app.settings.update { it.copy(folders = emptyList(), homeOrder = emptyList()) }
        try {
            home().use { scenario ->
                android.util.Log.i("FolderJourney", "create")
                onView(withId(R.id.settings_button)).perform(click())
                onView(withText("Tạo thư mục")).perform(scrollTo(), click())
                val minimum = (56 * context.resources.displayMetrics.density).toInt()
                onView(withId(android.R.id.button1)).check { view, _ ->
                    val rect = android.graphics.Rect(); view.getGlobalVisibleRect(rect)
                    assertTrue("Dialog save button visible rectangle $rect must reach $minimum px (56dp)", rect.height() >= minimum)
                }
                onView(allOf(isAssignableFrom(EditText::class.java), isDisplayed())).perform(replaceText("Điều hướng"), closeSoftKeyboard())
                onView(allOf(isAssignableFrom(CheckBox::class.java), withText("Ứng dụng mẫu A"))).perform(scrollTo(), click())
                onView(withText("Lưu")).perform(click())
                val saved = withTimeout(5000) { app.settings.settings.first { it.folders.any { folder -> folder.title == "Điều hướng" } } }
                assertEquals(listOf(ComponentName(fixture, FixtureActivity::class.java.name).flattenToString()), saved.folders.single().appIds)
                // Recreate this directly launched instance before system Home can select its own HOME task.
                android.util.Log.i("FolderJourney", "recreate")
                scenario.recreate()
                device.pressHome()
                assertTrue(device.wait(Until.hasObject(By.desc("Điều hướng")), 5000))
                device.findObject(By.desc("Điều hướng")).click()
                android.util.Log.i("FolderJourney", "open member")
                device.wait(Until.findObject(By.res("android", "text1").text("Ứng dụng mẫu A")), 5000)!!.click()
                assertTrue(device.wait(Until.hasObject(By.text("Ứng dụng mẫu A đang chạy")), 12000))
                device.pressHome()
                assertTrue(device.wait(Until.hasObject(By.desc("Điều hướng")), 5000))
                device.findObject(By.desc("Điều hướng")).click()
                android.util.Log.i("FolderJourney", "edit")
                device.wait(Until.findObject(By.res("android", "button3")), 5000)!!.click()
                assertTrue(device.wait(Until.hasObject(By.clazz(EditText::class.java)), 5000))
                onView(allOf(isAssignableFrom(EditText::class.java), isDisplayed()))
                    .perform(replaceText("Cài đặt và bản đồ"), closeSoftKeyboard())
                android.util.Log.i("FolderJourney", "save rename")
                device.wait(Until.findObject(By.res("android", "button1")), 5000)!!.click()
                val renamed = withTimeout(5000) { app.settings.settings.first { it.folders.any { folder -> folder.title == "Cài đặt và bản đồ" } } }
                assertTrue(device.wait(Until.hasObject(By.desc("Cài đặt và bản đồ")), 5000))
                device.findObject(By.desc("Cài đặt và bản đồ")).longClick()
                android.util.Log.i("FolderJourney", "delete menu")
                device.waitForIdle()
                // After system Home selected its HOME task, use the active accessibility window.
                val remove = device.wait(Until.findObject(By.text("Xóa thư mục · đưa ứng dụng về Home")), 5000)
                assertNotNull(remove)
                remove!!.click()
                val deleted = withTimeoutOrNull(5000) { app.settings.settings.first { it.folders.none { folder -> folder.id == renamed.folders.single().id } } }
                assertNotNull("Folder removal was not persisted: ${app.settings.settings.first().folders}", deleted)
                var restored = device.wait(Until.findObject(By.desc("Ứng dụng mẫu A")), 1000)
                while (restored == null) {
                    val next = device.findObject(By.res(context.packageName, "next"))
                    if (next == null || !next.isEnabled) break
                    next.click(); device.waitForIdle()
                    restored = device.wait(Until.findObject(By.desc("Ứng dụng mẫu A")), 1000)
                }
                assertNotNull("Deleted folder must restore its real app on the appropriate Home page", restored)
            }
        } finally { app.settings.update { previous } }
    }
    @Test fun folderDataStoreSurvivesReopenAndCorruptRowsArePreservedBeforeRecovery() = runBlocking {
        val name = "folders-${System.nanoTime()}"
        val folder = LauncherFolder("folder:00000000-0000-0000-0000-000000000001", "Đường · Bản đồ", listOf("com.missing/com.missing.Main"))
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val repo = DataStoreLauncherSettingsRepository(context, name, scope)
        repo.update { it.copy(folders = listOf(folder), homeOrder = listOf(folder.id)) }
        assertEquals(listOf(folder), repo.settings.first().folders)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        val otherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val reopened = DataStoreLauncherSettingsRepository(context, name, otherScope).settings.first()
            assertEquals(listOf(folder), reopened.folders); assertEquals(listOf(folder.id), reopened.homeOrder)
        } finally { otherScope.coroutineContext[Job]!!.cancelAndJoin() }
        val rawName = "$name-invalid"
        val source = context.preferencesDataStoreFile(rawName)
        val writerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val writer = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = writerScope, produceFile = { source })
        writer.updateData { androidx.datastore.preferences.core.mutablePreferencesOf(DataStoreLauncherSettingsRepository.FOLDERS to "truncated") }
        writerScope.coroutineContext[Job]!!.cancelAndJoin()
        val bytes = source.readBytes()
        val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val recovered = DataStoreLauncherSettingsRepository(context, rawName, recoveryScope).settings.first()
            assertTrue(recovered.folders.isEmpty()); assertNotNull(recovered.recoveryNotice)
            assertTrue(java.io.File(context.filesDir, "recovery").listFiles()!!.any { it.readBytes().contentEquals(bytes) })
        } finally { recoveryScope.coroutineContext[Job]!!.cancelAndJoin() }
    }
}
