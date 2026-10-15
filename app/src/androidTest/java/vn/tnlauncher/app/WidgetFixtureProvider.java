package vn.tnlauncher.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import vn.tnlauncher.app.test.R;

// Framework-only Java: this separate test-APK process cannot use the target APK's Kotlin runtime.
public class WidgetFixtureProvider extends AppWidgetProvider {
    private static final String REFRESH = "vn.tnlauncher.app.test.WIDGET_REFRESH";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, id, getClass());
    }
    @Override public void onDeleted(Context context, int[] ids) {
        SharedPreferences.Editor editor = context.getSharedPreferences("widget-fixture", Context.MODE_PRIVATE).edit();
        for (int id : ids) editor.remove("count-" + id);
        editor.apply();
    }
    @Override public void onReceive(Context context, Intent intent) {
        if (!REFRESH.equals(intent.getAction())) { super.onReceive(context, intent); return; }
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        int[] ownIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, getClass()));
        for (int ownId : ownIds) {
            if (ownId != id) continue;
            SharedPreferences prefs = context.getSharedPreferences("widget-fixture", Context.MODE_PRIVATE);
            prefs.edit().putInt("count-" + id, prefs.getInt("count-" + id, 0) + 1).apply();
            update(context, id, getClass());
            return;
        }
    }
    public static void update(Context context, int id, Class<? extends WidgetFixtureProvider> provider) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_fixture);
        int count = context.getSharedPreferences("widget-fixture", Context.MODE_PRIVATE).getInt("count-" + id, 0);
        views.setTextViewText(R.id.widget_fixture_text, "Tiện ích mẫu thật · " + count);
        views.setOnClickPendingIntent(R.id.widget_fixture_update, PendingIntent.getBroadcast(context, id,
            new Intent(context, provider).setAction(REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        views.setOnClickPendingIntent(R.id.widget_fixture_open, PendingIntent.getActivity(context, id,
            new Intent(context, FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        AppWidgetManager.getInstance(context).updateAppWidget(id, views);
    }
}
