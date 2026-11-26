package com.example.codemissions;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** Bottom bar shared by the 3 main screens (Parcours, Classement, Profil). */
public class BottomNav {

    public static final int ROADMAP = 0;
    public static final int LEADERBOARD = 1;
    public static final int PROFILE = 2;
    public static final int ADMIN = 3;

    public static void setup(Activity activity, int selected) {
        style(activity, R.id.navRoadmap, R.id.navRoadmapIcon, R.id.navRoadmapLabel, selected == ROADMAP);
        style(activity, R.id.navLeaderboard, R.id.navLeaderboardIcon, R.id.navLeaderboardLabel, selected == LEADERBOARD);
        style(activity, R.id.navProfile, R.id.navProfileIcon, R.id.navProfileLabel, selected == PROFILE);

        if (Session.isAdmin(activity)) {
            activity.findViewById(R.id.navAdmin).setVisibility(android.view.View.VISIBLE);
            style(activity, R.id.navAdmin, R.id.navAdminIcon, R.id.navAdminLabel, selected == ADMIN);
            activity.findViewById(R.id.navAdmin).setOnClickListener(v ->
                    go(activity, selected, ADMIN, AdminActivity.class));
        }

        activity.findViewById(R.id.navRoadmap).setOnClickListener(v ->
                go(activity, selected, ROADMAP, RoadmapActivity.class));
        activity.findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                go(activity, selected, LEADERBOARD, LeaderboardActivity.class));
        activity.findViewById(R.id.navProfile).setOnClickListener(v ->
                go(activity, selected, PROFILE, ProfileActivity.class));
    }

    private static void style(Activity a, int boxId, int iconId, int labelId, boolean active) {
        a.findViewById(boxId).setBackgroundResource(active ? R.drawable.bg_nav_active : 0);
        ImageView icon = a.findViewById(iconId);
        TextView label = a.findViewById(labelId);
        icon.setColorFilter(ContextCompat.getColor(a,
                active ? R.color.py_yellow : R.color.text_muted));
        label.setTextColor(ContextCompat.getColor(a,
                active ? R.color.py_yellow : R.color.text_muted));
    }

    private static void go(Activity from, int current, int target, Class<?> cls) {
        if (current == target) return;
        Intent intent = new Intent(from, cls);
        // Re-use the screen if it is already open instead of stacking copies
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        from.startActivity(intent);
    }
}
