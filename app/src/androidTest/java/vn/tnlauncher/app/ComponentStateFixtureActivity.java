package vn.tnlauncher.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

/** A test app may change its own component state; shell cannot do this on the API 29 image. */
public class ComponentStateFixtureActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (Build.VERSION.SDK_INT != 29 || !"ranchu".equals(Build.HARDWARE)
                    || !"vn.tnlauncher.app.test".equals(getPackageName())) return;
            int state = getIntent().getIntExtra("component_state", -1);
            if (state < PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    || state > PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) return;
            getPackageManager().setComponentEnabledSetting(
                new ComponentName(getPackageName(), "vn.tnlauncher.app.FixtureActivity"),
                state, PackageManager.DONT_KILL_APP);
        } finally { finish(); }
    }
}
