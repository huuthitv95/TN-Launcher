package vn.tnlauncher.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// Test APK only, independently executable without dependencies supplied by the target process.
public class WidgetFixtureConfigureActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        Intent result = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        setResult(RESULT_CANCELED, result);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setText("Cấu hình tiện ích mẫu"); title.setTextSize(26);
        root.addView(title);
        Button save = new Button(this);
        save.setText("Lưu tiện ích mẫu"); save.setAllCaps(false);
        save.setMinHeight(Math.round(56 * getResources().getDisplayMetrics().density));
        save.setOnClickListener(view -> {
            WidgetFixtureProvider.update(this, id, WidgetFixtureProvider.class);
            setResult(RESULT_OK, result); finish();
        });
        root.addView(save);
        Button cancel = new Button(this);
        cancel.setText("Hủy cấu hình mẫu"); cancel.setAllCaps(false);
        cancel.setMinHeight(Math.round(56 * getResources().getDisplayMetrics().density));
        cancel.setOnClickListener(view -> finish());
        root.addView(cancel);
        setContentView(root);
    }
}
