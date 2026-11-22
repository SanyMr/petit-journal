package com.petitjournal.nikki;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/** All of today's habits: one tap puts a stamp, without opening the journal. */
public class HabitsWidget extends AppWidgetProvider {
    private static final int[] CELLS = {R.id.w_h0, R.id.w_h1, R.id.w_h2, R.id.w_h3, R.id.w_h4};
    private static final int[] IMGS = {R.id.w_hi0, R.id.w_hi1, R.id.w_hi2, R.id.w_hi3, R.id.w_hi4};
    private static final int[] LBLS = {R.id.w_hl0, R.id.w_hl1, R.id.w_hl2, R.id.w_hl3, R.id.w_hl4};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    @Override
    public void onReceive(Context c, Intent i) {
        if (HabitsData.ACTION.equals(i.getAction())) { HabitsData.tap(c, i.getStringExtra("id")); return; }
        super.onReceive(c, i);
    }

    static PendingIntent tapIntent(Context c, Class<?> cls, String id, int req) {
        Intent i = new Intent(c, cls).setAction(HabitsData.ACTION).putExtra("id", id);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void render(Context c, AppWidgetManager m, int wid) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_habits);
        JSONObject d = HabitsData.data(c);
        JSONArray l = HabitsData.list(d);
        String day = WidgetData.today();
        Calendar cal = Calendar.getInstance();
        int shown = 0, done = 0;
        for (int i = 0; i < l.length() && shown < CELLS.length; i++) {
            JSONObject h = l.optJSONObject(i);
            if (h == null || !HabitsData.planned(h, cal)) continue;
            String id = h.optString("id");
            int val = HabitsData.value(c, d, id, day), g = HabitsData.goal(h);
            boolean on = val >= g;
            if (on) done++;
            int k = shown++;
            v.setViewVisibility(CELLS[k], View.VISIBLE);
            Bitmap b = HabitsData.img(c, id, on);
            if (b != null) v.setImageViewBitmap(IMGS[k], b); else v.setImageViewResource(IMGS[k], R.drawable.w_sel);
            String name = h.optString("name", "");
            String lbl = "count".equals(h.optString("type")) && !on ? val + "/" + g : (name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name);
            v.setTextViewText(LBLS[k], lbl);
            v.setContentDescription(CELLS[k], on ? Tr.s(c, R.string.w_habit_done_desc, name) : name);
            v.setOnClickPendingIntent(CELLS[k], tapIntent(c, HabitsWidget.class, id, 300 + k));
        }
        for (int k = shown; k < CELLS.length; k++) v.setViewVisibility(CELLS[k], View.GONE);
        boolean none = l.length() == 0;
        v.setViewVisibility(R.id.w_empty, shown == 0 ? View.VISIBLE : View.GONE);
        v.setTextViewText(R.id.w_empty, Tr.s(c, none ? R.string.w_habits_none : R.string.w_habits_free));
        String cnt = HabitsData.rest(d) ? Tr.s(c, R.string.w_habits_rest) : shown == 0 ? "" : done + " / " + shown + (done == shown ? " ✿" : "");
        v.setTextViewText(R.id.w_count, cnt);
        v.setTextViewText(R.id.w_hd, Tr.s(c, R.string.w_habits_head));
        v.setOnClickPendingIntent(R.id.w_head, WidgetData.open(c, "habitudes", 46));
        v.setOnClickPendingIntent(R.id.w_empty, WidgetData.open(c, "habitudes", 47));
        WidgetData.seasonArt(c, v);
        m.updateAppWidget(wid, v);
    }
}
