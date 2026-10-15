package vn.tnlauncher.app

import androidx.lifecycle.ViewModelStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import vn.tnlauncher.app.data.LauncherApp
import vn.tnlauncher.app.data.LauncherAppRepository
import vn.tnlauncher.app.data.LauncherSettings
import vn.tnlauncher.app.data.LauncherSettingsRepository
import vn.tnlauncher.app.data.ThemeMode
import vn.tnlauncher.app.integration.Availability
import vn.tnlauncher.app.integration.Capability
import vn.tnlauncher.app.integration.LaunchResult
import vn.tnlauncher.app.integration.WincavnIntegration
import vn.tnlauncher.app.ui.LauncherViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        Dispatchers.resetMain()
    }

    @Test fun loadedAppsUseSavedOrderAndAppendNewAppsWithoutErasingMissingEntries() = runTest {
        val preferences = FakeSettings(LauncherSettings(homeOrder = listOf("missing", "b", "b")))
        val model = model(FakeApps(listOf(app("a"), app("b"), app("c"))), preferences)

        model.state.first { !it.loading }

        assertEquals(listOf("b", "a", "c"), model.orderedApps().map { it.id })
        assertEquals(listOf("missing", "b", "b"), model.state.value.settings.homeOrder)
        assertNull(model.state.value.error)
    }

    @Test fun favoriteChangesAreUniqueBoundedAndDoNotRequireAppCurrentlyInstalled() = runTest {
        val preferences = FakeSettings(LauncherSettings(favorites = listOf("temporarily-missing")))
        val model = model(preferences = preferences)
        model.state.first { !it.loading }

        (1..7).forEach { model.pin("app$it") }
        model.pin("app1")
        advanceUntilIdle()

        assertEquals(listOf("temporarily-missing", "app1", "app2", "app3", "app4", "app5"),
            model.state.value.settings.favorites)
        model.unpin("app3")
        model.pin("app6")
        advanceUntilIdle()
        assertEquals(listOf("temporarily-missing", "app1", "app2", "app4", "app5", "app6"),
            model.state.value.settings.favorites)
    }

    @Test fun dockReorderDeduplicatesAndLimitsFavorites() = runTest {
        val model = model()
        model.state.first { !it.loading }

        model.reorder(listOf("c", "b", "c", "a", "d", "e", "f", "g"), dock = true)
        advanceUntilIdle()

        assertEquals(listOf("c", "b", "a", "d", "e", "f"), model.state.value.settings.favorites)
    }

    @Test fun visibleHomeReorderPreservesSavedEntriesForAbsentApps() = runTest {
        val preferences = FakeSettings(LauncherSettings(homeOrder = listOf("missing", "a", "b")))
        val model = model(FakeApps(listOf(app("a"), app("b"), app("c"))), preferences)
        model.state.first { !it.loading }

        model.reorder(listOf("c", "b", "a"), dock = false)
        advanceUntilIdle()

        assertEquals(listOf("missing", "c", "b", "a"), model.state.value.settings.homeOrder)
        assertEquals(listOf("c", "b", "a"), model.orderedApps().map { it.id })
    }

    @Test fun menuMoveAddsNewAppsAndLeavesOtherSavedEntriesIntact() = runTest {
        val preferences = FakeSettings(LauncherSettings(homeOrder = listOf("missing", "a", "b")))
        val model = model(FakeApps(listOf(app("a"), app("b"), app("c"))), preferences)
        model.state.first { !it.loading }

        model.move("c", -1, dock = false)
        advanceUntilIdle()

        assertEquals(listOf("missing", "a", "c", "b"), model.state.value.settings.homeOrder)
        model.move("unknown", 1, dock = false)
        advanceUntilIdle()
        assertEquals(listOf("missing", "a", "c", "b"), model.state.value.settings.homeOrder)
    }

    @Test fun loadFailureEndsLoadingAndSuccessfulRetryClearsError() = runTest {
        val apps = FakeApps(listOf(app("a"))).apply { loadFailure = IOException("unavailable") }
        val model = model(apps)

        model.state.first { !it.loading }
        assertNotNull(model.state.value.error)
        assertTrue(model.state.value.apps.isEmpty())

        apps.loadFailure = null
        model.refresh()
        // Wait for the refreshed app rather than the previous non-loading state.
        model.state.first { !it.loading && it.apps.isNotEmpty() }

        assertEquals(listOf("a"), model.state.value.apps.map { it.id })
        assertNull(model.state.value.error)
        assertEquals(2, apps.loadCount)
    }

    @Test fun refreshFailureKeepsPreviouslyDiscoveredAppsAndLayout() = runTest {
        val apps = FakeApps(listOf(app("a"), app("b")))
        val preferences = FakeSettings(LauncherSettings(homeOrder = listOf("b", "a")))
        val model = model(apps, preferences)
        model.state.first { !it.loading }

        apps.loadFailure = IOException("package query failed")
        model.refresh()
        model.state.first { !it.loading && it.error != null }

        assertEquals(listOf("b", "a"), model.orderedApps().map { it.id })
        assertEquals(listOf("b", "a"), model.state.value.settings.homeOrder)
    }

    @Test fun settingsWriteFailurePreservesPreviousStateAndInvokesFailureCallback() = runTest {
        val initial = LauncherSettings(theme = ThemeMode.DARK, favorites = listOf("a"))
        val preferences = FakeSettings(initial).apply { failWrites = true }
        val model = model(preferences = preferences)
        model.state.first { !it.loading }
        var failures = 0

        model.change(onFailure = { failures++ }) { it.copy(theme = ThemeMode.LIGHT, favorites = listOf("b")) }
        advanceUntilIdle()

        assertEquals(initial, model.state.value.settings)
        assertNotNull(model.state.value.error)
        assertEquals(1, failures)
    }

    @Test fun newSettingsEmissionUpdatesStateWithoutReloadingApps() = runTest {
        val apps = FakeApps(listOf(app("a")))
        val preferences = FakeSettings()
        val model = model(apps, preferences)
        model.state.first { !it.loading }
        val restored = LauncherSettings(theme = ThemeMode.LIGHT, reducedMotion = true,
            wallpaper = "wallpaper.png", favorites = listOf("a"))

        preferences.settings.value = restored
        advanceUntilIdle()

        assertEquals(restored, model.state.value.settings)
        assertEquals(1, apps.loadCount)
    }

    @Test fun vendorAbsentIsVisibleAsUnsupportedWhileHomeAndRealAppLaunchRemainUsable() = runTest {
        val vendor = FakeVendor()
        val apps = FakeApps(listOf(app("a")))
        val model = model(apps, vendor = vendor)
        model.state.first { !it.loading }

        assertEquals(Availability.PACKAGE_MISSING, model.state.value.capabilities.single().availability)
        assertEquals(LaunchResult.Missing, model.openVendor("camera"))
        assertEquals(LaunchResult.Opened, model.launch("a"))
        assertEquals("a", apps.lastLaunch)
        assertNull(model.state.value.error)
    }

    @Test fun appAndVendorLaunchFailuresKeepTheirTypedResults() = runTest {
        val apps = FakeApps().apply { launchResult = LaunchResult.Denied }
        val vendor = FakeVendor().apply { openResult = LaunchResult.Error("unavailable") }
        val model = model(apps, vendor = vendor)
        model.state.first { !it.loading }

        assertEquals(LaunchResult.Denied, model.launch("restricted"))
        assertEquals(LaunchResult.Error("unavailable"), model.openVendor("camera"))
        assertFalse(model.state.value.loading)
    }

    private fun model(apps: FakeApps = FakeApps(), preferences: FakeSettings = FakeSettings(),
        vendor: FakeVendor = FakeVendor()): LauncherViewModel {
        val model = LauncherViewModel(apps, preferences, vendor)
        stores += ViewModelStore().apply { put("launcher", model) }
        return model
    }

    private fun app(id: String) = LauncherApp(id, id, null)

    private class FakeApps(var installed: List<LauncherApp> = emptyList()) : LauncherAppRepository {
        var loadFailure: Exception? = null
        var loadCount = 0
        var launchResult: LaunchResult = LaunchResult.Opened
        var lastLaunch: String? = null
        override suspend fun load(): List<LauncherApp> {
            loadCount++
            loadFailure?.let { throw it }
            return installed
        }
        override fun launch(id: String): LaunchResult {
            lastLaunch = id
            return launchResult
        }
    }

    private class FakeSettings(initial: LauncherSettings = LauncherSettings()) : LauncherSettingsRepository {
        override val settings = MutableStateFlow(initial)
        var failWrites = false
        override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
            if (failWrites) throw IOException("write failed")
            settings.value = transform(settings.value)
        }
    }

    private class FakeVendor : WincavnIntegration {
        var openResult: LaunchResult = LaunchResult.Missing
        override fun inspect() = listOf(Capability("camera", "Camera", Availability.PACKAGE_MISSING,
            null, "Test double: original vendor app absent"))
        override fun open(id: String) = openResult
    }
}
