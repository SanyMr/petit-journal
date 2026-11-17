package com.petitjournal.nikki;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

/** Shared helpers for all home-screen widgets. Data is pushed by the journal (WidgetPlugin). */
final class WidgetData {
    static final String PREFS = "nikki_widget";

    static JSONObject read(Context c) {
        try {
            return new JSONObject(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static String dayKey(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    static String today() { return dayKey(System.currentTimeMillis()); }

    static String daysAgo(int n) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -n);
        return dayKey(cal.getTimeInMillis());
    }

    /** true when today's page has been written (data must be from today). */
    static boolean writtenToday(JSONObject j) {
        JSONObject days = j.optJSONObject("days");
        if (days == null) return false;
        JSONObject d = days.optJSONObject(today());
        return d != null && d.optBoolean("w", false);
    }

    /** Current streak, kept right after midnight even if the app was not opened. */
    static int streak(JSONObject j) {
        String day = j.optString("day", "");
        int s = j.optInt("streak", 0);
        if (day.equals(today())) return s;
        if (day.equals(daysAgo(1)) && j.optBoolean("today", false)) return s;
        return 0;
    }

    static int mascotRes(Context c, JSONObject j) {
        int res = c.getResources().getIdentifier("w_" + j.optString("mascot", "onigiri").toLowerCase(Locale.US), "drawable", c.getPackageName());
        return res == 0 ? R.drawable.w_onigiri : res;
    }

    static PendingIntent open(Context c, String view, int requestCode) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("petitjournal://" + view));
        i.setClass(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 5) return "Douce nuit ✿";
        if (h < 12) return "Bonjour ✿";
        if (h < 18) return "Bel après-midi ✿";
        if (h < 22) return "Bonne soirée ✿";
        return "Douce nuit ✿";
    }

    /** Japanese seasons: haru (spring), natsu (summer), aki (autumn), fuyu (winter). */
    static String season() {
        int mo = Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (mo >= 3 && mo <= 5) return "haru";
        if (mo >= 6 && mo <= 8) return "natsu";
        if (mo >= 9 && mo <= 11) return "aki";
        return "fuyu";
    }

    /** Seasonal corner art (sakura, seigaiha waves, momiji, snow) on a widget. */
    static void seasonArt(Context c, android.widget.RemoteViews v) {
        String s = season();
        v.setImageViewResource(R.id.w_art_br, c.getResources().getIdentifier("w_art_" + s + "_br", "drawable", c.getPackageName()));
        v.setImageViewResource(R.id.w_art_tr, c.getResources().getIdentifier("w_art_" + s + "_tr", "drawable", c.getPackageName()));
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : m.getAppWidgetIds(new ComponentName(c, NikkiWidget.class))) NikkiWidget.render(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, ClockWidget.class))) ClockWidget.render(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, PromptWidget.class))) PromptWidget.render(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, TodoWidget.class))) TodoWidget.render(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, MailWidget.class))) MailWidget.render(c, m, id);
    }

    private WidgetData() { }
}
