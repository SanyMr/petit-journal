package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** One habit that counts (glasses of water, pages…): each tap adds one. */
public class HabitCountWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    @Override
    public void onReceive(Context c, Intent i) {
        if (HabitsData.ACTION.equals(i.getAction())) { HabitsData.tap(c, i.getStringExtra("id")); return; }
        super.onReceive(c, i);
    }

    static JSONObject pick(JSONArray l) {
        for (int i = 0; i < l.length(); i++) { JSONObject h = l.optJSONObject(i); if (h != null && "count".equals(h.optString("type"))) return h; }
        return l.optJSONObject(0);
    }

    static void render(Context c, AppWidgetManager m, int wid) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_habit_count);
        JSONObject d = HabitsData.data(c);
        JSONObject h = pick(HabitsData.list(d));
        if (h == null) {
            v.setImageViewResource(R.id.w_ci, R.drawable.w_sel);
            v.setTextViewText(R.id.w_cv, "✿");
            v.setTextViewText(R.id.w_cl, Tr.s(c, R.string.w_hc_create));
            v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "habitudes", 48));
        } else {
            String id = h.optString("id");
            int val = HabitsData.value(c, d, id, WidgetData.today()), g = HabitsData.goal(h);
            boolean on = val >= g, count = "count".equals(h.optString("type"));
            Bitmap b = HabitsData.img(c, id, on);
            if (b != null) v.setImageViewBitmap(R.id.w_ci, b);
            v.setTextViewText(R.id.w_cv, count ? val + " / " + g : Tr.s(c, on ? R.string.w_hc_done : R.string.w_hc_todo));
            v.setTextColor(R.id.w_cv, HabitsData.color(h));
            v.setTextViewText(R.id.w_cl, on ? h.optString("name") + " ✿" : Tr.s(c, count ? R.string.w_hc_plus : R.string.w_hc_stamp));
            v.setOnClickPendingIntent(R.id.w_root, HabitsWidget.tapIntent(c, HabitCountWidget.class, id, 320));
        }
        m.updateAppWidget(wid, v);
    }
}
