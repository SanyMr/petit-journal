package com.petitjournal.nikki;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;

/** Draws the "Sagesse du jour" widget (backgrounds, matcha bowl, vertical calligraphy, seal). */
final class WisdomRender {
    static final String[] THEMES = {"clair", "sumi", "aizome", "urushi", "charbon"};
    static final String[] THEME_NAMES = {"Clair", "Sumi-e", "Indigo", "Laque", "Charbon"};
    // text, secondary, accent
    private static final String[][] COL = {
        {"#4b3a47", "#7a6672", "#a5802b"},
        {"#efe6d6", "#a99f95", "#e2bf6a"},
        {"#eef2ff", "#9fb0dc", "#f0c96a"},
        {"#f3e3c4", "#b79a74", "#e7c06a"},
        {"#f1ece2", "#aaa39a", "#e9c46a"},
    };
    private static final String SMALLK = "ぁぃぅぇぉっゃゅょゎ";
    private static Typeface yuji, kiwi, zen, zenb;

    private static synchronized void fonts(Context c) {
        if (yuji != null) return;
        try {
            yuji = Typeface.createFromAsset(c.getAssets(), "wisdom/yuji.ttf");
            kiwi = Typeface.createFromAsset(c.getAssets(), "wisdom/kiwi.ttf");
            zen = Typeface.createFromAsset(c.getAssets(), "wisdom/zen.ttf");
            zenb = Typeface.createFromAsset(c.getAssets(), "wisdom/zenb.ttf");
        } catch (Exception e) {
            yuji = Typeface.SERIF;
            kiwi = Typeface.DEFAULT;
            zen = Typeface.DEFAULT;
            zenb = Typeface.DEFAULT_BOLD;
        }
    }

    static int themeIndex(String t) {
        for (int i = 0; i < THEMES.length; i++) if (THEMES[i].equals(t)) return i;
        return 1;
    }

    /** Natural size (dp) of a forced format. */
    static float[] natural(String fmt) {
        if ("S".equals(fmt)) return new float[] {160, 170};
        if ("L".equals(fmt)) return new float[] {340, 340};
        return new float[] {340, 160};
    }

    static String autoFormat(float w, float h) {
        if (w < 200) return "S";
        if (h >= 220) return "L";
        return "M";
    }

    // ---------------------------------------------------------------- drawing helpers
    private final Canvas cv;
    private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float W, H;

    private WisdomRender(Canvas cv, float w, float h) {
        this.cv = cv;
        W = w;
        H = h;
    }

    private Paint font(Typeface tf, float size) {
        pt.setTypeface(tf);
        pt.setTextSize(size);
        pt.setStyle(Paint.Style.FILL);
        pt.setShader(null);
        return pt;
    }

    private float measure(String t, Typeface tf, float size) { return font(tf, size).measureText(t); }

    private void text(float x, float y, String t, Typeface tf, float size, int col, boolean center) {
        Paint p = font(tf, size);
        p.setColor(col);
        p.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);
        cv.drawText(t, x, y, p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private List<String> tokens(String t) {
        List<String> out = new ArrayList<>();
        for (String w : t.split(" ")) {
            if (!out.isEmpty() && (w.equals(":") || w.equals(";") || w.equals("!") || w.equals("?") || w.equals("»"))) {
                out.set(out.size() - 1, out.get(out.size() - 1) + " " + w);
            } else out.add(w);
        }
        return out;
    }

    private List<String> wrap(String t, Typeface tf, float size, float maxw) {
        List<String> lines = new ArrayList<>();
        String cur = "";
        for (String w : tokens(t)) {
            String nxt = cur.isEmpty() ? w : cur + " " + w;
            if (!cur.isEmpty() && measure(nxt, tf, size) > maxw) {
                lines.add(cur);
                cur = w;
            } else cur = nxt;
        }
        if (!cur.isEmpty()) lines.add(cur);
        return lines;
    }

    private static final class Fit {
        float s;
        List<String> lines;
    }

    private Fit fit(String t, Typeface tf, float mx, float mn, float maxw, int maxl) {
        Fit f = new Fit();
        float s = mx;
        List<String> L;
        while (true) {
            L = wrap(t, tf, s, maxw);
            if (L.size() <= maxl || s - .5f < mn) break;
            s -= .5f;
        }
        if (L.size() > maxl) {
            L = new ArrayList<>(L.subList(0, Math.max(1, maxl)));
            String l = L.get(L.size() - 1);
            while (!l.isEmpty() && measure(l + "…", tf, s) > maxw) l = l.substring(0, l.length() - 1);
            l = l.replaceAll("[ ,;:]+$", "");
            L.set(L.size() - 1, l + "…");
        }
        f.s = s;
        f.lines = L;
        return f;
    }

    private float para(float x, float y, Fit f, Typeface tf, int col, float lh, float centerW) {
        for (int i = 0; i < f.lines.size(); i++) {
            float by = y + f.s * (1 + i * lh);
            if (centerW > 0) text(x + centerW / 2, by, f.lines.get(i), tf, f.s, col, true);
            else text(x, by, f.lines.get(i), tf, f.s, col, false);
        }
        return y + f.s * (1 + (f.lines.size() - 1) * lh) + f.s * .35f;
    }

    /** {cols, per, size} of a vertical calligraphy block. */
    private static float[] kblock(int n, float availH, float maxS, int maxPer, float maxW) {
        while (true) {
            int cols = (n + maxPer - 1) / maxPer;
            int per = (n + cols - 1) / cols;
            float s = Math.min(maxS, availH / per);
            if (cols * s * 1.08f <= maxW || maxPer >= n) return new float[] {cols, per, s};
            maxPer++;
        }
    }

    /** Vertical writing, columns from right to left. Returns the left edge of the block. */
    private float kanji(String t, float right, float top, float[] kb, int col) {
        int per = (int) kb[1];
        float s = kb[2], cw = s * 1.08f;
        int i = 0;
        for (int off = 0; off < t.length(); ) {
            int cp = t.codePointAt(off);
            String ch = new String(Character.toChars(cp));
            off += Character.charCount(cp);
            int c = i / per, r = i % per;
            float x = right - cw * (c + .5f), y = top + s * (r + 1) - s * .14f;
            if (SMALLK.contains(ch)) {
                x += s * .14f;
                y -= s * .16f;
            }
            text(x, y, ch, yuji, s, col, true);
            i++;
        }
        return right - cw * kb[0];
    }

    private void seal(float x, float y, float sz, String ch) {
        pt.setShader(null);
        pt.setStyle(Paint.Style.FILL);
        pt.setColor(Color.parseColor("#b8322b"));
        cv.drawRoundRect(new RectF(x, y, x + sz, y + sz), sz * .18f, sz * .18f, pt);
        text(x + sz / 2, y + sz * .8f, ch, yuji, sz * .74f, Color.parseColor("#fbeee0"), true);
    }

    private void header(int col, boolean shortH) {
        text(14, 22, "一期一会", yuji, 12.5f, col, false);
        if (!shortH) text(14 + measure("一期一会", yuji, 12.5f) + 5, 22, "· Sagesse du jour", zenb, 10, col, false);
    }

    private void cover(Bitmap b) {
        float sc = Math.max(W / b.getWidth(), H / b.getHeight());
        float w = b.getWidth() * sc, h = b.getHeight() * sc;
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        cv.drawBitmap(b, null, new RectF((W - w) / 2, (H - h) / 2, (W + w) / 2, (H + h) / 2), p);
    }

    private static int count(String s) { return s.codePointCount(0, s.length()); }

    // ---------------------------------------------------------------- the widget
    /** Renders the widget at wDp × hDp (layout fmt: S, M or L). */
    static Bitmap render(Context c, WisdomData.Pick pk, String theme, String fmt, float wDp, float hDp, float density) {
        fonts(c);
        float k = Math.min(density, 1000f / Math.max(wDp, hDp));
        int pw = Math.max(1, Math.round(wDp * k)), ph = Math.max(1, Math.round(hDp * k));
        Bitmap raw = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(raw);
        canvas.scale(pw / wDp, ph / hDp);
        WisdomRender R_ = new WisdomRender(canvas, wDp, hDp);
        int ti = themeIndex(theme);
        int tc = Color.parseColor(COL[ti][0]), sc = Color.parseColor(COL[ti][1]), ac = Color.parseColor(COL[ti][2]);
        String res = "wis_bg_" + THEMES[ti] + (wDp / hDp > 1.4f ? "_w" : "_sq");
        int rid = c.getResources().getIdentifier(res, "drawable", c.getPackageName());
        if (rid != 0) {
            Bitmap bg = BitmapFactory.decodeResource(c.getResources(), rid);
            if (bg != null) R_.cover(bg);
        }
        if (ti == 3) {
            Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
            g.setStyle(Paint.Style.STROKE);
            g.setStrokeWidth(1.1f);
            g.setColor(Color.parseColor("#d8ad4a"));
            canvas.drawRoundRect(new RectF(5, 5, wDp - 5, hDp - 5), 17, 17, g);
        }
        if (pk != null && pk.p != null) R_.draw(c, pk, fmt, tc, sc, ac);
        else R_.text(wDp / 2, hDp / 2, "Ouvre le journal une fois ✿", zen, 13, tc, true);

        // rounded, anti-aliased corners
        Bitmap out = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888);
        Canvas oc = new Canvas(out);
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setShader(new BitmapShader(raw, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float rad = 20 * pw / wDp;
        oc.drawRoundRect(new RectF(0, 0, pw, ph), rad, rad, sp);
        raw.recycle();
        return out;
    }

    private void draw(Context c, WisdomData.Pick pk, String fmt, int tc, int sc, int ac) {
        WisdomData.Proverb p = pk.p;
        String sealCh = WisdomData.SEAL[pk.cat];
        int n = count(p.k);
        Bitmap bowl = null;
        int bid = c.getResources().getIdentifier("wis_bowl", "drawable", c.getPackageName());
        if (bid != 0) bowl = BitmapFactory.decodeResource(c.getResources(), bid);
        float BA = 525f / 600f;
        Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        if ("S".equals(fmt)) {
            header(sc, true);
            seal(W - 28, 12, 15, sealCh);
            float tb = 46;
            float[] kb = kblock(n, H - 34 - tb, 32, 5, 1e9f);
            float bw = kb[2] * 1.08f * kb[0];
            kanji(p.k, W / 2 + bw / 2, 32, kb, tc);
            Fit f = fit(p.fr, kiwi, 12.5f, 9.5f, W - 24, 2);
            para(12, H - 14 - f.s * (1 + (f.lines.size() - 1) * 1.25f), f, kiwi, tc, 1.25f, W - 24);
        } else if ("M".equals(fmt)) {
            header(sc, false);
            float bw = Math.min(W * .30f, (H - 36) / BA), bh = bw * BA;
            if (bowl != null) cv.drawBitmap(bowl, null, new RectF(8, H - 8 - bh, 8 + bw, H - 8), bp);
            float[] kb = kblock(n, H - 36, 30, 6, W * .27f);
            float left = kanji(p.k, W - 14, 28, kb, tc);
            seal(left - 17, H - 12 - 15, 14, sealCh);
            float x0 = 8 + bw + 10, x1 = left - 24, y = 32;
            Fit f = fit(p.fr, kiwi, 15, 11, x1 - x0, 4);
            y = para(x0, y, f, kiwi, tc, 1.22f, 0) + 3;
            int ml = Math.max(1, (int) ((H - 12 - y) / (10.5f * 1.32f)));
            Fit m = fit(p.m, zen, 10.5f, 9.5f, x1 - x0, ml);
            para(x0, y, m, zen, sc, 1.32f, 0);
        } else {
            header(sc, false);
            float topH = H * .44f;
            float bw = Math.min(W * .46f, (topH - 10) / BA), bh = bw * BA;
            if (bowl != null) cv.drawBitmap(bowl, null, new RectF(10, 30 + topH - bh - 4, 10 + bw, 30 + topH - 4), bp);
            float[] kb = kblock(n, topH - 4, 40, 6, W * .45f);
            kanji(p.k, W - 18, 32, kb, tc);
            float y = 34 + topH + 4;
            Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
            lp.setColor((sc & 0x00ffffff) | 0x55000000);
            lp.setStrokeWidth(1);
            cv.drawLine(18, y, W - 18, y, lp);
            y += 10;
            Fit f = fit(p.fr, kiwi, 18, 13, W - 36, 3);
            y = para(18, y, f, kiwi, tc, 1.22f, 0) + 2;
            text(18, y + 10, p.r, Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC), 10, ac, false);
            y += 18;
            int ml = Math.max(1, (int) ((H - 40 - y) / (12 * 1.35f)));
            Fit m = fit(p.m, zen, 12, 10, W - 36, ml);
            para(18, y, m, zen, sc, 1.35f, 0);
            text(18, H - 16, WisdomData.WHY[pk.cat], zenb, 10, sc, false);
            seal(W - 34, H - 36, 18, sealCh);
        }
    }
}
