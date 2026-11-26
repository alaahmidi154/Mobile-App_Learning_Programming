package com.example.codemissions;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Player profile: level, stats and badges. */
public class ProfileActivity extends AppCompatActivity {

    private static class Badge {
        final String icon, title, description;
        final boolean unlocked;

        Badge(String icon, String title, String description, boolean unlocked) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.unlocked = unlocked;
        }
    }

    private TextView txtStatMissions, txtStatRank, txtStatus;
    private LinearLayout badgesContainer;

    private List<Mission> missions = new ArrayList<>();
    private int rank = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Screen.fit(this);

        txtStatMissions = findViewById(R.id.txtStatMissions);
        txtStatRank = findViewById(R.id.txtStatRank);
        txtStatus = findViewById(R.id.txtProfileStatus);
        badgesContainer = findViewById(R.id.badgesContainer);

        BottomNav.setup(this, BottomNav.PROFILE);

        findViewById(R.id.btnLogout).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Se déconnecter ?")
                        .setMessage("Ta progression reste sauvegardée sur ton compte.")
                        .setNegativeButton("Annuler", null)
                        .setPositiveButton("Déconnexion", (d, w) -> {
                            Session.clear(this);
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPlayer();
        Sync.refresh(this, () -> { bindPlayer(); loadStats(); });
    }

    private void bindPlayer() {
        String name = Session.getName(this);
        int xp = Session.getXp(this);
        int level = Session.getLevel(this);

        ((TextView) findViewById(R.id.txtAvatar))
                .setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        ((TextView) findViewById(R.id.txtName)).setText(name);
        ((TextView) findViewById(R.id.txtRole))
                .setText(Session.isAdmin(this) ? "administrateur" : "joueur");
        ((TextView) findViewById(R.id.txtStatXp)).setText(String.valueOf(xp));
        ((TextView) findViewById(R.id.txtLevelInfo))
                .setText("Niveau " + level + " · encore " + (100 - xp % 100)
                        + " XP pour le niveau " + (level + 1));
        ProgressBar bar = findViewById(R.id.progressXp);
        ObjectAnimator.ofInt(bar, "progress", 0, xp % 100).setDuration(700).start();
        refreshBadges();
        boolean admin = Session.isAdmin(this);
        // an admin is not a player: no badges, no rank
        findViewById(R.id.txtBadgesTitle).setVisibility(admin ? View.GONE : View.VISIBLE);
        badgesContainer.setVisibility(admin ? View.GONE : View.VISIBLE);
        if (admin) txtStatRank.setText("admin");
    }

    private void loadStats() {
        txtStatus.setText("");
        ApiClient.getInstance(this).getArray("/missions",
                (JSONArray response) -> {
                    missions = Mission.fromJson(response);
                    int done = 0;
                    for (Mission m : missions) if (m.isSolved()) done++;
                    txtStatMissions.setText(done + "/" + missions.size());
                    refreshBadges();
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });

        ApiClient.getInstance(this).getArray("/leaderboard",
                (JSONArray response) -> {
                    String me = Session.getName(this);
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o != null && me.equals(o.optString("nom"))) {
                            rank = o.optInt("rang", i + 1);
                            break;
                        }
                    }
                    if (!Session.isAdmin(this)) txtStatRank.setText(rank > 0 ? "#" + rank : "#-");
                    refreshBadges();
                },
                error -> { /* rank stays "#-" */ });
    }

    private boolean levelDone(int level) {
        int total = 0, done = 0;
        for (Mission m : missions) {
            if (m.getLevel() != level) continue;
            total++;
            if (m.isSolved()) done++;
        }
        return total > 0 && total == done;
    }

    private void refreshBadges() {
        int solved = 0;
        for (Mission m : missions) if (m.isSolved()) solved++;
        boolean all = !missions.isEmpty() && solved == missions.size();

        List<Badge> badges = new ArrayList<>();
        badges.add(new Badge("1", "Premier pas", "Réussir ta première mission.", solved >= 1));
        badges.add(new Badge("01", "print()", "Terminer tout le niveau Débutant.", levelDone(1)));
        badges.add(new Badge("def", "def", "Terminer tout le niveau Intermédiaire.", levelDone(2)));
        badges.add(new Badge("O(n)", "Algo", "Terminer tout le niveau Avancé.", levelDone(3)));
        badges.add(new Badge("lv3", "Niveau 3", "Atteindre le niveau 3.", Session.getLevel(this) >= 3));
        badges.add(new Badge("#3", "Top 3", "Entrer dans le top 3 du classement.", rank > 0 && rank <= 3));
        badges.add(new Badge("py", "Pythonista", "Réussir toutes les missions.", all));

        badgesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int perRow = 3;
        for (int i = 0; i < badges.size(); i += perRow) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(rowParams);

            for (int j = 0; j < perRow; j++) {
                int idx = i + j;
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                int gap = (int) (4 * getResources().getDisplayMetrics().density);
                p.setMargins(gap, 0, gap, 0);
                if (idx >= badges.size()) {
                    View filler = new View(this);
                    filler.setLayoutParams(p);
                    row.addView(filler);
                    continue;
                }
                final Badge b = badges.get(idx);
                View item = inflater.inflate(R.layout.item_badge, row, false);
                item.setLayoutParams(p);
                ((TextView) item.findViewById(R.id.txtBadgeIcon)).setText(b.icon);
                ((TextView) item.findViewById(R.id.txtBadgeTitle)).setText(b.title);
                TextView glyph = item.findViewById(R.id.txtBadgeIcon);
                glyph.setBackgroundResource(b.unlocked ? R.drawable.bg_badge_on : R.drawable.bg_badge_off);
                glyph.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                        b.unlocked ? R.color.py_yellow : R.color.text_muted));
                item.setAlpha(b.unlocked ? 1f : 0.55f);
                item.setOnClickListener(v -> Toast.makeText(this,
                        b.title + " : " + b.description + (b.unlocked ? " (obtenu)" : ""),
                        Toast.LENGTH_SHORT).show());
                row.addView(item);
            }
            badgesContainer.addView(row);
        }
    }
}
