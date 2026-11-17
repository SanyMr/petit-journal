package com.petitjournal.nikki;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.getcapacitor.Plugin;
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
        call.resolve();
    }

    @PluginMethod
    public void pomoHide(PluginCall call) {
        NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(POMO_ID);
        call.resolve();
    }
}
