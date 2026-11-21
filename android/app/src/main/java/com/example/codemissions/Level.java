package com.example.codemissions;

public class Level {
    private final int level;
    private final String name;
    private final int total;
    private final int completed;
    private final boolean unlocked;

    public Level(int level, String name, int total, int completed, boolean unlocked) {
        this.level = level;
        this.name = name;
        this.total = total;
        this.completed = completed;
        this.unlocked = unlocked;
    }

    public int getLevel() { return level; }
    public String getName() { return name; }
    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public boolean isUnlocked() { return unlocked; }
    public boolean isCompleted() { return total > 0 && completed == total; }
}