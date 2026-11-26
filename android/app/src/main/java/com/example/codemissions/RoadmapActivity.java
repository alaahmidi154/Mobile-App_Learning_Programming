package com.example.codemissions;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/** Home screen: the player's header and the roadmap of missions. */
public class RoadmapActivity extends AppCompatActivity {

    private TextView txtAvatar, txtUserName, txtUserLevel, txtProgressCount, txtUserXp, txtStatus;
    private ProgressBar progressXp;
    private RecyclerView recycler;
    private LinearLayoutManager layoutManager;
    private RoadmapAdapter adapter;
    private boolean firstScroll = true;
    private int shownXp = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roadmap);
        Screen.fit(this);

        txtAvatar = findViewById(R.id.txtAvatar);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserLevel = findViewById(R.id.txtUserLevel);
        txtProgressCount = findViewById(R.id.txtProgressCount);
        txtUserXp = findViewById(R.id.txtUserXp);
        txtStatus = findViewById(R.id.txtRoadmapStatus);
        progressXp = findViewById(R.id.progressXp);
        recycler = findViewById(R.id.recyclerRoadmap);
        layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);

        BottomNav.setup(this, BottomNav.ROADMAP);

        adapter = new RoadmapAdapter(new RoadmapAdapter.Listener() {
            @Override
            public void onMissionClick(Mission m) { openMission(m); }

            @Override
            public void onLockedClick(Mission m) {
                Toast.makeText(RoadmapActivity.this,
                        "Réussis d'abord la mission précédente.", Toast.LENGTH_SHORT).show();
            }
        });
        recycler.setAdapter(adapter);
        findViewById(R.id.btnHero).setOnClickListener(v -> {
            Mission m = adapter.getCurrentMission();
            if (m != null) openMission(m);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindHeader();      // XP / level may have changed after a mission
        Sync.refresh(this, () -> { bindHeader(); loadMissions(); });
    }

    private void bindHeader() {
        String name = Session.getName(this);
        int xp = Session.getXp(this);
        int level = Session.getLevel(this);

        txtAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        txtUserName.setText(name);
        txtUserLevel.setText(Session.isAdmin(this) ? "administrateur" : "niveau " + level);
        ((TextView) findViewById(R.id.txtXpChip)).setText(String.valueOf(xp));
        View chip = (View) ((TextView) findViewById(R.id.txtXpChip)).getParent();
        if (Session.isAdmin(this)) {
            chip.setVisibility(View.GONE);
            progressXp.setVisibility(View.INVISIBLE);
            txtUserXp.setText("Mode aperçu : tout est débloqué, pas de XP.");
            return;
        }
        chip.setVisibility(View.VISIBLE);
        progressXp.setVisibility(View.VISIBLE);
        txtUserXp.setText(xp + " XP · encore " + (100 - xp % 100) + " XP pour le niveau " + (level + 1));

        int from = shownXp < 0 ? 0 : shownXp % 100;
        int to = xp % 100;
        if (shownXp >= 0 && xp / 100 > shownXp / 100) from = 0;   // level up: restart the bar
        ObjectAnimator.ofInt(progressXp, "progress", from, to).setDuration(700).start();
        shownXp = xp;
    }

    private void loadMissions() {
        txtStatus.setText("");
        ApiClient.getInstance(this).getArray("/missions",
                (JSONArray response) -> {
                    List<Mission> missions = new ArrayList<>(Mission.fromJson(response));
                    if (Session.isAdmin(this)) Mission.unlockAll(missions);   // admin: preview, nothing locked
                    adapter.setMissions(missions);

                    int done = 0;
                    for (Mission m : missions) if (m.isSolved()) done++;
                    txtProgressCount.setText(done + "/" + missions.size());
                    bindHero(adapter.getCurrentMission(), !missions.isEmpty());
                    if (missions.isEmpty()) txtStatus.setText("Aucune mission disponible.");

                    // Put the current mission near the top, below the level title
                    int pos = adapter.getCurrentPosition();
                    if (firstScroll && pos >= 0) {
                        layoutManager.scrollToPositionWithOffset(pos, dp(120));
                    }
                    firstScroll = false;
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }

    private void openMission(Mission m) {
        Intent intent = new Intent(this, MissionActivity.class);
        intent.putExtra("mission_id", m.getId());
        intent.putExtra("mission_title", m.getTitle());
        intent.putExtra("mission_xp", m.getXp());
        intent.putExtra("mission_difficulty", m.getDifficulty());
        startActivity(intent);
    }

    private void bindHero(Mission current, boolean hasMissions) {
        TextView label = findViewById(R.id.txtHeroLabel);
        TextView title = findViewById(R.id.txtHeroTitle);
        View btn = findViewById(R.id.btnHero);
        if (current != null) {
            label.setText("MISSION EN COURS · " + Mission.levelName(current.getLevel()).toUpperCase());
            title.setText(current.getTitle());
            btn.setVisibility(View.VISIBLE);
        } else {
            label.setText("PARCOURS");
            title.setText(hasMissions ? "Toutes les missions sont réussies !" : "Aucune mission");
            btn.setVisibility(View.GONE);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
