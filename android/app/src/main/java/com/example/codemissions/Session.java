package com.example.codemissions;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("session", Context.MODE_PRIVATE);
    }

    public static void save(Context c, String token, String email, int xp) {
        prefs(c).edit()
                .putString("token", token)
                .putString("email", email)
                .putInt("xp", xp)
                .apply();
    }

    public static String getToken(Context c) { return prefs(c).getString("token", null); }
    public static String getEmail(Context c) { return prefs(c).getString("email", ""); }
    public static int getXp(Context c) { return prefs(c).getInt("xp", 0); }
    public static void setXp(Context c, int xp) { prefs(c).edit().putInt("xp", xp).apply(); }
    public static boolean isLoggedIn(Context c) { return getToken(c) != null; }
    public static void clear(Context c) { prefs(c).edit().clear().apply(); }
}