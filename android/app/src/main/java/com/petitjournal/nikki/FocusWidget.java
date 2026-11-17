package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** Starts a focus session in the garden; shows a live countdown while it runs. */
public class FocusWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static void render(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_focus);
        JSONObject p;
        try { p = new JSONObject(c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).getString("pomo", "{}")); } catch (Exception e) { p = new JSONObject(); }
        long end = p.optLong("end", 0), now = System.currentTimeMillis();
        boolean paused = p.optBoolean("paused", false), running = p.optBoolean("on", false) && (paused || end > now);
        int n = WidgetData.today().equals(p.optString("day")) ? p.optInt("n", 0) : 0;
        StringBuilder dango = new StringBuilder();
        for (int k = 0; k < Math.min(n, 6); k++) dango.append("🍡");
        if (running) {
            boolean focus = "focus".equals(p.optString("phase"));
            v.setViewVisibility(R.id.w_btns, View.GONE);
            if (paused) {
                v.setViewVisibility(R.id.w_chrono, View.GONE);
                v.setViewVisibility(R.id.w_big, View.VISIBLE);
                v.setTextViewText(R.id.w_big, "⏸ En pause · " + Math.max(1, p.optLong("left", 0) / 60000) + " min");
            } else {
                v.setViewVisibility(R.id.w_chrono, View.VISIBLE);
                v.setViewVisibility(R.id.w_big, View.GONE);
                v.setChronometer(R.id.w_chrono, SystemClock.elapsedRealtime() + (end - now), null, true);
                if (Build.VERSION.SDK_INT >= 24) v.setChronometerCountDown(R.id.w_chrono, true);
            }
            String label = p.optString("label", "");
            v.setTextViewText(R.id.w_line, focus ? (label.isEmpty() ? "Concentration · une chose à la fois ✿" : "✎ " + label) : "☕ Pause · respire un peu ✿");
            v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "jardin", 51));
        } else {
            v.setViewVisibility(R.id.w_chrono, View.GONE);
            v.setViewVisibility(R.id.w_big, View.VISIBLE);
            v.setViewVisibility(R.id.w_btns, View.VISIBLE);
            boolean done = p.optBoolean("done", false) && now - p.optLong("at", 0) < 3600000;
            v.setTextViewText(R.id.w_big, done ? "Session terminée ✿" : "Une session au calme ✿");
            v.setTextViewText(R.id.w_line, n > 0 ? dango + "  " + n + " session" + (n > 1 ? "s" : "") + " aujourd'hui" : "Le jardin veille pendant que tu travailles");
            v.setOnClickPendingIntent(R.id.w_b15, WidgetData.open(c, "pomo/15", 52));
            v.setOnClickPendingIntent(R.id.w_b25, WidgetData.open(c, "pomo/25", 53));
            v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "jardin", 51));
        }
        WidgetData.seasonArt(c, v);
        m.updateAppWidget(id, v);
    }
}
