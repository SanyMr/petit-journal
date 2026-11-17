package com.petitjournal.nikki;

import android.content.Context;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sagesse du jour 一期一会 : one Japanese proverb per day, chosen from the moods of the three
 * previous days. Same algorithm as wisPick() in the journal, so the widget and the app agree.
 */
final class WisdomData {
    static final String CATS = "RPJES";
    static final String[] SEAL = {"希", "憩", "喜", "進", "縁"};
    static final String[] WHY = {
        "Choisi pour toi après quelques jours tristes.",
        "Choisi pour toi après quelques jours fatigués.",
        "Choisi pour toi : tes derniers jours étaient lumineux.",
        "Choisi pour te donner un peu d'élan.",
        "La sagesse du jour."
    };

    static final class Proverb {
        String id, k, r, fr, m, q, c;
    }

    static final class Pick {
        Proverb p;
        int cat;
    }

    private static List<Proverb> all;

    static synchronized List<Proverb> all(Context c) {
        if (all != null) return all;
        List<Proverb> out = new ArrayList<>();
        try (InputStream in = c.getAssets().open("wisdom/proverbs.json")) {
            byte[] buf = new byte[in.available()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            JSONArray a = new JSONArray(new String(buf, 0, n, "UTF-8"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Proverb p = new Proverb();
                p.id = o.optString("id");
                p.k = o.optString("k");
                p.r = o.optString("r");
                p.fr = o.optString("fr");
                p.m = o.optString("m");
                p.q = o.optString("q");
                p.c = o.optString("c");
                out.add(p);
            }
        } catch (Exception ignored) { }
        all = out;
        return out;
    }

    /** Days since 1970-01-01 of a local "yyyy-MM-dd" date (same as Date.UTC(y,m-1,d)/864e5 in JS). */
    static int dayNum(String key) {
        try {
            String[] s = key.split("-");
            Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            u.clear();
            u.set(Integer.parseInt(s[0]), Integer.parseInt(s[1]) - 1, Integer.parseInt(s[2]));
            return (int) (u.getTimeInMillis() / 86400000L);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Moods of the 3 previous days (today is excluded so the proverb stays the same all day). */
    static List<String> recentMoods(Context c) {
        List<String> out = new ArrayList<>();
        JSONObject days = WidgetData.read(c).optJSONObject("days");
        String pendDay = "", pendMood = "";
        try {
            String raw = c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).getString("mood_pending", "");
            if (!raw.isEmpty()) {
                JSONObject p = new JSONObject(raw);
                pendDay = p.optString("day");
                pendMood = p.optString("mood");
            }
        } catch (Exception ignored) { }
        for (int i = 1; i <= 3; i++) {
            String k = WidgetData.daysAgo(i);
            JSONObject d = days == null ? null : days.optJSONObject(k);
            String m = d == null ? "" : d.optString("m", "");
            if (m.isEmpty() && k.equals(pendDay)) m = pendMood;
            if (!m.isEmpty()) out.add(m);
        }
        return out;
    }

    static int category(List<String> moods) {
        if (moods.isEmpty()) return 4;
        if (moods.contains("triste")) return 0;
        if (moods.contains("fatigue")) return 1;
        int good = 0;
        for (String m : moods) if (m.equals("super") || m.equals("bien")) good++;
        return good * 2 >= moods.size() ? 2 : 3;
    }

    static Pick today(Context c) {
        Pick r = new Pick();
        r.cat = category(recentMoods(c));
        char cc = CATS.charAt(r.cat);
        List<Proverb> pool = new ArrayList<>();
        for (Proverb p : all(c)) if (p.c.indexOf(cc) >= 0) pool.add(p);
        if (pool.isEmpty()) pool = all(c);
        if (pool.isEmpty()) return null;
        int n = pool.size();
        int idx = ((dayNum(WidgetData.today()) + r.cat * 7) % n + n) % n;
        r.p = pool.get(idx);
        return r;
    }

    private WisdomData() { }
}
