package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.util.Calendar;

/** The 24 Japanese solar terms (sekki) and the phase of the moon. */
public class SeasonWidget extends AppWidgetProvider {
    // mois, jour, kanji (noms et phrases : R.array.w_sekki et w_sekki_line, même ordre)
    static final Object[][] SEKKI = {
        {1, 5, "小寒"},
        {1, 20, "大寒"},
        {2, 4, "立春"},
        {2, 19, "雨水"},
        {3, 5, "啓蟄"},
        {3, 20, "春分"},
        {4, 4, "清明"},
        {4, 20, "穀雨"},
        {5, 5, "立夏"},
        {5, 21, "小満"},
        {6, 5, "芒種"},
        {6, 21, "夏至"},
        {7, 7, "小暑"},
        {7, 23, "大暑"},
        {8, 7, "立秋"},
        {8, 23, "処暑"},
        {9, 7, "白露"},
        {9, 23, "秋分"},
        {10, 8, "寒露"},
        {10, 23, "霜降"},
        {11, 7, "立冬"},
        {11, 22, "小雪"},
        {12, 7, "大雪"},
        {12, 22, "冬至"}
    };
    static final String[] MOON = {"🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { for (int id : ids) render(c, m, id); }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id); }

    static int dayOfYear(int mo, int d) { Calendar x = Calendar.getInstance(); x.set(Calendar.MONTH, mo - 1); x.set(Calendar.DAY_OF_MONTH, d); return x.get(Calendar.DAY_OF_YEAR); }

    static void render(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_season);
        Calendar now = Calendar.getInstance();
        int doy = now.get(Calendar.DAY_OF_YEAR), cur = SEKKI.length - 1;
        for (int i = 0; i < SEKKI.length; i++) if (dayOfYear((int) SEKKI[i][0], (int) SEKKI[i][1]) <= doy) cur = i;
        Object[] s = SEKKI[cur], nx = SEKKI[(cur + 1) % SEKKI.length];
        int nd = dayOfYear((int) nx[0], (int) nx[1]) - doy;
        if (nd <= 0) nd += now.getActualMaximum(Calendar.DAY_OF_YEAR);
        v.setTextViewText(R.id.w_jp, (String) s[2]);
        String[] names = Tr.arr(c, R.array.w_sekki), lines = Tr.arr(c, R.array.w_sekki_line), moons = Tr.arr(c, R.array.w_moon);
        v.setTextViewText(R.id.w_fr, names[cur]);
        v.setTextViewText(R.id.w_line, lines[cur]);
        v.setTextViewText(R.id.w_next, Tr.p(c, R.plurals.w_season_next, nd, names[(cur + 1) % SEKKI.length], nd));
        double syn = 29.530588853, ref = 947182440000.0; // 2000-01-06 18:14 UTC
        double age = (((System.currentTimeMillis() - ref) / 86400000.0) % syn + syn) % syn;
        int ph = (int) Math.floor((age / syn) * 8 + 0.5) % 8;
        v.setTextViewText(R.id.w_moon, MOON[ph]);
        v.setContentDescription(R.id.w_moon, moons[ph]);
        v.setTextViewText(R.id.w_k, Tr.s(c, R.string.w_season_head, moons[ph]));
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "ecrire", 81));
        m.updateAppWidget(id, v);
    }
}
