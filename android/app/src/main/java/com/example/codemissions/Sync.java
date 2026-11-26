package com.example.codemissions;

import android.app.Activity;

import org.json.JSONObject;

/** Re-reads XP, level and role from the server (an admin may have changed them). */
public class Sync {
    public static void refresh(Activity activity, Runnable done) {
        ApiClient.getInstance(activity).getObject("/me",
                (JSONObject o) -> {
                    Session.setProgress(activity, o.optInt("xpTotal"), o.optInt("niveauJoueur", 1));
                    String role = o.optString("role", "");
                    if (!role.isEmpty()) Session.setRole(activity, role);
                    if (done != null) done.run();
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(activity, error)) return;
                    if (done != null) done.run();      // offline: keep what we have
                });
    }
}
