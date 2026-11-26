package com.example.codemissions;

import android.content.Context;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class Mission {
    private final int id;
    private final String title;
    private final String difficulty;
    private final int xp;
    private final boolean solved;
    private boolean unlocked = true;

    public Mission(int id, String title, String difficulty, int xp, boolean solved) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.xp = xp;
        this.solved = solved;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDifficulty() { return difficulty; }
    public int getXp() { return xp; }
    public boolean isSolved() { return solved; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    /** The API has no "level" for missions: the level = the difficulty (1, 2, 3). */
    public int getLevel() { return levelOf(difficulty); }

    private static String norm(String s) {
        return s == null ? "" : Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    public static int levelOf(String difficulty) {
        switch (norm(difficulty)) {
            case "Intermédiaire": return 2;
            case "Avancé":        return 3;
            default:              return 1;   // "Débutant" and anything unknown
        }
    }

    public static String levelName(int level) {
        switch (level) {
            case 2:  return "Intermédiaire";
            case 3:  return "Avancé";
            default: return "Débutant";
        }
    }

    /** A small Python flavour for each level, shown on the roadmap. */
    public static String levelCode(int level) {
        switch (level) {
            case 2:  return "if / for / def";
            case 3:  return "algorithmes";
            default: return "print()";
        }
    }

    /** Green / orange / red for the 3 difficulties of the database. */
    public static int difficultyColor(Context c, String difficulty) {
        switch (levelOf(difficulty)) {
            case 2:  return ContextCompat.getColor(c, R.color.diff_medium);
            case 3:  return ContextCompat.getColor(c, R.color.diff_hard);
            default: return ContextCompat.getColor(c, R.color.diff_easy);
        }
    }

    /** First mission of a level is open; the next one opens when the previous is solved. */
    public static void applyLocks(java.util.List<Mission> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setUnlocked(i == 0 || list.get(i - 1).isSolved());
        }
    }

    /** Admins preview the whole roadmap: nothing is locked for them. */
    public static void unlockAll(java.util.List<Mission> list) {
        for (Mission m : list) m.setUnlocked(true);
    }

    /** Parses GET /api/missions: [{id, titre, difficulte, xp, reussie}], already locked in order. */
    public static List<Mission> fromJson(JSONArray array) {
        List<Mission> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            list.add(new Mission(
                    o.optInt("id"),
                    o.optString("titre"),
                    o.optString("difficulte"),
                    o.optInt("xp"),
                    o.optBoolean("reussie", false)));
        }
        // Débutant, then Intermédiaire, then Avancé (stable: keeps the API order inside a level)
        java.util.Collections.sort(list, (a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
        applyLocks(list);
        return list;
    }
}
