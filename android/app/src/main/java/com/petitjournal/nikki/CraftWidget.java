package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.io.File;
import org.json.JSONObject;

/** Craft widgets (omamori, daruma, kintsugi, cranes, seals, lanterns): picture + texts prepared by the journal. */
public abstract class CraftWidget extends AppWidgetProvider {
    static final String[] KINDS = {"omamori", "daruma", "kintsugi", "grues", "goshuin", "lanternes"};
    static final Class<?>[] CLASSES = {OmamoriWidget.class, DarumaWidget.class, KintsugiWidget.class, CranesWidget.class, GoshuinWidget.class, LanternWidget.class};

    abstract String kind();

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id, kind()); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id, kind()); }

    @Override
    public void onDeleted(Context c, int[] ids) {
        SharedPreferences.Editor e = WisdomWidget.prefs(c).edit();
        for (int id : ids) e.remove("wis_" + id);
        e.apply();
    }

    static int index(String kind) {
        for (int i = 0; i < KINDS.length; i++) if (KINDS[i].equals(kind)) return i;
        return -1;
    }

    /** Kind of a placed widget, from its provider class ("wisdom" for the proverb widget). */
    static String kindOf(Context c, int id) {
        try {
            String cls = AppWidgetManager.getInstance(c).getAppWidgetInfo(id).provider.getClassName();
            for (int i = 0; i < CLASSES.length; i++) if (CLASSES[i].getName().equals(cls)) return KINDS[i];
        } catch (Exception ignored) { }
        return "wisdom";
    }

    static int[] ids(Context c, String kind) {
        int i = index(kind);
        if (i < 0) return new int[0];
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, CLASSES[i]));
    }

    static JSONObject data(Context c, String kind) {
        try {
            String raw = WisdomWidget.prefs(c).getString("craft", "");
            if (raw.isEmpty()) return null;
            return new JSONObject(raw).optJSONObject(kind);
        } catch (Exception e) {
            return null;
        }
    }

    static Bitmap bitmap(Context c, String kind, String theme, String fmt, float boxW, float boxH) {
        String f = fmt;
        float w = boxW, h = boxH;
        if ("A".equals(fmt)) f = WisdomRender.autoFormat(boxW, boxH);
        else {
            float[] nat = WisdomRender.natural(fmt);
            float sc = Math.max(.75f, Math.min(1.25f, Math.min(boxW / nat[0], boxH / nat[1])));
            w = nat[0] * sc;
            h = nat[1] * sc;
        }
        Bitmap sprite = null;
        File img = new File(c.getFilesDir(), "craft_" + kind + ".png");
        if (img.exists()) {
            try { sprite = BitmapFactory.decodeFile(img.getAbsolutePath()); } catch (Exception ignored) { }
        }
        float density = c.getResources().getDisplayMetrics().density;
        return WisdomRender.renderCraft(c, data(c, kind), sprite, theme, f, w, h, density);
    }

    static void render(Context c, AppWidgetManager m, int id, String kind) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_craft);
        try {
            String[] st = WisdomWidget.style(c, id);
            float[] sz = WisdomWidget.size(m, id);
            v.setImageViewBitmap(R.id.w_img, bitmap(c, kind, st[0], st[1], sz[0], sz[1]));
        } catch (Throwable ignored) { }
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "porte/" + kind, 60 + Math.max(0, index(kind))));
        m.updateAppWidget(id, v);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (String k : KINDS) for (int id : ids(c, k)) render(c, m, id, k);
    }
}
