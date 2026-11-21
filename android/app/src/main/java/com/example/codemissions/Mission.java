package com.example.codemissions;

import android.content.Context;

import androidx.core.content.ContextCompat;

import java.text.Normalizer;

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
}
