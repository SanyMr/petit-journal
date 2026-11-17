package com.petitjournal.nikki;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** Five mood faces: one tap notes today's mood without opening the journal. */
public class MoodWidget extends AppWidgetProvider {
    static final String ACTION = "com.petitjournal.nikki.MOOD";
    static final String[] IDS = {"super", "bien", "bof", "triste", "fatigue"};
    static final String[] LBL = {"Super", "Bien", "Bof", "Triste", "Fatigué·e"};
    private static final int[] CELLS = {R.id.w_m0, R.id.w_m1, R.id.w_m2, R.id.w_m3, R.id.w_m4};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    @Override
    public void onReceive(Context c, Intent i) {
        if (ACTION.equals(i.getAction())) {
            String mood = i.getStringExtra("mood");
            try {
                JSONObject p = new JSONObject();
                p.put("day", WidgetData.today());
                p.put("mood", mood);
                c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).edit().putString("mood_pending", p.toString()).apply();
            } catch (Exception ignored) { }
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            for (int id : m.getAppWidgetIds(new ComponentName(c, MoodWidget.class))) render(c, m, id);
            return;
        }
        super.onReceive(c, i);
    }

    /** Mood chosen today: a pending tap from the widget wins over the journal's data. */
    static String todayMood(Context c) {
        try {
            String raw = c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).getString("mood_pending", "");
            if (!raw.isEmpty()) {
                JSONObject p = new JSONObject(raw);
                if (WidgetData.today().equals(p.optString("day"))) return p.optString("mood", "");
            }
        } catch (Exception ignored) { }
        JSONObject days = WidgetData.read(c).optJSONObject("days");
        JSONObject d = days == null ? null : days.optJSONObject(WidgetData.today());
        return d == null ? "" : d.optString("m", "");
    }

    static void render(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_mood);
        String cur = todayMood(c);
        String msg = "Un toucher suffit ✿";
        for (int k = 0; k < IDS.length; k++) {
            boolean on = IDS[k].equals(cur);
            v.setInt(CELLS[k], "setBackgroundResource", on ? R.drawable.w_sel : 0);
            if (on) msg = "Noté : " + LBL[k] + " ✿ ta fleur du jour t'attend";
            Intent i = new Intent(c, MoodWidget.class).setAction(ACTION).putExtra("mood", IDS[k]);
            v.setOnClickPendingIntent(CELLS[k], PendingIntent.getBroadcast(c, 200 + k, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        v.setTextViewText(R.id.w_msg, msg);
        WidgetData.seasonArt(c, v);
        m.updateAppWidget(id, v);
    }
}
