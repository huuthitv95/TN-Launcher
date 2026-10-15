package vn.tnlauncher.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;

/** A test app may change its own component state; shell cannot do this on the API 29 image. */
public class ComponentStateFixtureActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntent(intent);
    }

    private void applyIntent(Intent intent) {
        try {
            if (Build.VERSION.SDK_INT != 29 || !"ranchu".equals(Build.HARDWARE)
                    || !"vn.tnlauncher.app.test".equals(getPackageName()) || intent == null) return;
            int state = intent.getIntExtra("component_state", -1);
            if (state < PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    || state > PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) return;
            PackageManager pm = getPackageManager();
            ComponentName component = new ComponentName(getPackageName(), "vn.tnlauncher.app.FixtureActivity");
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);
            ResultReceiver acknowledgement = intent.getParcelableExtra("component_state_result");
            if (acknowledgement != null) {
                Bundle result = new Bundle();
                result.putInt("component_state", pm.getComponentEnabledSetting(component));
                acknowledgement.send(RESULT_OK, result);
            }
        } finally { finish(); }
    }
}
