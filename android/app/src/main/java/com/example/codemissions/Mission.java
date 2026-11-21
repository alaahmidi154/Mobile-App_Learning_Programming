package com.example.codemissions;

public class Mission {
    private int id;
    private int level;
    private String title;
    private String difficulty;
    private int xp;
    private boolean solved;
    private boolean unlocked = true;

    public Mission(int id, int level, String title, String difficulty, int xp, boolean solved) {
        this.id = id;
        this.level = level;
        this.title = title;
        this.difficulty = difficulty;
        this.xp = xp;
        this.solved = solved;
    }

    public int getId() { return id; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public String getDifficulty() { return difficulty; }
    public int getXp() { return xp; }
    public boolean isSolved() { return solved; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}