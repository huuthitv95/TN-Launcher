package vn.tnlauncher.app

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.test.runner.AndroidJUnitRunner
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Test APK only. Ordinary invocation delegates to AndroidJUnitRunner. The
 * explicit confirm argument selects an intentionally terminating probe instead
 * of JUnit. It backgrounds Home and terminates only its own PID; the host checks
 * restoration. No external kill, root, or SELinux-policy changes are needed.
 */
class LauncherTestRunner : AndroidJUnitRunner() {
    private var probeMode = false
    private var confirmation: String? = null

    override fun onCreate(arguments: Bundle?) {
        probeMode = arguments?.containsKey("confirm") == true
        confirmation = arguments?.getString("confirm")
        // AndroidJUnitRunner registers its lifecycle infrastructure and starts
        // instrumentation. Do not call start() a second time.
        super.onCreate(arguments)
    }

    override fun onStart() {
        if (!probeMode) {
            super.onStart()
            return
        }
        try {
            check(confirmation == "emulator-api29-only") { "Explicit emulator probe token missing" }
            check(Build.VERSION.SDK_INT == 29) { "Probe requires API 29" }
            fun property(name: String): String = ParcelFileDescriptor.AutoCloseInputStream(
                uiAutomation.executeShellCommand("getprop $name")
            ).bufferedReader().use { it.readText().trim() }
            check(property("ro.kernel.qemu") == "1" && property("ro.build.version.sdk") == "29") {
                "Probe requires an API 29 Android emulator"
            }
            check(targetContext.packageName == "vn.tnlauncher.app" &&
                Application.getProcessName() == "vn.tnlauncher.app") { "Unexpected target process" }
            check(context.packageName == "vn.tnlauncher.app.test") { "Unexpected probe APK" }

            val device = UiDevice.getInstance(this)
            targetContext.startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                component = ComponentName("vn.tnlauncher.app", "vn.tnlauncher.app.ui.HomeActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            check(device.wait(Until.hasObject(By.res("vn.tnlauncher.app", "grid")), 10000)) {
                "Home grid did not appear before process-death probe"
            }
            check(device.currentPackageName == "vn.tnlauncher.app") { "Home was not foreground" }

            targetContext.startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName("vn.tnlauncher.app.test", "vn.tnlauncher.app.FixtureActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            check(device.wait(Until.hasObject(By.pkg("vn.tnlauncher.app.test")), 10000)) {
                "Fixture did not appear before process-death probe"
            }
            check(device.currentPackageName == "vn.tnlauncher.app.test") { "Fixture was not foreground" }

            sendStatus(0, Bundle().apply {
                putInt("probe_pid", Process.myPid())
                putString("probe_phase", "background_before_self_termination")
                putBoolean("probe_expected_termination", true)
            })
            // The PID comes only from this process; never kill a supplied PID.
            Process.killProcess(Process.myPid())
        } catch (error: Exception) {
            finish(Activity.RESULT_CANCELED, Bundle().apply {
                putString("probe_error", error.message ?: error.javaClass.simpleName)
            })
        }
    }
}
