package com.example.codemissions;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Session {

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("session", Context.MODE_PRIVATE);
    }

    /** Real mode: the API answers {token, nom, xpTotal, niveauJoueur}. */
    public static void save(Context c, String token, String nom, int xp, int level) {
        save(c, token, nom, xp, level, "joueur");
    }

    public static void save(Context c, String token, String nom, int xp, int level, String role) {
        prefs(c).edit()
                .putString("token", token)
                .putString("nom", nom)
                .putInt("xp", xp)
                .putInt("level", level)
                .putString("role", role)
                .apply();
    }

    /** Called after a submission: the API returns the new XP total. */
    public static void setProgress(Context c, int xp, int level) {
        prefs(c).edit().putInt("xp", xp).putInt("level", level).apply();
    }

    public static void setRole(Context c, String role) {
        prefs(c).edit().putString("role", role).apply();
    }

    /** Same formula as the API (MissionsController): level = xpTotal / 100 + 1. */
    public static int levelFor(int xp) { return xp / 100 + 1; }

    public static String getToken(Context c) { return prefs(c).getString("token", null); }
    public static String getName(Context c) { return prefs(c).getString("nom", ""); }
    public static int getXp(Context c) { return prefs(c).getInt("xp", 0); }
    public static int getLevel(Context c) { return prefs(c).getInt("level", 1); }
    public static String getRole(Context c) { return prefs(c).getString("role", "joueur"); }
    public static boolean isAdmin(Context c) { return "admin".equals(getRole(c)); }
    public static boolean isLoggedIn(Context c) { return getToken(c) != null; }

    public static void clear(Context c) { prefs(c).edit().clear().apply(); }
}
