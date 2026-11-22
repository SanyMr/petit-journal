package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** The rare visitor walking in the garden today, or a riddle for the next one. */
public class VisitorWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static void render(Context c, AppWidgetManager m, int id) {
        JSONObject j = WidgetData.read(c);
        JSONObject vis = j.optJSONObject("vis");
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_visitor);
        v.setImageViewResource(R.id.w_mascot, WidgetData.mascotRes(c, j));
        v.setContentDescription(R.id.w_vis, Tr.s(c, R.string.w_visitor_desc));
        v.setTextViewText(R.id.w_k, Tr.s(c, R.string.w_visitor_today));
        if (vis == null) {
            v.setImageViewResource(R.id.w_vis, R.drawable.w_vis_grue_s);
            v.setTextViewText(R.id.w_name, "???");
            v.setTextViewText(R.id.w_msg, Tr.s(c, R.string.w_visitor_open));
        } else {
            boolean hint = "hint".equals(vis.optString("kind"));
            int res = c.getResources().getIdentifier("w_vis_" + vis.optString("id", "grue") + (hint ? "_s" : ""), "drawable", c.getPackageName());
            v.setImageViewResource(R.id.w_vis, res == 0 ? R.drawable.w_vis_grue : res);
            v.setTextViewText(R.id.w_k, Tr.s(c, hint ? R.string.w_visitor_next : R.string.w_visitor_today));
            v.setTextViewText(R.id.w_name, vis.optString("name", ""));
            v.setTextViewText(R.id.w_msg, vis.optString("msg", ""));
        }
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "jardin", 61));
        m.updateAppWidget(id, v);
    }
}
