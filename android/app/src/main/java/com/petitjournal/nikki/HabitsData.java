package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Habits shown on the home screen. The journal pushes the habits ("hb_data") and their stamp pictures;
 * taps on a widget are kept as pending changes ("hb_ops") until the journal picks them up.
 */
final class HabitsData {
    static final String ACTION = "com.petitjournal.nikki.HABIT";

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE); }

    static JSONObject data(Context c) {
        try { return new JSONObject(prefs(c).getString("hb_data", "{}")); } catch (Exception e) { return new JSONObject(); }
    }

    static JSONArray ops(Context c) {
        try { return new JSONArray(prefs(c).getString("hb_ops", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    static JSONArray list(JSONObject d) { JSONArray a = d.optJSONArray("list"); return a == null ? new JSONArray() : a; }

    static int goal(JSONObject h) { return "count".equals(h.optString("type")) ? Math.max(1, h.optInt("goal", 1)) : 1; }

    /** Today's value of a habit: the last tap on a widget, else what the journal said. */
    static int value(Context c, JSONObject d, String id, String day) {
        JSONArray o = ops(c);
        for (int i = o.length() - 1; i >= 0; i--) {
            JSONObject x = o.optJSONObject(i);
            if (x != null && id.equals(x.optString("id")) && day.equals(x.optString("d"))) return x.optInt("v", 0);
        }
        if (!day.equals(d.optString("day"))) return 0;
        JSONObject v = d.optJSONObject("vals");
        return v == null ? 0 : v.optInt(id, 0);
    }

    static int weekday(Calendar cal) { return cal.get(Calendar.DAY_OF_WEEK) - 1; } // 0 = dimanche, comme le journal

    static boolean planned(JSONObject h, Calendar cal) {
        JSONArray days = h.optJSONArray("days");
        if (days == null) return true;
        int wd = weekday(cal);
        for (int i = 0; i < days.length(); i++) if (days.optInt(i, -1) == wd) return true;
        return false;
    }

    static boolean rest(JSONObject d) { return WidgetData.today().equals(d.optString("day")) && d.optBoolean("rest", false); }

    /** One tap: a stamp (or its removal), or +1 for a habit that counts. */
    static void tap(Context c, String id) {
        JSONObject d = data(c);
        JSONArray l = list(d);
        String day = WidgetData.today();
        for (int i = 0; i < l.length(); i++) {
            JSONObject h = l.optJSONObject(i);
            if (h == null || !id.equals(h.optString("id"))) continue;
            int v = value(c, d, id, day), g = goal(h), nv;
            if ("count".equals(h.optString("type"))) nv = v >= g ? v : v + 1; else nv = v > 0 ? 0 : 1;
            if (nv == v) return;
            try {
                JSONArray o = ops(c);
                JSONObject x = new JSONObject();
                x.put("d", day); x.put("id", id); x.put("v", nv);
                o.put(x);
                prefs(c).edit().putString("hb_ops", o.toString()).apply();
            } catch (Exception ignored) { }
            refresh(c);
            return;
        }
    }

    static Bitmap img(Context c, String id, boolean on) {
        File f = new File(new File(c.getFilesDir(), "hb"), id + (on ? "_on.png" : "_off.png"));
        if (!f.exists()) return null;
        try { return BitmapFactory.decodeFile(f.getAbsolutePath()); } catch (Exception e) { return null; }
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : m.getAppWidgetIds(new ComponentName(c, HabitsWidget.class))) HabitsWidget.render(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, HabitCountWidget.class))) HabitCountWidget.render(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, HabitMonthWidget.class))) HabitMonthWidget.render(c, m, id);
    }

    static int color(JSONObject h) {
        try { return android.graphics.Color.parseColor(h.optString("color", "#d9708d")); } catch (Exception e) { return 0xFFD9708D; }
    }

    private HabitsData() { }
}
