package com.petitjournal.nikki;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

/**
 * Langue des widgets et notifications : la même que celle choisie dans l'appli (envoyée dans les
 * données des widgets, rangée à part sous "lang"). Les textes vivent dans res/values/strings.xml (français, repli)
 * et res/values-<code>/strings.xml. Ajouter une langue = ajouter un dossier values-<code>.
 */
final class Tr {
    private static String cachedLang;
    private static Resources cachedRes;

    /** Code de langue choisi dans l'appli ; avant la première ouverture, la langue du téléphone. */
    static String lang(Context c) {
        String l = c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).getString("lang", "");
        if (l.isEmpty()) l = Locale.getDefault().getLanguage();
        return l;
    }

    static Locale locale(Context c) {
        return new Locale(lang(c));
    }

    static synchronized Resources res(Context c) {
        String l = lang(c);
        if (cachedRes != null && l.equals(cachedLang)) return cachedRes;
        Configuration cf = new Configuration(c.getResources().getConfiguration());
        cf.setLocale(new Locale(l));
        cachedRes = c.createConfigurationContext(cf).getResources();
        cachedLang = l;
        return cachedRes;
    }

    static String s(Context c, int id, Object... args) {
        Resources r = res(c);
        return args.length == 0 ? r.getString(id) : r.getString(id, args);
    }

    static String p(Context c, int id, int n, Object... args) {
        return res(c).getQuantityString(id, n, args);
    }

    static String[] arr(Context c, int id) {
        return res(c).getStringArray(id);
    }

    private Tr() { }
}
