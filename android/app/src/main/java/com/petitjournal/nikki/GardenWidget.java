package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.io.File;
import java.util.Calendar;
import org.json.JSONObject;

/** The user's own Japanese garden, at the right time of day (pictures rendered by the journal). */
public class GardenWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static String timeOfDay() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 10) return "matin";
        if (h >= 10 && h < 17) return "aprem";
        if (h >= 17 && h < 20) return "soir";
        return "nuit";
    }

    static void render(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_garden);
        v.setTextViewText(R.id.w_title, Tr.s(c, R.string.w_garden_head));
        v.setTextViewText(R.id.w_empty, Tr.s(c, R.string.w_garden_empty));
        v.setContentDescription(R.id.w_img, Tr.s(c, R.string.w_garden_desc));
        File f = new File(c.getFilesDir(), "garden_" + timeOfDay() + ".jpg");
        Bitmap b = null;
        if (f.exists()) {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            } catch (Exception ignored) { }
        }
        if (b != null) {
            v.setImageViewBitmap(R.id.w_img, b);
            v.setViewVisibility(R.id.w_empty, View.GONE);
        } else {
            v.setViewVisibility(R.id.w_empty, View.VISIBLE);
        }
        JSONObject g = WidgetData.read(c).optJSONObject("garden");
        String sub = g == null ? "" : g.optString("sub", "");
        v.setTextViewText(R.id.w_sub, sub);
        v.setViewVisibility(R.id.w_sub, sub.isEmpty() ? View.GONE : View.VISIBLE);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "jardin", 41));
        m.updateAppWidget(id, v);
    }
}
