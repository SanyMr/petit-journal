package com.petitjournal.nikki;

import android.accounts.Account;
import android.app.Activity;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import java.io.IOException;

/**
 * Accès au Google Drive de la personne, limité aux fichiers créés par l'appli (drive.file).
 * connect() : choix du compte + consentement ; token() : jeton d'accès, sans écran si déjà autorisé.
 */
@CapacitorPlugin(name = "NikkiDrive")
public class DrivePlugin extends Plugin {
    static final String SCOPE = "https://www.googleapis.com/auth/drive.file";
    private String pendingEmail = null;

    private GoogleSignInClient client() {
        GoogleSignInOptions o = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(new Scope(SCOPE))
            .build();
        return GoogleSignIn.getClient(getActivity(), o);
    }

    @PluginMethod
    public void connect(PluginCall call) {
        GoogleSignInClient c = client();
        // déconnecte d'abord pour laisser choisir le compte
        c.signOut().addOnCompleteListener(t -> startActivityForResult(call, c.getSignInIntent(), "onSignIn"));
    }

    @ActivityCallback
    private void onSignIn(PluginCall call, ActivityResult result) {
        if (call == null) return;
        try {
            GoogleSignInAccount a = GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);
            if (a == null || a.getEmail() == null) { call.reject("signin_failed"); return; }
            fetch(call, a.getEmail(), true);
        } catch (ApiException e) {
            call.reject(e.getStatusCode() == 12501 ? "cancel" : "signin_failed_" + e.getStatusCode());
        } catch (Exception e) {
            call.reject("signin_failed");
        }
    }

    @PluginMethod
    public void token(PluginCall call) {
        String email = call.getString("email");
        if (email == null || email.isEmpty()) { call.reject("no_account"); return; }
        fetch(call, email, Boolean.TRUE.equals(call.getBoolean("interactive", false)));
    }

    private void fetch(PluginCall call, String email, boolean interactive) {
        new Thread(() -> {
            try {
                String t = GoogleAuthUtil.getToken(getContext(), new Account(email, "com.google"), "oauth2:" + SCOPE);
                JSObject o = new JSObject();
                o.put("token", t);
                o.put("email", email);
                call.resolve(o);
            } catch (UserRecoverableAuthException e) {
                if (interactive && e.getIntent() != null) {
                    pendingEmail = email;
                    getActivity().runOnUiThread(() -> startActivityForResult(call, e.getIntent(), "onConsent"));
                } else {
                    call.reject("need_consent");
                }
            } catch (IOException e) {
                call.reject("network");
            } catch (Exception e) {
                call.reject("auth_failed");
            }
        }).start();
    }

    @ActivityCallback
    private void onConsent(PluginCall call, ActivityResult result) {
        if (call == null) return;
        String email = pendingEmail;
        pendingEmail = null;
        if (result.getResultCode() == Activity.RESULT_OK && email != null) fetch(call, email, false);
        else call.reject("cancel");
    }

    @PluginMethod
    public void clear(PluginCall call) {
        String t = call.getString("token");
        new Thread(() -> {
            try { if (t != null) GoogleAuthUtil.clearToken(getContext(), t); } catch (Exception ignored) { }
            call.resolve();
        }).start();
    }
}
