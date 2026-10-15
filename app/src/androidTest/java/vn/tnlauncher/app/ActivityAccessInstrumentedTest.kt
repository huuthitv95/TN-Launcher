package vn.tnlauncher.app

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.tnlauncher.app.integration.ActivityAccess
import vn.tnlauncher.app.integration.Availability
import vn.tnlauncher.app.integration.LaunchResult
import vn.tnlauncher.app.integration.SafeActivityLauncher

@RunWith(AndroidJUnit4::class)
class ActivityAccessInstrumentedTest {
    @Test fun activityDisappearingDuringStartReturnsMissing() {
        assertEquals(LaunchResult.Missing, ActivityAccess.runStart { throw ActivityNotFoundException() })
    }

    @Test fun permissionChangingDuringStartReturnsDenied() {
        assertEquals(LaunchResult.Denied, ActivityAccess.runStart { throw SecurityException() })
    }

    @Test fun runtimeLaunchFailureReturnsSafeError() {
        val result = ActivityAccess.runStart { throw IllegalStateException("test-only-sensitive-detail") }
        assertTrue(result is LaunchResult.Error)
        assertFalse((result as LaunchResult.Error).reason.contains("test-only-sensitive-detail"))
    }

    @Test fun disabledFixtureIsUnavailableAndOriginalComponentStateIsRestored() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        // State mutations are limited to our test APK on an actual API 29 emulator.
        assertEquals("29", device.executeShellCommand("getprop ro.build.version.sdk").trim())
        assertEquals("1", device.executeShellCommand("getprop ro.kernel.qemu").trim())
        assertEquals("vn.tnlauncher.app.test", instrumentation.context.packageName)
        val component = ComponentName(instrumentation.context.packageName, FixtureActivity::class.java.name)
        val pm = instrumentation.targetContext.packageManager
        val launcher = SafeActivityLauncher(instrumentation.targetContext)
        val originalState = pm.getComponentEnabledSetting(component)

        fun setState(state: Int) {
            instrumentation.targetContext.startActivity(Intent()
                .setComponent(ComponentName(instrumentation.context.packageName, ComponentStateFixtureActivity::class.java.name))
                .putExtra("component_state", state).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val end = android.os.SystemClock.uptimeMillis() + 5000
            while (pm.getComponentEnabledSetting(component) != state && android.os.SystemClock.uptimeMillis() < end) {
                android.os.SystemClock.sleep(25)
            }
            assertEquals("The fixture must change its own real PackageManager component state", state, pm.getComponentEnabledSetting(component))
        }

        try {
            setState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            assertEquals(Availability.AVAILABLE, launcher.inspect(component))
            setState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
            assertEquals(Availability.COMPONENT_UNAVAILABLE, launcher.inspect(component))
            assertEquals(LaunchResult.Missing, launcher.launch(component))
            setState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            assertEquals(Availability.AVAILABLE, launcher.inspect(component))
        } finally {
            setState(originalState)
        }
    }
}
