package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.util.Calendar;
import org.json.JSONObject;

/** "Ma semaine" widget: flame, streak and the mood of the last 7 days. */
public class NikkiWidget extends AppWidgetProvider {
    private static final int[] DOTS = {R.id.w_d0, R.id.w_d1, R.id.w_d2, R.id.w_d3, R.id.w_d4, R.id.w_d5, R.id.w_d6};
    private static final int[] LBLS = {R.id.w_l0, R.id.w_l1, R.id.w_l2, R.id.w_l3, R.id.w_l4, R.id.w_l5, R.id.w_l6};
    private static final String[] LETTERS = {"D", "L", "M", "M", "J", "V", "S"};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static int moodColor(Context c, String mood) {
        switch (mood) {
            case "super": return 0xFFFFB6C9;
            case "bien": return 0xFFFFD8A1;
            case "bof": return 0xFFC6E3C2;
            case "triste": return 0xFFB8D6F0;
            case "fatigue": return 0xFFD9C9F1;
            default: return 0xFFF5A9BC;
        }
    }

    static void render(Context c, AppWidgetManager m, int id) {
        JSONObject j = WidgetData.read(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_week);
        int s = WidgetData.streak(j);
        boolean today = WidgetData.writtenToday(j);
        v.setImageViewResource(R.id.w_flame, s > 0 ? (today ? R.drawable.w_flame_on : R.drawable.w_flame_dim) : R.drawable.w_flame_off);
        v.setTextViewText(R.id.w_streak, String.valueOf(s));
        v.setTextViewText(R.id.w_streak_lbl, s > 1 ? "jours d'affilée" : "jour d'affilée");
        v.setImageViewResource(R.id.w_mascot, WidgetData.mascotRes(c, j));

        JSONObject days = j.optJSONObject("days");
        int written = 0;
        for (int i = 0; i < 7; i++) {
            int back = 6 - i;
            JSONObject d = days == null ? null : days.optJSONObject(WidgetData.daysAgo(back));
            boolean w = d != null && d.optBoolean("w", false);
            if (w) written++;
            int color = w ? moodColor(c, d.optString("m", "")) : c.getColor(R.color.w_soft);
            v.setInt(DOTS[i], "setColorFilter", color);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -back);
            v.setTextViewText(LBLS[i], LETTERS[cal.get(Calendar.DAY_OF_WEEK) - 1]);
            v.setTextColor(LBLS[i], back == 0 ? c.getColor(R.color.w_accent) : c.getColor(R.color.w_muted));
        }
        String msg;
        if (today) msg = written >= 5 ? "Superbe semaine ✿ " + written + " pages sur 7" : "Page du jour écrite ✓ À demain !";
        else if (s > 0) msg = "Ta flamme t'attend ce soir ✿";
        else msg = "Une page aujourd'hui pour allumer ta flamme ✿";
        v.setTextViewText(R.id.w_msg, msg);
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "flamme", 11));
        m.updateAppWidget(id, v);
    }
}
