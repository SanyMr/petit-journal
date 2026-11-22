package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** The next tasks of the to-do list. */
public class TodoWidget extends AppWidgetProvider {
    private static final int[] ROWS = {R.id.w_t0, R.id.w_t1, R.id.w_t2, R.id.w_t3};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static void render(Context c, AppWidgetManager m, int id) {
        JSONObject j = WidgetData.read(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_todo);
        JSONArray list = j.optJSONArray("todo");
        int total = j.optInt("todoCount", list == null ? 0 : list.length());
        int shown = 0;
        for (int i = 0; i < ROWS.length; i++) {
            JSONObject t = list == null ? null : list.optJSONObject(i);
            if (t != null) {
                v.setTextViewText(ROWS[i], (t.optBoolean("s", false) ? "★  " : "○  ") + t.optString("t", ""));
                v.setViewVisibility(ROWS[i], View.VISIBLE);
                shown++;
            } else {
                v.setViewVisibility(ROWS[i], View.GONE);
            }
        }
        v.setTextViewText(R.id.w_count, total > 0 ? String.valueOf(total) : "");
        v.setTextViewText(R.id.w_hd, Tr.s(c, R.string.w_todo_head));
        v.setTextViewText(R.id.w_empty, Tr.s(c, R.string.w_todo_empty));
        v.setViewVisibility(R.id.w_empty, shown == 0 ? View.VISIBLE : View.GONE);
        int more = total - shown;
        v.setTextViewText(R.id.w_more, more > 0 ? Tr.p(c, R.plurals.w_todo_more, more, more) : "");
        v.setViewVisibility(R.id.w_more, more > 0 ? View.VISIBLE : View.GONE);
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "todo", 41));
        m.updateAppWidget(id, v);
    }
}
