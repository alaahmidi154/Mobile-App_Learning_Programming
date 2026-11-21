package com.example.codemissions;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class MockData {

    private static Mission m(Context c, int id, int level, String title, String diff, int xp) {
        return new Mission(id, level, title, diff, xp, Session.isSolved(c, id));
    }

    public static List<Mission> allMissions(Context c) {
        List<Mission> l = new ArrayList<>();
        l.add(m(c, 1, 1, "Hello World", "Easy", 10));
        l.add(m(c, 2, 1, "Sum of two numbers", "Easy", 10));
        l.add(m(c, 3, 1, "Even or odd", "Easy", 15));
        l.add(m(c, 4, 2, "Reverse a string", "Medium", 20));
        l.add(m(c, 5, 2, "Factorial", "Medium", 25));
        l.add(m(c, 6, 2, "Count vowels", "Medium", 25));
        l.add(m(c, 7, 3, "Palindrome", "Hard", 35));
        l.add(m(c, 8, 3, "Fibonacci", "Hard", 40));
        return l;
    }

    public static List<Mission> missionsForLevel(Context c, int level) {
        List<Mission> result = new ArrayList<>();
        for (Mission mission : allMissions(c)) {
            if (mission.getLevel() == level) result.add(mission);
        }
        return result;
    }

    public static List<Level> levels(Context c) {
        String[] names = {"Easy", "Medium", "Hard"};
        List<Level> result = new ArrayList<>();
        for (int lv = 1; lv <= 3; lv++) {
            List<Mission> ms = missionsForLevel(c, lv);
            int done = 0;
            for (Mission mission : ms) if (mission.isSolved()) done++;
            result.add(new Level(lv, names[lv - 1], ms.size(), done, true));
        }
        return result;
    }
}