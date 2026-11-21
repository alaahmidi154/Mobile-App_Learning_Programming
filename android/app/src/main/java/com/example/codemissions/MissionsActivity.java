package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
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
    private TextView txtUserInfo, txtStatus;
    private int level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missions);

        level = getIntent().getIntExtra("level", 1);
        setTitle("Level " + level);

        txtUserInfo = findViewById(R.id.txtUserInfo);
        txtStatus = findViewById(R.id.txtMissionsStatus);
        ListView listView = findViewById(R.id.listViewMissions);
        Button btnLeaderboard = findViewById(R.id.btnLeaderboard);
        Button btnLogout = findViewById(R.id.btnLogout);

        adapter = new MissionAdapter(this, missions);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Mission m = missions.get(position);
            if (!m.isUnlocked()) {
                Toast.makeText(this, "Complete the previous mission first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, MissionActivity.class);
            intent.putExtra("mission_id", m.getId());
            intent.putExtra("mission_title", m.getTitle());
            intent.putExtra("mission_xp", m.getXp());
            startActivity(intent);
        });

        btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        btnLogout.setOnClickListener(v -> {
            Session.clear(this);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        txtUserInfo.setText(Session.getEmail(this) + " - " + Session.getXp(this) + " XP");
        loadMissions();
    }

    // The first mission is open; each next one opens when the previous is solved
    private void applyLocks() {
        for (int i = 0; i < missions.size(); i++) {
            missions.get(i).setUnlocked(i == 0 || missions.get(i - 1).isSolved());
        }
    }

    private void loadMissions() {
        txtStatus.setText("");
        if (ApiClient.USE_MOCK_DATA) {
            missions.clear();
            missions.addAll(MockData.missionsForLevel(this, level));
            applyLocks();
            adapter.notifyDataSetChanged();
            return;
        }

        ApiClient.getInstance(this).getArray("/missions?level=" + level,
                (JSONArray response) -> {
                    missions.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        missions.add(new Mission(
                                o.optInt("id"),
                                o.optInt("level", level),
                                o.optString("title"),
                                o.optString("difficulty"),
                                o.optInt("xp"),
                                o.optBoolean("solved", false)));
                    }
                    applyLocks();
                    adapter.notifyDataSetChanged();
                },
                error -> txtStatus.setText(ErrorUtils.message(error)));
    }
}