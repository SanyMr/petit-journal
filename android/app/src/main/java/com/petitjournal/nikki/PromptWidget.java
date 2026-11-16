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
        if (q.isEmpty()) q = "Qu'est-ce qui t'a fait sourire aujourd'hui ?";
        v.setTextViewText(R.id.w_prompt, q);
        v.setImageViewResource(R.id.w_mascot, WidgetData.mascotRes(c, j));
        boolean today = WidgetData.writtenToday(j);
        v.setTextViewText(R.id.w_btn, today ? "Répondu ✓ À demain" : "Écrire ✎");
        v.setInt(R.id.w_btn, "setBackgroundResource", today ? R.drawable.widget_pill_soft : R.drawable.widget_pill);
        v.setTextColor(R.id.w_btn, today ? c.getColor(R.color.w_muted) : c.getColor(R.color.w_on_accent));
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "ecrire", 31));
        m.updateAppWidget(id, v);
    }
}
