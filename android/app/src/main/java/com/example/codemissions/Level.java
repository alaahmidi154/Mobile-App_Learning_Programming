package com.example.codemissions;

import java.util.ArrayList;
import java.util.List;

public class Level {
    private final int level;
    private final String name;
    private final int total;
    private final int completed;

    public Level(int level, String name, int total, int completed) {
        this.level = level;
        this.name = name;
        this.total = total;
        this.completed = completed;
    }

    public int getLevel() { return level; }
    public String getName() { return name; }
    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public boolean isCompleted() { return total > 0 && completed == total; }

    /** Builds the levels screen from the missions list (works with the API's data as it is). */
    public static List<Level> group(List<Mission> missions) {
        List<Level> result = new ArrayList<>();
        for (int lv = 1; lv <= 3; lv++) {
            int total = 0, done = 0;
            for (Mission m : missions) {
                if (m.getLevel() != lv) continue;
                total++;
                if (m.isSolved()) done++;
            }
            if (total > 0) result.add(new Level(lv, Mission.levelName(lv), total, done));
        }
        return result;
    }
}
