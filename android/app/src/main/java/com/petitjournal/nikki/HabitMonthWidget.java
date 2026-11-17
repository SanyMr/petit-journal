package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** The month's stamps of one habit; a tap on the title shows the next habit. */
public class HabitMonthWidget extends AppWidgetProvider {
    static final String NEXT = "com.petitjournal.nikki.HABIT_NEXT";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    @Override
    public void onReceive(Context c, Intent i) {
        if (NEXT.equals(i.getAction())) {
            HabitsData.prefs(c).edit().putInt("hb_month_idx", HabitsData.prefs(c).getInt("hb_month_idx", 0) + 1).apply();
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            for (int id : m.getAppWidgetIds(new ComponentName(c, HabitMonthWidget.class))) render(c, m, id);
            return;
        }
        super.onReceive(c, i);
    }

    static void render(Context c, AppWidgetManager m, int wid) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_habit_month);
        JSONObject d = HabitsData.data(c);
        JSONArray l = HabitsData.list(d);
        Intent nx = new Intent(c, HabitMonthWidget.class).setAction(NEXT);
        v.setOnClickPendingIntent(R.id.w_mt, PendingIntent.getBroadcast(c, 330, nx, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.w_mg, WidgetData.open(c, "habitudes", 49));
        if (l.length() == 0) {
            v.setTextViewText(R.id.w_mt, "Mes habitudes");
            v.setTextViewText(R.id.w_ms, "Crée-les dans le journal ✿");
            v.setImageViewBitmap(R.id.w_mg, grid(c, null, 0xFFE09A3A, new HashSet<>()));
            m.updateAppWidget(wid, v);
            return;
        }
        int idx = ((HabitsData.prefs(c).getInt("hb_month_idx", 0) % l.length()) + l.length()) % l.length();
        JSONObject h = l.optJSONObject(idx);
        String id = h.optString("id");
        Calendar now = Calendar.getInstance();
        String ym = String.format(Locale.US, "%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
        Set<Integer> done = new HashSet<>();
        JSONObject mo = ym.equals(d.optString("ym")) ? d.optJSONObject("month") : null;
        JSONArray a = mo == null ? null : mo.optJSONArray(id);
        if (a != null) for (int i = 0; i < a.length(); i++) done.add(a.optInt(i));
        int today = now.get(Calendar.DAY_OF_MONTH);
        if (HabitsData.value(c, d, id, WidgetData.today()) >= HabitsData.goal(h)) done.add(today); else done.remove(today);
        v.setTextViewText(R.id.w_mt, h.optString("name") + (l.length() > 1 ? "  ›" : ""));
        String mn = new java.text.SimpleDateFormat("MMMM", Locale.FRENCH).format(now.getTime());
        v.setTextViewText(R.id.w_ms, mn + " · " + done.size() + " jour" + (done.size() > 1 ? "s" : "") + " ✿");
        v.setImageViewBitmap(R.id.w_mg, grid(c, h, HabitsData.color(h), done));
        m.updateAppWidget(wid, v);
    }

    /** 7 columns, Monday first: a coloured dot for each day done. */
    static Bitmap grid(Context c, JSONObject h, int col, Set<Integer> done) {
        float dp = c.getResources().getDisplayMetrics().density;
        int cell = Math.round(16 * dp), r = Math.round(5.5f * dp), w = cell * 7, hgt = cell * 6;
        Bitmap b = Bitmap.createBitmap(w, hgt, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH), max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int off = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (int day = 1; day <= max; day++) {
            int pos = off + day - 1, cx = (pos % 7) * cell + cell / 2, cy = (pos / 7) * cell + cell / 2;
            cal.set(Calendar.DAY_OF_MONTH, day);
            boolean plan = h == null || HabitsData.planned(h, cal);
            if (done.contains(day)) p.setColor(col);
            else if (day > today) p.setColor(0xFFF1ECE4);
            else p.setColor(plan ? 0xFFF6E7CF : 0xFFFAF5EE);
            cv.drawCircle(cx, cy, r, p);
            if (day == today) {
                Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
                s.setStyle(Paint.Style.STROKE); s.setStrokeWidth(1.6f * dp); s.setColor(col);
                cv.drawCircle(cx, cy, r + 2 * dp, s);
            }
        }
        return b;
    }
}
