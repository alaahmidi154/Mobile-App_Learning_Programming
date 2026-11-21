package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MissionsActivity extends AppCompatActivity {

    private final List<Mission> missions = new ArrayList<>();
    private MissionAdapter adapter;
    private TextView txtUserName, txtUserLevel, txtUserXp, txtStatus;
    private ProgressBar progressLevel;
    private int level;   // 1 = Débutant, 2 = Intermédiaire, 3 = Avancé

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missions);

        level = getIntent().getIntExtra("level", 1);
        setTitle("Niveau " + level + " · " + Mission.levelName(level));

        txtUserName = findViewById(R.id.txtUserName);
        txtUserLevel = findViewById(R.id.txtUserLevel);
        txtUserXp = findViewById(R.id.txtUserXp);
        progressLevel = findViewById(R.id.progressLevel);
        txtStatus = findViewById(R.id.txtMissionsStatus);
        ListView listView = findViewById(R.id.listViewMissions);
        Button btnLeaderboard = findViewById(R.id.btnLeaderboard);
        Button btnLogout = findViewById(R.id.btnLogout);

        adapter = new MissionAdapter(this, missions);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Mission m = missions.get(position);
            if (!m.isUnlocked()) {
                Toast.makeText(this, "Réussissez d'abord la mission précédente.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, MissionActivity.class);
            intent.putExtra("mission_id", m.getId());
            intent.putExtra("mission_title", m.getTitle());
            intent.putExtra("mission_xp", m.getXp());
            intent.putExtra("mission_difficulty", m.getDifficulty());
            startActivity(intent);
        });

        btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        btnLogout.setOnClickListener(v -> {
            Session.clear(this);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindHeader();   // XP/level may have changed after a submission
        loadMissions();
    }

    private void bindHeader() {
        int xp = Session.getXp(this);
        int level = Session.getLevel(this);
        int inLevel = xp % 100;   // 100 XP per level (same formula as the API)
        txtUserName.setText(Session.getName(this));
        txtUserLevel.setText("Niveau " + level);
        txtUserXp.setText(xp + " XP · encore " + (100 - inLevel) + " XP pour le niveau " + (level + 1));
        progressLevel.setMax(100);
        progressLevel.setProgress(inLevel);
    }

    private void loadMissions() {
        txtStatus.setText("");
        if (ApiClient.USE_MOCK_DATA) {
            missions.clear();
            for (Mission m : MockData.allMissions(this)) {
                if (m.getLevel() == level) missions.add(m);
            }
            Mission.applyLocks(missions);
            adapter.notifyDataSetChanged();
            return;
        }

        ApiClient.getInstance(this).getArray("/missions",
                (JSONArray response) -> {
                    missions.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        // API contract: id, titre, difficulte, xp, reussie
                        Mission m = new Mission(
                                o.optInt("id"),
                                o.optString("titre"),
                                o.optString("difficulte"),
                                o.optInt("xp"),
                                o.optBoolean("reussie", false));
                        if (m.getLevel() == level) missions.add(m);
                    }
                    Mission.applyLocks(missions);
                    adapter.notifyDataSetChanged();
                    if (missions.isEmpty()) txtStatus.setText("Aucune mission disponible.");
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }
}
