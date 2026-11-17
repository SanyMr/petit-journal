package com.petitjournal.nikki;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.util.Calendar;

/** 一期一会 La sagesse du jour : a Japanese proverb chosen from the recent moods, in 5 styles and 3 formats. */
public class WisdomWidget extends AppWidgetProvider {
    static final String TICK = "com.petitjournal.nikki.WISDOM_TICK";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) render(c, m, id);
        scheduleMidnight(c);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    @Override
    public void onDeleted(Context c, int[] ids) {
        SharedPreferences.Editor e = prefs(c).edit();
        for (int id : ids) e.remove("wis_" + id);
        e.apply();
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if (TICK.equals(i.getAction())) {
            refresh(c);
            scheduleMidnight(c);
            return;
        }
        super.onReceive(c, i);
    }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE); }

    /** "theme|format" of one widget (format: A = auto, S, M, L). */
    static String[] style(Context c, int id) {
        String def = prefs(c).getString("wis_default", "sumi|A");
        String[] s = prefs(c).getString("wis_" + id, def).split("\\|");
        if (s.length < 2) return new String[] {"sumi", "A"};
        return s;
    }

    static void setStyle(Context c, int id, String theme, String fmt) {
        SharedPreferences.Editor e = prefs(c).edit();
        String v = theme + "|" + fmt;
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) e.putString("wis_default", v);
        else e.putString("wis_" + id, v);
        e.apply();
    }

    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, WisdomWidget.class));
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) render(c, m, id);
    }

    /** Size of the widget on the home screen, in dp (portrait). */
    static float[] size(AppWidgetManager m, int id) {
        Bundle o = m.getAppWidgetOptions(id);
        float w = o == null ? 0 : o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        float h = o == null ? 0 : o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (w <= 0 || h <= 0) return new float[] {340, 160};
        return new float[] {w, h};
    }

    /** Bitmap for a style in a box of boxW × boxH dp. A forced format keeps its own proportions. */
    static Bitmap bitmap(Context c, String theme, String fmt, float boxW, float boxH) {
        String f = fmt;
        float w = boxW, h = boxH;
        if ("A".equals(fmt)) f = WisdomRender.autoFormat(boxW, boxH);
        else {
            float[] nat = WisdomRender.natural(fmt);
            float sc = Math.min(boxW / nat[0], boxH / nat[1]);
            if (sc > 1.25f) sc = Math.min(sc, 1.25f);
            w = nat[0] * Math.max(sc, .75f);
            h = nat[1] * Math.max(sc, .75f);
        }
        float density = c.getResources().getDisplayMetrics().density;
        return WisdomRender.render(c, WisdomData.today(c), theme, f, w, h, density);
    }

    static void render(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_wisdom);
        try {
            String[] st = style(c, id);
            float[] sz = size(m, id);
            v.setImageViewBitmap(R.id.w_img, bitmap(c, st[0], st[1], sz[0], sz[1]));
        } catch (Throwable ignored) { }
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "sagesse", 45));
        m.updateAppWidget(id, v);
    }

    /** A new proverb right after midnight, even if the journal is not opened. */
    static void scheduleMidnight(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 30);
            Intent i = new Intent(c, WisdomWidget.class).setAction(TICK);
            PendingIntent pi = PendingIntent.getBroadcast(c, 4501, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.set(AlarmManager.RTC, cal.getTimeInMillis(), pi);
        } catch (Exception ignored) { }
    }
}
