package com.petitjournal.nikki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.util.Calendar;

/** The 24 Japanese solar terms (sekki) and the phase of the moon. */
public class SeasonWidget extends AppWidgetProvider {
    // month, day, kanji, french name, line
    static final Object[][] SEKKI = {
        {1, 5, "小寒", "Petit froid", "Le froid s'installe doucement."},
        {1, 20, "大寒", "Grand froid", "Le cœur de l'hiver ; les jours rallongent déjà."},
        {2, 4, "立春", "Début du printemps", "Les premières fleurs de prunier s'ouvrent."},
        {2, 19, "雨水", "Eau de pluie", "La neige fond et devient pluie."},
        {3, 5, "啓蟄", "Réveil des insectes", "Les petites bêtes sortent de terre."},
        {3, 20, "春分", "Équinoxe de printemps", "Le jour et la nuit se partagent le ciel."},
        {4, 4, "清明", "Pure clarté", "Tout est frais et lumineux, les cerisiers fleurissent."},
        {4, 20, "穀雨", "Pluie des céréales", "Une pluie douce nourrit les rizières."},
        {5, 5, "立夏", "Début de l'été", "Les grenouilles commencent à chanter."},
        {5, 21, "小満", "Petite plénitude", "Les plantes poussent de toutes leurs forces."},
        {6, 5, "芒種", "Graines barbues", "C'est le temps de repiquer le riz."},
        {6, 21, "夏至", "Solstice d'été", "Le jour le plus long de l'année."},
        {7, 7, "小暑", "Petite chaleur", "Les cigales se réveillent, Tanabata scintille."},
        {7, 23, "大暑", "Grande chaleur", "Les journées les plus chaudes, l'heure des furin."},
        {8, 7, "立秋", "Début de l'automne", "Le vent tourne doucement."},
        {8, 23, "処暑", "Fin de la chaleur", "La chaleur s'apaise enfin."},
        {9, 7, "白露", "Rosée blanche", "La rosée perle sur les herbes."},
        {9, 23, "秋分", "Équinoxe d'automne", "Le jour et la nuit s'équilibrent à nouveau."},
        {10, 8, "寒露", "Rosée froide", "Les oies sauvages arrivent."},
        {10, 23, "霜降", "Descente du givre", "Premières gelées, les érables rougissent."},
        {11, 7, "立冬", "Début de l'hiver", "Les camélias s'ouvrent dans le froid."},
        {11, 22, "小雪", "Petite neige", "Les premiers flocons dansent."},
        {12, 7, "大雪", "Grande neige", "Les montagnes se couvrent de blanc."},
        {12, 22, "冬至", "Solstice d'hiver", "La nuit la plus longue : un bain au yuzu."}
    };
    static final String[] MOON = {"🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"};
    static final String[] MOON_FR = {"Nouvelle lune", "Premier croissant", "Premier quartier", "Lune gibbeuse croissante", "Pleine lune", "Lune gibbeuse décroissante", "Dernier quartier", "Dernier croissant"};

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
        v.setTextViewText(R.id.w_fr, (String) s[3]);
        v.setTextViewText(R.id.w_line, (String) s[4]);
        v.setTextViewText(R.id.w_next, "Prochaine saison : " + nx[3] + " · dans " + nd + " jour" + (nd > 1 ? "s" : ""));
        double syn = 29.530588853, ref = 947182440000.0; // 2000-01-06 18:14 UTC
        double age = (((System.currentTimeMillis() - ref) / 86400000.0) % syn + syn) % syn;
        int ph = (int) Math.floor((age / syn) * 8 + 0.5) % 8;
        v.setTextViewText(R.id.w_moon, MOON[ph]);
        v.setContentDescription(R.id.w_moon, MOON_FR[ph]);
        v.setTextViewText(R.id.w_k, "Saison japonaise · " + MOON_FR[ph]);
        WidgetData.seasonArt(c, v);
        v.setOnClickPendingIntent(R.id.w_root, WidgetData.open(c, "ecrire", 81));
        m.updateAppWidget(id, v);
    }
}
