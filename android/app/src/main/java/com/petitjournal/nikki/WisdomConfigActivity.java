package com.petitjournal.nikki;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Choose the background and format of the "Sagesse du jour" widget, with a live preview. */
public class WisdomConfigActivity extends Activity {
    private static final int INK = Color.parseColor("#4b3a47"), MUT = Color.parseColor("#7a6672"),
        ACC = Color.parseColor("#b5506d"), BG = Color.parseColor("#fbf6ef");
    private static final String[] FMT = {"A", "S", "M", "L"};
    private static final String[] FMT_LBL = {"Auto", "Petit 2×2", "Moyen 4×2", "Grand 4×4"};
    private int wid = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String theme = "sumi", fmt = "A", kind = "wisdom";
    private static final String[][] KIND_TXT = {
        {"wisdom", "一期一会", "La sagesse du jour", "Chaque jour, un proverbe japonais choisi selon tes humeurs. Choisis son style."},
        {"omamori", "お守り", "L'omamori brodé", "Ton intention du mois, brodée de fils d'or à chaque page écrite. Choisis son style."},
        {"daruma", "達磨", "Le daruma des objectifs", "Un œil pour ton objectif, le second quand tu l'atteins. Choisis son style."},
        {"kintsugi", "金継ぎ", "Le bol kintsugi", "Les jours difficiles deviennent des fils d'or. Choisis son style."},
        {"grues", "千羽鶴", "Les mille grues", "Une grue pliée pour chaque page écrite. Choisis son style."},
        {"goshuin", "御朱印", "Le carnet de sceaux", "Un sceau vermillon pour chaque semaine d'écriture. Choisis son style."},
        {"lanternes", "灯籠流し", "La rivière des lanternes", "Les soucis confiés à la rivière. Choisis son style."},
    };

    private String[] txt() {
        for (String[] t : KIND_TXT) if (t[0].equals(kind)) return t;
        return KIND_TXT[0];
    }
    private ImageView preview;
    private final View[] themeViews = new View[5], fmtViews = new View[4];
    private Typeface kiwi, zen, zenb;
    private float boxW = 340, boxH = 160;

    private int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setResult(RESULT_CANCELED);
        Bundle ex = getIntent().getExtras();
        if (ex != null) wid = ex.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (ex != null && ex.getString("kind") != null) kind = ex.getString("kind");
        else if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) kind = CraftWidget.kindOf(this, wid);
        String[] st = WisdomWidget.style(this, wid);
        theme = st[0];
        fmt = st[1];
        if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) {
            float[] sz = WisdomWidget.size(AppWidgetManager.getInstance(this), wid);
            boxW = sz[0];
            boxH = sz[1];
        }
        try {
            kiwi = Typeface.createFromAsset(getAssets(), "wisdom/kiwi.ttf");
            zen = Typeface.createFromAsset(getAssets(), "wisdom/zen.ttf");
            zenb = Typeface.createFromAsset(getAssets(), "wisdom/zenb.ttf");
        } catch (Exception e) {
            kiwi = zen = Typeface.DEFAULT;
            zenb = Typeface.DEFAULT_BOLD;
        }
        if (Build.VERSION.SDK_INT >= 21) getWindow().setStatusBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(22), dp(28), dp(22), dp(22));
        sv.addView(col);

        TextView kan = label(txt()[1], 15, ACC, zenb);
        col.addView(kan);
        TextView title = label(txt()[2], 24, INK, kiwi);
        col.addView(title);
        TextView sub = label(txt()[3], 14, MUT, zen);
        sub.setPadding(0, dp(4), 0, dp(14));
        col.addView(sub);

        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(12), dp(16), dp(12), dp(16));
        box.setBackground(round(Color.parseColor("#efe6dc"), 22, 0, 0));
        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));
        col.addView(box);

        col.addView(section("Fond"));
        LinearLayout themes = new LinearLayout(this);
        themes.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 5; i++) {
            final String t = WisdomRender.THEMES[i];
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER_HORIZONTAL);
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(thumb(t));
            iv.setPadding(dp(3), dp(3), dp(3), dp(3));
            chip.addView(iv, new LinearLayout.LayoutParams(dp(56), dp(56)));
            TextView tl = label(WisdomRender.THEME_NAMES[i], 12, INK, zen);
            tl.setGravity(Gravity.CENTER);
            tl.setPadding(0, dp(4), 0, 0);
            chip.addView(tl);
            chip.setOnClickListener(v -> { theme = t; refresh(); });
            chip.setContentDescription("Fond " + WisdomRender.THEME_NAMES[i]);
            themeViews[i] = iv;
            themes.addView(chip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        col.addView(themes);

        col.addView(section("Format"));
        LinearLayout fmts = new LinearLayout(this);
        fmts.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 4; i++) {
            final String f = FMT[i];
            TextView b2 = label(FMT_LBL[i], 13, INK, zenb);
            b2.setGravity(Gravity.CENTER);
            b2.setPadding(dp(4), dp(12), dp(4), dp(12));
            b2.setOnClickListener(v -> { fmt = f; refresh(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(i == 0 ? 0 : dp(4), 0, i == 3 ? 0 : dp(4), 0);
            fmtViews[i] = b2;
            fmts.addView(b2, lp);
        }
        col.addView(fmts);
        TextView hint = label("Auto : le widget choisit sa mise en page selon la place que tu lui donnes. Agrandis-le sur l'écran d'accueil pour voir la scène complète.", 12.5f, MUT, zen);
        hint.setPadding(0, dp(10), 0, dp(18));
        col.addView(hint);

        View spacer = new View(this);
        col.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        TextView ok = label(wid == AppWidgetManager.INVALID_APPWIDGET_ID ? "Enregistrer" : "Poser le widget", 16, Color.WHITE, zenb);
        ok.setGravity(Gravity.CENTER);
        ok.setPadding(0, dp(15), 0, dp(15));
        ok.setBackground(round(ACC, 30, 0, 0));
        ok.setOnClickListener(v -> save());
        col.addView(ok);

        setContentView(sv);
        refresh();
    }

    private TextView label(String t, float sp, int color, Typeface tf) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        tv.setTypeface(tf);
        return tv;
    }

    private TextView section(String t) {
        TextView tv = label(t, 15, INK, zenb);
        tv.setPadding(0, dp(18), 0, dp(8));
        return tv;
    }

    private GradientDrawable round(int fill, float r, int stroke, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(r));
        if (stroke > 0) g.setStroke(dp(stroke), strokeColor);
        return g;
    }

    private Bitmap thumb(String t) {
        int rid = getResources().getIdentifier("wis_bg_" + t + "_sq", "drawable", getPackageName());
        int s = dp(50);
        Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Bitmap src = rid == 0 ? null : BitmapFactory.decodeResource(getResources(), rid);
        if (src == null) return out;
        Bitmap sc = Bitmap.createScaledBitmap(src, s, s, true);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new BitmapShader(sc, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        new Canvas(out).drawRoundRect(new RectF(0, 0, s, s), dp(14), dp(14), p);
        return out;
    }

    private void refresh() {
        for (int i = 0; i < 5; i++) {
            boolean on = WisdomRender.THEMES[i].equals(theme);
            themeViews[i].setBackground(round(Color.TRANSPARENT, 17, on ? 3 : 0, ACC));
        }
        for (int i = 0; i < 4; i++) {
            boolean on = FMT[i].equals(fmt);
            fmtViews[i].setBackground(round(on ? Color.parseColor("#fde8ee") : Color.WHITE, 14, on ? 2 : 1, on ? ACC : Color.parseColor("#e6d9df")));
        }
        float w = boxW, h = boxH;
        if ("A".equals(fmt) && wid == AppWidgetManager.INVALID_APPWIDGET_ID) { w = 340; h = 160; }
        try {
            preview.setImageBitmap("wisdom".equals(kind) ? WisdomWidget.bitmap(this, theme, fmt, w, h) : CraftWidget.bitmap(this, kind, theme, fmt, w, h));
        } catch (Throwable ignored) { }
    }

    private void save() {
        if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) {
            WisdomWidget.setStyle(this, wid, theme, fmt);
            for (int id : "wisdom".equals(kind) ? WisdomWidget.ids(this) : CraftWidget.ids(this, kind)) WisdomWidget.setStyle(this, id, theme, fmt);
        } else {
            WisdomWidget.setStyle(this, wid, theme, fmt);
            WisdomWidget.setStyle(this, AppWidgetManager.INVALID_APPWIDGET_ID, theme, fmt);
        }
        WisdomWidget.refresh(this);
        CraftWidget.refreshAll(this);
        WisdomWidget.scheduleMidnight(this);
        Intent r = new Intent();
        r.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid);
        setResult(RESULT_OK, r);
        finish();
    }
}
