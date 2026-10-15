package vn.tnlauncher.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import vn.tnlauncher.app.data.*
import vn.tnlauncher.app.integration.*
import vn.tnlauncher.app.ui.HomeActivity
import vn.tnlauncher.app.ui.LauncherTypography
import java.io.File
import android.view.View
import android.view.MotionEvent
import android.os.SystemClock
import android.graphics.Rect
import org.hamcrest.Matcher

@RunWith(AndroidJUnit4::class)
class LauncherInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun launchHome(): ActivityScenario<HomeActivity> = ActivityScenario.launch(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(context, HomeActivity::class.java))
    )
    @Before fun emulatorHomeSelection() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertEquals("29", device.executeShellCommand("getprop ro.build.version.sdk").trim())
        assertEquals("1", device.executeShellCommand("getprop ro.kernel.qemu").trim())
        device.executeShellCommand("cmd package set-home-activity vn.tnlauncher.app/.ui.HomeActivity")
    }
    @After fun restoreTestLayout() = runBlocking {
        (context as LauncherApplication).settings.update { it.copy(homeOrder = emptyList(), favorites = emptyList()) }
    }
    @Test fun manifestAndRealAppsAndMissingVendor() = runBlocking {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName)
        assertTrue(context.packageManager.queryIntentActivities(home, 0).any { it.activityInfo.exported })
        val repository = AndroidLauncherAppRepository(context)
        val apps = repository.load()
        assertTrue(apps.any { it.id.startsWith("com.android.settings/") })
        val unchangedSettings = apps.first { it.id.startsWith("com.android.settings/") }
        assertSame("Unchanged discovery should reuse the installed app icon/label", unchangedSettings,
            repository.load().first { it.id == unchangedSettings.id })
        assertTrue(apps.none { it.id.startsWith(context.packageName + "/") })
        assertEquals(LaunchResult.Missing, AndroidLauncherAppRepository(context).launch("missing/missing.Main"))
        assertTrue(AndroidWincavnIntegration(context).inspect().all { it.availability == Availability.PACKAGE_MISSING })
        assertEquals(LaunchResult.Missing, AndroidWincavnIntegration(context).openManual())
    }
    @Test fun dataStoreRoundTripAndCorruptionPreservesBytes() = runBlocking {
        val name = "test-${System.nanoTime()}"
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val repo = DataStoreLauncherSettingsRepository(context, name, scope)
        repo.update { it.copy(homeOrder = listOf("com.a/com.a.Main"), favorites = listOf("com.a/com.a.Main"), theme = ThemeMode.DARK, wallpaper = "wallpaper-test.jpg", reducedMotion = true) }
        val value = repo.settings.first()
        assertEquals(ThemeMode.DARK, value.theme); assertEquals(listOf("com.a/com.a.Main"), value.favorites)
        assertEquals("wallpaper-test.jpg", value.wallpaper); assertTrue(value.reducedMotion)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        val newScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        assertEquals(value, DataStoreLauncherSettingsRepository(context, name, newScope).settings.first())
        newScope.coroutineContext[Job]!!.cancelAndJoin()
        val corruptName = "$name-corrupt"
        val bytes = byteArrayOf(0, -1, -1, -1, -1, 22)
        val corrupt = context.preferencesDataStoreFile(corruptName)
        corrupt.parentFile!!.mkdirs(); corrupt.writeBytes(bytes)
        val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val recovered = DataStoreLauncherSettingsRepository(context, corruptName, recoveryScope).settings.first()
        assertNotNull(recovered.recoveryNotice); assertTrue(recovered.favorites.isEmpty())
        assertTrue(File(context.filesDir, "recovery").listFiles()!!.any { it.readBytes().contentEquals(bytes) })
        recoveryScope.coroutineContext[Job]!!.cancelAndJoin()
    }
    @Test fun actualComponentAccessAndFixtureLaunch() {
        val fixturePackage = InstrumentationRegistry.getInstrumentation().context.packageName
        val launcher = SafeActivityLauncher(context)
        assertEquals(Availability.COMPONENT_UNAVAILABLE, launcher.inspect(ComponentName(fixturePackage, PrivateFixtureActivity::class.java.name)))
        assertEquals(Availability.PERMISSION_REQUIRED, launcher.inspect(ComponentName(fixturePackage, ProtectedFixtureActivity::class.java.name)))
        assertEquals(LaunchResult.Denied, launcher.launch(ComponentName(fixturePackage, ProtectedFixtureActivity::class.java.name)))
        assertEquals(LaunchResult.Opened, launcher.launch(ComponentName(fixturePackage, FixtureActivity::class.java.name)))
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.pkg(fixturePackage)), 5000))
        device.pressHome()
        assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.pkg(context.packageName)), 5000))
    }
    @Test fun logicalPreferencesRecoveryAndWallpaperCommitCleanup() = runBlocking {
        val name = "logical-${System.nanoTime()}"
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = context.preferencesDataStoreFile(name)
        val raw = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        raw.updateData { androidx.datastore.preferences.core.mutablePreferencesOf(
            androidx.datastore.preferences.core.booleanPreferencesKey("theme") to true,
            androidx.datastore.preferences.core.stringPreferencesKey("home_order") to "../private\ncom.a/com.a.Main",
            androidx.datastore.preferences.core.stringPreferencesKey("unrelated") to "retain"
        ) }
        val original = file.readBytes()
        scope.coroutineContext[Job]!!.cancelAndJoin()
        val fixedScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val repo = DataStoreLauncherSettingsRepository(context, name, fixedScope)
        val value = repo.settings.first()
        assertEquals(ThemeMode.SYSTEM, value.theme)
        assertEquals(listOf("com.a/com.a.Main"), value.homeOrder)
        assertNotNull(value.recoveryNotice)
        assertTrue(File(context.filesDir, "recovery").listFiles()!!.any { it.readBytes().contentEquals(original) })
        val oldImage = File(context.filesDir, "wallpaper-${System.nanoTime()}.jpg").apply { writeBytes(byteArrayOf(1)) }
        val newImage = File(context.filesDir, "wallpaper-${System.nanoTime()}.jpg").apply { writeBytes(byteArrayOf(2)) }
        repo.update { it.copy(wallpaper = oldImage.name) }
        repo.update { it.copy(wallpaper = newImage.name) }
        assertFalse(oldImage.exists()); assertTrue(newImage.exists())
        repo.update { it.copy(wallpaper = null) }
        assertFalse(newImage.exists())
        fixedScope.coroutineContext[Job]!!.cancelAndJoin()
    }
    @Test fun actualHomeAndDockDragPersist() = runBlocking {
        val app = context as LauncherApplication
        val apps = app.apps.load()
        assertTrue(apps.size >= 2)
        val first = apps[0].id; val second = apps[1].id
        app.settings.update { it.copy(homeOrder = emptyList(), favorites = listOf(first, second)) }
        launchHome().use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.res(context.packageName, "grid")), 5000))
            onView(withId(R.id.grid)).perform(dragFirstToSecond())
            val homeChanged = withTimeoutOrNull(5000) { app.settings.settings.first { value -> value.homeOrder.take(2) == listOf(second, first) } }
            assertNotNull("Home drag was not persisted: ${app.settings.settings.first().homeOrder}", homeChanged)
            onView(withId(R.id.favorites)).perform(dragFirstToSecond())
            val dockChanged = withTimeoutOrNull(5000) { app.settings.settings.first { value -> value.favorites == listOf(second, first) } }
            assertNotNull("Dock drag was not persisted: ${app.settings.settings.first().favorites}", dockChanged)
        }
        app.settings.update { it.copy(homeOrder = emptyList(), favorites = emptyList()) }
    }
    @Test fun wallpaperImportIsBoundedAndRejectsInvalidImage() = runBlocking {
        val importer = WallpaperImporter(context)
        val source = File.createTempFile("test-source-", ".png", context.cacheDir)
        val invalid = File.createTempFile("test-invalid-", ".png", context.cacheDir)
        try {
            context.resources.openRawResource(R.drawable.tn_brand).use { input -> source.outputStream().use { input.copyTo(it) } }
            val imported = importer.importImage(android.net.Uri.fromFile(source))
            val output = File(context.filesDir, imported)
            val bitmap = android.graphics.BitmapFactory.decodeFile(output.path)
            assertNotNull(bitmap); assertTrue(bitmap.width <= 2048 && bitmap.height <= 2048); bitmap.recycle()
            invalid.writeText("not an image")
            assertTrue(runCatching { importer.importImage(android.net.Uri.fromFile(invalid)) }.isFailure)
            assertTrue(output.exists())
            importer.discard(imported)
            assertFalse(output.exists())
            assertTrue(context.cacheDir.listFiles()!!.none { it.name.startsWith("wallpaper-import-") })
        } finally { source.delete(); invalid.delete() }
    }
    @Test fun largeTextKeepsHomeCellsInsideGrid() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            device.executeShellCommand("settings put system font_scale 1.5")
            launchHome().use { scenario ->
                assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.res(context.packageName, "label")), 5000))
                scenario.onActivity { activity ->
                    val grid = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.grid)
                    assertTrue(grid.childCount > 0)
                    for (index in 0 until grid.childCount) {
                        assertTrue("Home row clipped at large font", grid.getChildAt(index).bottom <= grid.height - grid.paddingBottom)
                    }
                    assertTrue(activity.findViewById<View>(R.id.apps_button).height >= (72 * 1.5 * activity.resources.displayMetrics.density).toInt())
                }
                onView(withId(R.id.apps_button)).perform(click())
                onView(withId(R.id.home_button)).perform(click())
                onView(withId(R.id.grid)).check(matches(isDisplayed()))
            }
        } finally { device.executeShellCommand("settings put system font_scale 1.0") }
    }
    @Test fun suppliedFontsLoadWithVietnameseAcrossLightAndDarkScreens() = runBlocking {
        val app = context as LauncherApplication
        val originalTheme = app.settings.settings.first().theme
        val typography = LauncherTypography(context)
        listOf(R.font.tn_sf_body, R.font.tn_sf_heading, R.font.tn_new_york).forEach {
            assertNotNull("Supplied font did not load on API29", androidx.core.content.res.ResourcesCompat.getFont(context, it))
        }
        assertSame(android.graphics.Typeface.SANS_SERIF, LauncherTypography.loadOrFallback(context, 0))
        val phrases = "Tiếng Việt; Điều hướng; Cài đặt; Âm lượng; Kết nối; Bản đồ; Đường; 0123456789 ABC xyz"
        fun checkGlyphs(typeface: android.graphics.Typeface, sample: String = phrases) {
            val paint = android.graphics.Paint().apply { this.typeface = typeface; textSize = 32f }
            sample.codePoints().filter { !Character.isWhitespace(it) }.forEach {
                assertTrue("Missing glyph U+${it.toString(16)}", paint.hasGlyph(String(Character.toChars(it))))
            }
        }
        checkGlyphs(typography.body); checkGlyphs(typography.heading)
        checkGlyphs(typography.brand, "TN Launcher")
        try {
            for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                app.settings.update { it.copy(theme = theme) }
                launchHome().use { scenario ->
                    assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wait(
                        androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.res(context.packageName, "label")), 5000))
                    scenario.onActivity { activity ->
                        checkGlyphs(activity.findViewById<android.widget.TextView>(R.id.clock).typeface)
                        val grid = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.grid)
                        for (index in 0 until grid.childCount) {
                            val label = grid.getChildAt(index).findViewById<android.widget.TextView>(R.id.label)
                            checkGlyphs(label.typeface)
                            assertTrue(label.lineCount <= 2)
                        }
                    }
                    onView(withId(R.id.apps_button)).perform(click())
                    onView(withId(R.id.search)).perform(replaceText(phrases), closeSoftKeyboard())
                    scenario.onActivity { activity -> checkGlyphs(activity.findViewById<android.widget.TextView>(R.id.search).typeface) }
                    onView(withId(R.id.home_button)).perform(click())
                    onView(withId(R.id.settings_button)).perform(click())
                    onView(withText("Cài đặt TN Launcher")).check { view, error ->
                        if (error != null) throw error
                        checkGlyphs((view as android.widget.TextView).typeface)
                    }
                    onView(withText(if (theme == ThemeMode.DARK) "Tối" else "Sáng")).check(matches(isChecked()))
                }
            }
        } finally { app.settings.update { it.copy(theme = originalTheme) } }
    }
    private fun dragFirstToSecond() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(androidx.recyclerview.widget.RecyclerView::class.java)
        override fun getDescription() = "Drag first visible installed app onto its neighbor"
        override fun perform(controller: UiController, view: View) {
            val recycler = view as androidx.recyclerview.widget.RecyclerView
            val first = Rect(); val second = Rect()
            check(recycler.getChildAt(0).getGlobalVisibleRect(first))
            check(recycler.getChildAt(1).getGlobalVisibleRect(second))
            val started = SystemClock.uptimeMillis()
            // Cross the neighbor's center; ItemTouchHelper's default drop comparison is strict.
            val targetX = second.exactCenterX() + (second.exactCenterX() - first.exactCenterX()) * .15f
            val targetY = second.exactCenterY() + (second.exactCenterY() - first.exactCenterY()) * .15f
            fun inject(action: Int, x: Float, y: Float) {
                val event = MotionEvent.obtain(started, SystemClock.uptimeMillis(), action, x, y, 0)
                try { check(controller.injectMotionEvent(event)) } finally { event.recycle() }
            }
            inject(MotionEvent.ACTION_DOWN, first.exactCenterX(), first.exactCenterY())
            controller.loopMainThreadForAtLeast(android.view.ViewConfiguration.getLongPressTimeout().toLong() + 150)
            for (step in 1..20) {
                val ratio = step / 20f
                inject(MotionEvent.ACTION_MOVE, first.exactCenterX() + (targetX - first.exactCenterX()) * ratio,
                    first.exactCenterY() + (targetY - first.exactCenterY()) * ratio)
                controller.loopMainThreadForAtLeast(15)
            }
            inject(MotionEvent.ACTION_UP, targetX, targetY)
            controller.loopMainThreadForAtLeast(350)
        }
    }
    @Test fun pinMoveAndUnpinPersistThroughRecreation() = runBlocking {
        val app = context as LauncherApplication
        val fixturePackage = InstrumentationRegistry.getInstrumentation().context.packageName
        val fixtureId = ComponentName(fixturePackage, FixtureActivity::class.java.name).flattenToString()
        val seed = app.apps.load().first { !it.id.startsWith("$fixturePackage/") }
        app.settings.update { it.copy(favorites = listOf(seed.id)) }
        launchHome().use { scenario ->
            onView(withId(R.id.apps_button)).perform(click())
            onView(withId(R.id.search)).perform(replaceText("ung dung mau a"), closeSoftKeyboard())
            onView(allOf(withText("Ứng dụng mẫu A"), isDescendantOfA(withId(R.id.grid)))).perform(longClick())
            onView(withText("Ghim vào Dock")).check { view, error ->
                if (error != null) throw error
                assertTrue("Context menu touch target is below 56dp", view.height >= (56 * view.resources.displayMetrics.density).toInt())
            }
            onView(withText("Ghim vào Dock")).perform(click())
            withTimeout(5000) { app.settings.settings.first { it.favorites == listOf(seed.id, fixtureId) } }
            scenario.recreate()
            onView(allOf(withText("Ứng dụng mẫu A"), isDescendantOfA(withId(R.id.favorites)))).check(matches(isDisplayed()))
            onView(allOf(withText("Ứng dụng mẫu A"), isDescendantOfA(withId(R.id.favorites)))).perform(longClick())
            onView(withText("Di chuyển trước")).perform(click())
            withTimeout(5000) { app.settings.settings.first { it.favorites == listOf(fixtureId, seed.id) } }
            scenario.recreate()
            onView(allOf(withText("Ứng dụng mẫu A"), isDescendantOfA(withId(R.id.favorites)))).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val dock = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.favorites)
                val firstLabel = dock.getChildAt(0).findViewById<android.widget.TextView>(R.id.label)
                assertEquals("Ứng dụng mẫu A", firstLabel.text.toString())
            }
            onView(allOf(withText("Ứng dụng mẫu A"), isDescendantOfA(withId(R.id.favorites)))).perform(longClick())
            onView(withText("Bỏ ghim khỏi Dock")).perform(click())
            withTimeout(5000) { app.settings.settings.first { it.favorites == listOf(seed.id) } }
            scenario.recreate()
            onView(allOf(withText("Ứng dụng mẫu A"), isDescendantOfA(withId(R.id.favorites)))).check(doesNotExist())
            onView(allOf(withText(seed.label), isDescendantOfA(withId(R.id.favorites)))).check(matches(isDisplayed()))
            assertEquals(listOf(seed.id), app.settings.settings.first().favorites)
        }
    }
    @Test fun drawerSearchSettingsAndRecreation() {
        launchHome().use { scenario ->
            onView(withId(R.id.apps_button)).perform(click())
            onView(withId(R.id.search)).perform(replaceText("__no_app__"), closeSoftKeyboard())
            assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wait(
                androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.text(context.getString(R.string.empty_search))), 5000))
            onView(withText(R.string.empty_search)).check(matches(isDisplayed()))
            onView(withId(R.id.search)).perform(replaceText("settings"), closeSoftKeyboard())
            onView(withId(R.id.grid)).check(matches(isDisplayed()))
            onView(withId(R.id.home_button)).perform(click())
            onView(withId(R.id.search)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.settings_button)).perform(click())
            onView(withText("Tối")).perform(click())
            runBlocking { withTimeout(5000) { (context as LauncherApplication).settings.settings.first { it.theme == ThemeMode.DARK } } }
            scenario.recreate()
            onView(withText("Tối")).check(matches(isChecked()))
            onView(withText("Về màn hình chính")).perform(click())
            assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wait(
                androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.res(context.packageName, "grid")), 5000))
            onView(withId(R.id.grid)).check(matches(isDisplayed()))
        }
    }
    @Test fun externalAppKeepsForegroundUntilHome() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        launchHome().use {
            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.pkg("com.android.settings")), 5000))
            Thread.sleep(2000)
            assertEquals("com.android.settings", device.currentPackageName)
            device.pressHome()
            assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.pkg(context.packageName)), 5000))
        }
    }
}
