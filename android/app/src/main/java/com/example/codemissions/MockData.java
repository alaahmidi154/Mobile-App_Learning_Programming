package com.example.codemissions;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/** Demo mode only (ApiClient.USE_MOCK_DATA = true): same 6 missions as database/schema_seed_pg.sql. */
public class MockData {

    private static Mission m(Context c, int id, String title, String diff, int xp) {
        return new Mission(id, title, diff, xp, Session.isSolved(c, id));
    }

    public static List<Mission> allMissions(Context c) {
        List<Mission> l = new ArrayList<>();
        l.add(m(c, 1, "Somme de deux nombres", "Débutant", 50));
        l.add(m(c, 2, "Pair ou impair", "Débutant", 50));
        l.add(m(c, 3, "Maximum de trois nombres", "Débutant", 60));
        l.add(m(c, 4, "Factorielle", "Intermédiaire", 100));
        l.add(m(c, 5, "Compter les voyelles", "Intermédiaire", 100));
        l.add(m(c, 6, "Nombre premier", "Avancé", 150));
        return l;
    }
}
