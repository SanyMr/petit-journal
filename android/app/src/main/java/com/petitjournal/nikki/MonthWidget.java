package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.Locale;
import org.json.JSONObject;

/** The month as a flower bed: one flower per written page, in the colour of the mood. */
public class MonthWidget extends AppWidgetProvider {
    private static final int[] WD = {R.id.w_wd0, R.id.w_wd1, R.id.w_wd2, R.id.w_wd3, R.id.w_wd4, R.id.w_wd5, R.id.w_wd6};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static int idOf(Context c, String name) { return c.getResources().getIdentifier(name, "id", c.getPackageName()); }

    static void render(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_month);
        JSONObject days = WidgetData.read(c).optJSONObject("days");
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH), max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Monday first
        int count = 0;
        for (int n = 0; n < 42; n++) {
            int day = n - offset + 1;
            int img = idOf(c, "w_f" + n), cell = idOf(c, "w_c" + n);
            if (day < 1 || day > max) { v.setViewVisibility(img, View.INVISIBLE); v.setInt(cell, "setBackgroundResource", 0); continue; }
            String key = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, day);
            JSONObject d = days == null ? null : days.optJSONObject(key);
            boolean w = d != null && d.optBoolean("w", false);
            if (w) count++;
            v.setViewVisibility(img, View.VISIBLE);
            if (w) {
                v.setImageViewResource(img, R.drawable.w_flower);
                v.setInt(img, "setColorFilter", NikkiWidget.moodColor(c, d.optString("m", "")));
                v.setInt(img, "setImageAlpha", 255);
            } else {
                v.setImageViewResource(img, R.drawable.widget_dot);
                v.setInt(img, "setColorFilter", c.getColor(R.color.w_soft));
                v.setInt(img, "setImageAlpha", day > today ? 90 : 255);
            }
            v.setInt(cell, "setBackgroundResource", day == today ? R.drawable.w_ring : 0);
            if (day <= today) v.setOnClickPendingIntent(cell, WidgetData.open(c, "jour/" + key, 600 + n));
        }
        v.setTextViewText(R.id.w_title, Tr.s(c, R.string.w_month_title, new java.text.SimpleDateFormat("LLLL", Tr.locale(c)).format(cal.getTime())));
        v.setTextViewText(R.id.w_count, count + " ✿");
        String[] letters = Tr.arr(c, R.array.w_week_letters); // dimanche en premier
        for (int k = 0; k < 7; k++) v.setTextViewText(WD[k], letters[(k + 1) % 7]);
        WidgetData.seasonArt(c, v);
        m.updateAppWidget(id, v);
    }
}
