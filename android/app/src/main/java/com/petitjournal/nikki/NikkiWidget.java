package com.petitjournal.nikki;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

/** Home-screen widget: mascot, flame (streak), today's page status, tasks or capsules. */
public class NikkiWidget extends AppWidgetProvider {
    static final String PREFS = "nikki_widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, manager, id);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, NikkiWidget.class));
        for (int id : ids) update(context, manager, id);
    }

    private static String dayKey(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    static void update(Context c, AppWidgetManager manager, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.nikki_widget);
        String raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", "{}");
        int streak = 0, todo = 0, caps = 0;
        boolean wroteThatDay = false;
        String mascot = "onigiri", day = "";
        try {
            JSONObject j = new JSONObject(raw);
            streak = j.optInt("streak", 0);
            todo = j.optInt("todo", 0);
            caps = j.optInt("caps", 0);
            wroteThatDay = j.optBoolean("today", false);
            mascot = j.optString("mascot", "onigiri");
            day = j.optString("day", "");
        } catch (Exception ignored) { }

        long now = System.currentTimeMillis();
        String today = dayKey(now), yesterday = dayKey(now - 86400000L);
        boolean writtenToday = today.equals(day) && wroteThatDay;
        int shown = streak;
        if (!today.equals(day) && !(yesterday.equals(day) && wroteThatDay)) shown = 0;

        int mascotRes = c.getResources().getIdentifier("w_" + mascot.toLowerCase(Locale.US), "drawable", c.getPackageName());
        if (mascotRes == 0) mascotRes = R.drawable.w_onigiri;
        v.setImageViewResource(R.id.w_mascot, mascotRes);
        v.setImageViewResource(R.id.w_flame, shown > 0 ? (writtenToday ? R.drawable.w_flame_on : R.drawable.w_flame_dim) : R.drawable.w_flame_off);
        v.setTextViewText(R.id.w_streak, String.valueOf(shown));
        v.setTextViewText(R.id.w_streak_lbl, shown > 1 ? "jours d'affilée" : "jour d'affilée");
        v.setTextViewText(R.id.w_msg, writtenToday ? "Page du jour écrite ✓ À demain !" : "Ta page du jour t'attend ✿");

        String extra = "";
        if (todo > 0) extra = todo + (todo > 1 ? " tâches à faire" : " tâche à faire");
        else if (caps > 0) extra = caps + (caps > 1 ? " capsules à ouvrir" : " capsule à ouvrir");
        v.setTextViewText(R.id.w_extra, extra);
        v.setViewVisibility(R.id.w_extra, extra.isEmpty() ? View.GONE : View.VISIBLE);

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.w_root, pi);
        manager.updateAppWidget(id, v);
    }
}
