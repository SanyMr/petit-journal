package com.petitjournal.nikki;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Checks the friends' little notes in the background (about every 30 minutes),
 * refreshes the "Boîte aux lettres" widget and posts at most one gentle notification a day.
 * Uses only the free Firebase plan: the phone asks, no server code.
 */
public class MailWorker extends Worker {
    static final String[] NUDGES = {"Courage ✿", "Ta page t'attend ✎", "Je pense à toi ♡", "Bravo pour ta flamme !", "Belle journée ☀",
        "Repose-toi bien ☾", "On écrit ensemble ce soir ?", "Tu gères ✧", "Gros câlin ʕ•ᴥ•ʔ", "Merci d'être là ♡"};
    static final String CHANNEL = "lettres_furin";
    private static final int NOTIF_ID = 4242;

    public MailWorker(@NonNull Context c, @NonNull WorkerParameters p) { super(c, p); }

    static void schedule(Context c) {
        Constraints net = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest r = new PeriodicWorkRequest.Builder(MailWorker.class, 30, TimeUnit.MINUTES).setConstraints(net).build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork("nikki_mail", ExistingPeriodicWorkPolicy.KEEP, r);
    }

    static void checkNow(Context c) {
        Constraints net = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        WorkManager.getInstance(c).enqueueUniqueWork("nikki_mail_now", ExistingWorkPolicy.REPLACE,
            new OneTimeWorkRequest.Builder(MailWorker.class).setConstraints(net).build());
    }

    static String nudgeText(long k) { return k >= 0 && k < NUDGES.length ? NUDGES[(int) k] : "✿"; }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        SharedPreferences sp = c.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE);
        try {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            JSONObject mail = new JSONObject();
            mail.put("at", System.currentTimeMillis());
            if (u == null) {
                mail.put("signedIn", false);
                sp.edit().putString("mail", mail.toString()).apply();
                WidgetData.refreshAll(c);
                return Result.success();
            }
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = u.getUid();
            QuerySnapshot fl = Tasks.await(db.collection("friends").document(uid).collection("list").get(), 30, TimeUnit.SECONDS);
            Map<String, DocumentSnapshot> prof = new HashMap<>();
            for (DocumentSnapshot f : fl.getDocuments()) {
                try {
                    DocumentSnapshot p = Tasks.await(db.collection("profiles").document(f.getId()).get(), 20, TimeUnit.SECONDS);
                    if (p.exists()) prof.put(f.getId(), p);
                } catch (Exception ignored) { }
            }
            QuerySnapshot ns = Tasks.await(db.collection("nudges").document(uid).collection("msgs").get(), 30, TimeUnit.SECONDS);
            List<DocumentSnapshot> notes = new ArrayList<>(ns.getDocuments());
            notes.sort((a, b) -> Long.compare(num(b.get("at")), num(a.get("at"))));

            String today = WidgetData.today();
            // shared questions waiting for my answer
            List<String[]> questions = new ArrayList<>();
            for (Map.Entry<String, DocumentSnapshot> e : prof.entrySet()) {
                String fid = e.getKey();
                String pair = uid.compareTo(fid) < 0 ? uid + "_" + fid : fid + "_" + uid;
                try {
                    DocumentSnapshot q = Tasks.await(db.document("duos/" + pair + "/q/" + today).get(), 20, TimeUnit.SECONDS);
                    if (q.exists()) {
                        List<?> done = q.get("done") instanceof List ? (List<?>) q.get("done") : new ArrayList<>();
                        if (!done.contains(uid)) questions.add(new String[]{"q_" + pair + "_" + today, str(e.getValue().get("name"), "Un·e ami·e"), str(e.getValue().get("mascot"), "onigiri")});
                    }
                } catch (Exception ignored) { }
            }
            JSONArray band = new JSONArray();
            for (DocumentSnapshot p : prof.values()) {
                JSONObject b = new JSONObject();
                b.put("n", str(p.get("name"), "?"));
                b.put("m", str(p.get("mascot"), "onigiri"));
                List<?> week = p.get("week") instanceof List ? (List<?>) p.get("week") : null;
                b.put("w", week != null && week.contains(today));
                band.put(b);
                if (band.length() >= 5) break;
            }
            mail.put("signedIn", true);
            mail.put("friends", prof.size());
            mail.put("band", band);
            mail.put("count", notes.size());
            mail.put("duoCount", questions.size());
            if (!questions.isEmpty()) {
                JSONObject dq = new JSONObject();
                dq.put("name", questions.get(0)[1]);
                dq.put("mascot", questions.get(0)[2]);
                mail.put("duo", dq);
            }
            if (!notes.isEmpty()) {
                DocumentSnapshot n = notes.get(0);
                DocumentSnapshot p = prof.get(str(n.get("from"), ""));
                JSONObject last = new JSONObject();
                last.put("name", p != null ? str(p.get("name"), "Un·e ami·e") : "Un·e ami·e");
                last.put("mascot", p != null ? str(p.get("mascot"), "onigiri") : "onigiri");
                last.put("text", nudgeText(num(n.get("k"))));
                mail.put("last", last);
            }
            sp.edit().putString("mail", mail.toString()).apply();
            WidgetData.refreshAll(c);
            maybeNotify(c, sp, notes, prof, questions);
            return Result.success();
        } catch (Exception e) {
            return Result.success();
        }
    }

    private void maybeNotify(Context c, SharedPreferences sp, List<DocumentSnapshot> notes, Map<String, DocumentSnapshot> prof, List<String[]> questions) {
        Set<String> seen = new HashSet<>(sp.getStringSet("mail_seen", new HashSet<>()));
        JSONObject data = WidgetData.read(c);
        JSONArray appSeen = data.optJSONArray("mailSeen");
        if (appSeen != null) for (int i = 0; i < appSeen.length(); i++) seen.add(appSeen.optString(i));
        List<DocumentSnapshot> fresh = new ArrayList<>();
        Set<String> all = new HashSet<>();
        for (DocumentSnapshot n : notes) { all.add(n.getId()); if (!seen.contains(n.getId())) fresh.add(n); }
        List<String[]> freshQ = new ArrayList<>();
        for (String[] q : questions) { all.add(q[0]); if (!seen.contains(q[0])) freshQ.add(q); }
        boolean first = !sp.contains("mail_seen");
        sp.edit().putStringSet("mail_seen", all).apply();
        if (first || (fresh.isEmpty() && freshQ.isEmpty())) return;
        if (!data.optBoolean("mailNotif", true)) return;
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 8 || h >= 21) { sp.edit().putStringSet("mail_seen", seen).apply(); return; } // keep them for the morning
        String today = WidgetData.today();
        if (today.equals(sp.getString("mail_notif_day", ""))) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        String title, text;
        if (fresh.isEmpty()) {
            title = freshQ.get(0)[1] + " te pose une question ✿";
            text = "Réponds-lui : vos réponses se dévoileront ensemble.";
        } else {
            DocumentSnapshot n = fresh.get(0);
            DocumentSnapshot p = prof.get(str(n.get("from"), ""));
            String name = p != null ? str(p.get("name"), "Un·e ami·e") : "Un·e ami·e";
            title = fresh.size() == 1 ? "Un petit mot de " + name + " ✉" : name + " et " + (fresh.size() - 1) + (fresh.size() == 2 ? " autre ami·e" : " autres amis") + " t'ont écrit ✉";
            text = fresh.size() == 1 ? nudgeText(num(n.get("k"))) : "Ta boîte aux lettres t'attend ✿";
            if (!freshQ.isEmpty()) text += " Et une question t'attend !";
        }
        ensureChannel(c);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_nikki)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(0xFFD9708D)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(WidgetData.open(c, "amis", 50));
        try {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, b.build());
            sp.edit().putString("mail_notif_day", today).apply();
        } catch (SecurityException ignored) { }
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Petits mots des amis", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Quand un·e ami·e t'envoie un petit mot (au maximum une fois par jour)");
        ch.enableVibration(false);
        ch.setSound(Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.nikki_furin),
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        nm.createNotificationChannel(ch);
    }

    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }
    private static String str(Object o, String def) { return o instanceof String && !((String) o).isEmpty() ? (String) o : def; }
}
