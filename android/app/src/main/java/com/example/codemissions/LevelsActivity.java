package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LevelsActivity extends AppCompatActivity {

    private final List<Level> levels = new ArrayList<>();
    private LevelAdapter adapter;
    private TextView txtUserInfo, txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_levels);

        txtUserInfo = findViewById(R.id.txtUserInfo);
        txtStatus = findViewById(R.id.txtLevelsStatus);
        ListView listView = findViewById(R.id.listViewLevels);
        Button btnLeaderboard = findViewById(R.id.btnLeaderboard);
        Button btnAdmin = findViewById(R.id.btnAdmin);
        Button btnLogout = findViewById(R.id.btnLogout);

        adapter = new LevelAdapter(this, levels);
        listView.setAdapter(adapter);

        // All levels are open: the lock is on the missions inside a level
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, MissionsActivity.class);
            intent.putExtra("level", levels.get(position).getLevel());
            startActivity(intent);
        });

        btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        // Admin screen: demo mode only (the API has no /admin endpoint yet)
        if (ApiClient.USE_MOCK_DATA && Session.isAdmin(this)) {
            btnAdmin.setVisibility(View.VISIBLE);
            btnAdmin.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminActivity.class)));
        }

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
        super.onResume();   // XP/level/solved missions may have changed
        txtUserInfo.setText(Session.getName(this) + " · Niveau " + Session.getLevel(this)
                + " · " + Session.getXp(this) + " XP");
        loadLevels();
    }

    private void showLevels(List<Mission> missions) {
        levels.clear();
        levels.addAll(Level.group(missions));
        adapter.notifyDataSetChanged();
        if (levels.isEmpty()) txtStatus.setText("Aucune mission disponible.");
    }

    private void loadLevels() {
        txtStatus.setText("");
        if (ApiClient.USE_MOCK_DATA) {
            showLevels(MockData.allMissions(this));
            return;
        }

        // The API has no /levels: levels are built from GET /missions (grouped by difficulty)
        ApiClient.getInstance(this).getArray("/missions",
                (JSONArray response) -> {
                    List<Mission> missions = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        // API contract: id, titre, difficulte, xp, reussie
                        missions.add(new Mission(
                                o.optInt("id"),
                                o.optString("titre"),
                                o.optString("difficulte"),
                                o.optInt("xp"),
                                o.optBoolean("reussie", false)));
                    }
                    showLevels(missions);
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }
}
