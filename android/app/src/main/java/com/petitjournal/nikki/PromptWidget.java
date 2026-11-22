package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** Question of the day, refreshed each day even without opening the app. */
public class PromptWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static void render(Context c, AppWidgetManager m, int id) {
        JSONObject j = WidgetData.read(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_prompt);
        JSONObject prompts = j.optJSONObject("prompts");
        String q = prompts == null ? "" : prompts.optString(WidgetData.today(), "");
        if (q.isEmpty()) q = Tr.s(c, R.string.w_prompt_default);
        v.setTextViewText(R.id.w_hd, Tr.s(c, R.string.w_prompt_head));
        v.setTextViewText(R.id.w_prompt, q);
        v.setImageViewResource(R.id.w_mascot, WidgetData.mascotRes(c, j));
        boolean today = WidgetData.writtenToday(j);
        v.setTextViewText(R.id.w_btn, Tr.s(c, today ? R.string.w_prompt_done : R.string.w_prompt_write));
        v.setInt(R.id.w_btn, "setBackgroundResource", today ? R.drawable.widget_pill_soft : R.drawable.widget_pill);
        v.setTextColor(R.id.w_btn, today ? c.getColor(R.color.w_muted) : c.getColor(R.color.w_on_accent));
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "ecrire", 31));
        m.updateAppWidget(id, v);
    }
}
