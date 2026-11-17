package com.petitjournal.nikki;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONObject;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Widgets of the journal + the live "concentration" notification of the garden. */
@CapacitorPlugin(name = "NikkiWidget")
public class WidgetPlugin extends Plugin {
    static final int POMO_ID = 5200;
    static final String POMO_CH = "jd_pomo_live";

    @PluginMethod
    public void update(PluginCall call) {
        String data = call.getString("data", "{}");
        Context ctx = getContext();
        ctx.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).edit().putString("data", data).apply();
        WidgetData.refreshAll(ctx);
        call.resolve();
    }

    /** Ongoing notification with a native countdown (keeps ticking while the app is closed). */
    @PluginMethod
    public void pomoShow(PluginCall call) {
        Context ctx = getContext();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) { call.resolve(); return; }
        Double endD = call.getDouble("end", 0.0);
        long end = endD == null ? 0L : endD.longValue();
        boolean paused = Boolean.TRUE.equals(call.getBoolean("paused", false));
        String title = call.getString("title", "Concentration");
        String text = call.getString("text", "");
        String sub = call.getString("sub", "");
        long now = System.currentTimeMillis();

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(POMO_CH, "Concentration en cours", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Le minuteur de concentration du jardin, en direct");
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
            b = new Notification.Builder(ctx, POMO_CH);
        } else {
            b = new Notification.Builder(ctx);
            b.setPriority(Notification.PRIORITY_LOW);
        }
        Intent it = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (it != null) it.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, POMO_ID, it, flags);

        String big = sub.isEmpty() ? text : text + "\n" + sub;
        b.setSmallIcon(R.drawable.ic_stat_nikki)
         .setColor(0xFFD9708D)
         .setContentTitle(title)
         .setContentText(text)
         .setStyle(new Notification.BigTextStyle().bigText(big))
         .setOngoing(true)
         .setOnlyAlertOnce(true)
         .setAutoCancel(false)
         .setContentIntent(pi)
         .setCategory(Notification.CATEGORY_PROGRESS)
         .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (!paused && end > now) {
            b.setWhen(end).setShowWhen(true).setUsesChronometer(true);
            if (Build.VERSION.SDK_INT >= 24) b.setChronometerCountDown(true);
            if (Build.VERSION.SDK_INT >= 26) b.setTimeoutAfter(end - now + 1500);
        } else {
            b.setShowWhen(false);
        }
        try { nm.notify(POMO_ID, b.build()); } catch (Exception e) { /* notifications refused */ }
        savePomo(ctx, true, end, paused, call);
        call.resolve();
    }

    private void savePomo(Context ctx, boolean on, long end, boolean paused, PluginCall call) {
        try {
            JSONObject p = new JSONObject();
            p.put("on", on);
            p.put("end", end);
            p.put("paused", paused);
            p.put("phase", call.getString("phase", "focus"));
            p.put("label", call.getString("label", ""));
            Double left = call.getDouble("left", 0.0);
            p.put("left", left == null ? 0 : left.longValue());
            p.put("n", call.getInt("n", 0));
            p.put("day", WidgetData.today());
            p.put("done", Boolean.TRUE.equals(call.getBoolean("done", false)));
            p.put("at", System.currentTimeMillis());
            ctx.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).edit().putString("pomo", p.toString()).apply();
            WidgetData.refreshAll(ctx);
        } catch (Exception ignored) { }
    }

    /** Pictures of the garden at each time of day, drawn by the journal, for the garden widget. */
    @PluginMethod
    public void gardenImgs(PluginCall call) {
        Context ctx = getContext();
        JSObject imgs = call.getObject("imgs", new JSObject());
        for (String t : new String[]{"matin", "aprem", "soir", "nuit"}) {
            String b64 = imgs.getString(t);
            if (b64 == null || b64.isEmpty()) continue;
            try (FileOutputStream o = new FileOutputStream(new File(ctx.getFilesDir(), "garden_" + t + ".jpg"))) {
                o.write(Base64.decode(b64, Base64.DEFAULT));
            } catch (Exception ignored) { }
        }
        WidgetData.refreshAll(ctx);
        call.resolve();
    }

    /** Mood tapped on the home-screen widget, handed over to the journal once. */
    @PluginMethod
    public void takeMood(PluginCall call) {
        Context ctx = getContext();
        android.content.SharedPreferences sp = ctx.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString("mood_pending", "");
        JSObject r = new JSObject();
        try {
            if (!raw.isEmpty()) {
                JSONObject p = new JSONObject(raw);
                r.put("day", p.optString("day"));
                r.put("mood", p.optString("mood"));
            }
        } catch (Exception ignored) { }
        sp.edit().remove("mood_pending").apply();
        call.resolve(r);
    }

    @PluginMethod
    public void pomoHide(PluginCall call) {
        NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(POMO_ID);
        savePomo(getContext(), false, 0, false, call);
        call.resolve();
    }

    /** Opens the style chooser of the "Sagesse du jour" widget (applies to all of them). */
    @PluginMethod
    public void wisdomStyle(PluginCall call) {
        Intent i = new Intent(getContext(), WisdomConfigActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void wisdomInfo(PluginCall call) {
        JSObject r = new JSObject();
        r.put("count", WisdomWidget.ids(getContext()).length);
        call.resolve(r);
    }

    /** Habits for the home-screen widgets, with their stamp pictures; drops the widget taps the journal has taken in. */
    @PluginMethod
    public void hbUpdate(PluginCall call) {
        Context ctx = getContext();
        android.content.SharedPreferences sp = ctx.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor ed = sp.edit().putString("hb_data", call.getString("data", "{}"));
        int consumed = call.getInt("consumed", 0);
        if (consumed > 0) {
            try {
                org.json.JSONArray o = new org.json.JSONArray(sp.getString("hb_ops", "[]")), rest = new org.json.JSONArray();
                for (int i = consumed; i < o.length(); i++) rest.put(o.get(i));
                ed.putString("hb_ops", rest.toString());
            } catch (Exception ignored) { }
        }
        ed.apply();
        JSObject imgs = call.getObject("imgs", new JSObject());
        File dir = new File(ctx.getFilesDir(), "hb");
        if (!dir.exists()) dir.mkdirs();
        java.util.Iterator<String> it = imgs.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (!k.matches("[A-Za-z0-9_]+")) continue;
            String b64 = imgs.getString(k);
            if (b64 == null || b64.isEmpty()) continue;
            try (FileOutputStream o = new FileOutputStream(new File(dir, k + ".png"))) { o.write(Base64.decode(b64, Base64.DEFAULT)); } catch (Exception ignored) { }
        }
        HabitsData.refresh(ctx);
        call.resolve();
    }

    /** Taps made on the habit widgets since the journal last looked. */
    @PluginMethod
    public void takeHabits(PluginCall call) {
        JSObject r = new JSObject();
        r.put("ops", getContext().getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).getString("hb_ops", "[]"));
        call.resolve(r);
    }
}
