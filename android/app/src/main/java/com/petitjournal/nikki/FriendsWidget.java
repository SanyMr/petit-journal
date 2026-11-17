package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** Friends' flames, who wrote today, and the last petal or wish received in the garden. */
public class FriendsWidget extends AppWidgetProvider {
    private static final int[] ROWS = {R.id.w_r0, R.id.w_r1, R.id.w_r2, R.id.w_r3};
    private static final int[] MAS = {R.id.w_m0, R.id.w_m1, R.id.w_m2, R.id.w_m3};
    private static final int[] NAM = {R.id.w_n0, R.id.w_n1, R.id.w_n2, R.id.w_n3};
    private static final int[] FLA = {R.id.w_fl0, R.id.w_fl1, R.id.w_fl2, R.id.w_fl3};
    private static final int[] STR = {R.id.w_s0, R.id.w_s1, R.id.w_s2, R.id.w_s3};
    private static final int[] TOD = {R.id.w_t0, R.id.w_t1, R.id.w_t2, R.id.w_t3};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        MailWorker.schedule(c);
        for (int id : ids) render(c, m, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static int mascot(Context c, String id) {
        int res = c.getResources().getIdentifier("w_" + id, "drawable", c.getPackageName());
        return res == 0 ? R.drawable.w_onigiri : res;
    }

    static void render(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_friends);
        JSONObject mail;
        try { mail = new JSONObject(c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).getString("mail", "{}")); } catch (Exception e) { mail = new JSONObject(); }
        JSONArray band = mail.optJSONArray("band");
        int n = band == null ? 0 : band.length();
        String empty = "";
        if (!mail.optBoolean("signedIn", false)) empty = "Connecte-toi dans l'onglet Amis pour voir tes amies ici ✿";
        else if (n == 0) empty = "Invite une amie avec ton QR code ✿";
        v.setTextViewText(R.id.w_empty, empty);
        v.setViewVisibility(R.id.w_empty, empty.isEmpty() ? View.GONE : View.VISIBLE);
        for (int k = 0; k < 4; k++) {
            if (k >= n) { v.setViewVisibility(ROWS[k], View.GONE); continue; }
            JSONObject b = band.optJSONObject(k);
            v.setViewVisibility(ROWS[k], View.VISIBLE);
            v.setImageViewResource(MAS[k], mascot(c, b.optString("m", "onigiri")));
            v.setTextViewText(NAM[k], b.optString("n", "?"));
            int s = b.optInt("s", 0);
            v.setImageViewResource(FLA[k], s > 0 ? (b.optBoolean("w") ? R.drawable.w_flame_on : R.drawable.w_flame_dim) : R.drawable.w_flame_off);
            v.setTextViewText(STR[k], String.valueOf(s));
            v.setTextViewText(TOD[k], b.optBoolean("w") ? "✓ a écrit" : "");
        }
        JSONObject g = mail.optJSONObject("gift");
        String gift = "";
        if (g != null) gift = g.optBoolean("wish") ? "🎐 " + g.optString("name") + " a accroché un vœu dans ton jardin" : "🌸 " + g.optString("name") + " t'a laissé un pétale";
        v.setTextViewText(R.id.w_gift, gift);
        v.setViewVisibility(R.id.w_gift, gift.isEmpty() ? View.GONE : View.VISIBLE);
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "amis", 71));
        m.updateAppWidget(id, v);
    }
}
