package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** "Boîte aux lettres": little notes received from friends + who wrote today. */
public class MailWidget extends AppWidgetProvider {
    private static final int[] MASC = {R.id.w_b0, R.id.w_b1, R.id.w_b2, R.id.w_b3, R.id.w_b4};
    private static final int[] FLAME = {R.id.w_bf0, R.id.w_bf1, R.id.w_bf2, R.id.w_bf3, R.id.w_bf4};
    private static final int[] CELL = {R.id.w_c0, R.id.w_c1, R.id.w_c2, R.id.w_c3, R.id.w_c4};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) render(c, m, id);
        MailWorker.schedule(c);
        MailWorker.checkNow(c);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    /** The freshest of what the app pushed and what the background check found. */
    static JSONObject mail(Context c) {
        JSONObject fromApp = WidgetData.read(c).optJSONObject("mail");
        JSONObject fromWorker = null;
        try { fromWorker = new JSONObject(c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).getString("mail", "{}")); } catch (Exception ignored) { }
        if (fromApp == null) return fromWorker == null ? new JSONObject() : fromWorker;
        if (fromWorker == null) return fromApp;
        return fromWorker.optLong("at", 0) > fromApp.optLong("at", 0) ? fromWorker : fromApp;
    }

    /** Japanese seasons: haru (spring), natsu (summer), aki (autumn), fuyu (winter). */
    static String season() {
        int mo = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1;
        if (mo >= 3 && mo <= 5) return "haru";
        if (mo >= 6 && mo <= 8) return "natsu";
        if (mo >= 9 && mo <= 11) return "aki";
        return "fuyu";
    }

    static int mascot(Context c, String m) {
        int res = c.getResources().getIdentifier("w_" + m, "drawable", c.getPackageName());
        return res == 0 ? R.drawable.w_onigiri : res;
    }

    static void render(Context c, AppWidgetManager m, int id) {
        JSONObject mail = mail(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_mail);
        int count = mail.optInt("count", 0);
        boolean signed = mail.optBoolean("signedIn", false);
        v.setImageViewResource(R.id.w_env, count > 0 ? R.drawable.w_env_letter : R.drawable.w_env_empty);
        v.setViewVisibility(R.id.w_badge, count > 0 ? View.VISIBLE : View.GONE);
        v.setTextViewText(R.id.w_badge, count > 9 ? "9+" : String.valueOf(count));
        JSONObject last = mail.optJSONObject("last");
        if (!signed) {
            v.setTextViewText(R.id.w_title, "Écris avec tes amis");
            v.setTextViewText(R.id.w_from, "Connecte-toi dans l'onglet Amis ✿");
            v.setViewVisibility(R.id.w_from_m, View.GONE);
        } else if (count > 0 && last != null) {
            v.setTextViewText(R.id.w_title, count == 1 ? "1 petit mot pour toi" : count + " petits mots pour toi");
            v.setTextViewText(R.id.w_from, last.optString("name", "") + " : " + last.optString("text", ""));
            v.setImageViewResource(R.id.w_from_m, mascot(c, last.optString("mascot", "onigiri")));
            v.setViewVisibility(R.id.w_from_m, View.VISIBLE);
        } else {
            v.setTextViewText(R.id.w_title, "Pas de lettre pour l'instant");
            v.setTextViewText(R.id.w_from, "Et si tu envoyais un petit mot ? ✿");
            v.setViewVisibility(R.id.w_from_m, View.GONE);
        }
        JSONArray band = mail.optJSONArray("band");
        int n = band == null ? 0 : Math.min(band.length(), MASC.length), wrote = 0;
        for (int i = 0; i < MASC.length; i++) {
            JSONObject b = i < n ? band.optJSONObject(i) : null;
            if (b == null) { v.setViewVisibility(CELL[i], View.GONE); continue; }
            v.setViewVisibility(CELL[i], View.VISIBLE);
            v.setImageViewResource(MASC[i], mascot(c, b.optString("m", "onigiri")));
            boolean w = b.optBoolean("w", false);
            if (w) wrote++;
            v.setViewVisibility(FLAME[i], w ? View.VISIBLE : View.GONE);
            v.setInt(MASC[i], "setImageAlpha", w ? 255 : 120);
        }
        int friends = mail.optInt("friends", n);
        if (!signed || friends == 0) {
            v.setTextViewText(R.id.w_band_lbl, signed ? "Ma bande · ajoute des amis avec ton code ✿" : "");
            v.setViewVisibility(R.id.w_band_lbl, signed ? View.VISIBLE : View.GONE);
        } else {
            v.setViewVisibility(R.id.w_band_lbl, View.VISIBLE);
            v.setTextViewText(R.id.w_band_lbl, "Ma bande · " + wrote + "/" + Math.min(friends, n) + (wrote > 1 ? " ont écrit" : " a écrit") + " aujourd'hui");
        }
        String season = season();
        v.setImageViewResource(R.id.w_art_br, c.getResources().getIdentifier("w_art_" + season + "_br", "drawable", c.getPackageName()));
        v.setImageViewResource(R.id.w_art_tr, c.getResources().getIdentifier("w_art_" + season + "_tr", "drawable", c.getPackageName()));
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "amis", 40));
        m.updateAppWidget(id, v);
    }
}
