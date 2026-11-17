package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** Cute clock: time, French and Japanese date, greeting, mascot and today's page status. */
public class ClockWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static void render(Context c, AppWidgetManager m, int id) {
        JSONObject j = WidgetData.read(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_clock);
        v.setImageViewResource(R.id.w_mascot, WidgetData.mascotRes(c, j));
        v.setTextViewText(R.id.w_greet, WidgetData.greeting());
        boolean today = WidgetData.writtenToday(j);
        v.setTextViewText(R.id.w_status, today ? "✓ Page du jour écrite" : "✎ Ta page du jour t'attend");
        v.setTextColor(R.id.w_status, today ? c.getColor(R.color.w_muted) : c.getColor(R.color.w_accent));
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "ecrire", 21));
        m.updateAppWidget(id, v);
    }
}
