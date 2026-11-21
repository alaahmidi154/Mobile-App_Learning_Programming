package com.example.codemissions;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Session {

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("session", Context.MODE_PRIVATE);
    }

    public static void save(Context c, String token, String email, int xp, String role) {
        prefs(c).edit()
                .putString("token", token)
                .putString("email", email)
                .putInt("xp", xp)
                .putString("role", role)
                .apply();
    }

    public static String getToken(Context c) { return prefs(c).getString("token", null); }
    public static String getEmail(Context c) { return prefs(c).getString("email", ""); }
    public static int getXp(Context c) { return prefs(c).getInt("xp", 0); }
    public static void setXp(Context c, int xp) { prefs(c).edit().putInt("xp", xp).apply(); }
    public static String getRole(Context c) { return prefs(c).getString("role", "player"); }
    public static boolean isAdmin(Context c) { return "admin".equals(getRole(c)); }
    public static boolean isLoggedIn(Context c) { return getToken(c) != null; }

    // Mock mode only: remembers which missions were solved
    public static void markSolved(Context c, int missionId) {
        Set<String> s = new HashSet<>(prefs(c).getStringSet("solved", new HashSet<>()));
        s.add(String.valueOf(missionId));
        prefs(c).edit().putStringSet("solved", s).apply();
    }

    public static boolean isSolved(Context c, int missionId) {
        return prefs(c).getStringSet("solved", new HashSet<>()).contains(String.valueOf(missionId));
    }

    public static void clear(Context c) {
        prefs(c).edit().remove("token").remove("email").remove("xp").remove("role").apply();
    }
}