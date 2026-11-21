package com.example.codemissions;

public class Mission {
    private int id;
    private String title;
    private String difficulty;
    private int xp;
    private boolean solved;

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
}