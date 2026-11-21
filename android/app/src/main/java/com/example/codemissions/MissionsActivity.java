package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MissionsActivity extends AppCompatActivity {

    private final List<Mission> missions = new ArrayList<>();
    private MissionAdapter adapter;
    private TextView txtUserInfo, txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missions);

        txtUserInfo = findViewById(R.id.txtUserInfo);
        txtStatus = findViewById(R.id.txtMissionsStatus);
        ListView listView = findViewById(R.id.listViewMissions);
        Button btnLeaderboard = findViewById(R.id.btnLeaderboard);
        Button btnLogout = findViewById(R.id.btnLogout);

        adapter = new MissionAdapter(this, missions);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Mission m = missions.get(position);
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
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        txtUserInfo.setText(Session.getEmail(this) + " - " + Session.getXp(this) + " XP");
        loadMissions();
    }

    private void loadMissions() {
        txtStatus.setText("");
        if (ApiClient.USE_MOCK_DATA) {
            missions.clear();
            missions.add(new Mission(1, "Hello World", "Easy", 10, false));
            missions.add(new Mission(2, "Sum of two numbers", "Easy", 10, false));
            missions.add(new Mission(3, "Even or odd", "Easy", 15, false));
            missions.add(new Mission(4, "Reverse a string", "Medium", 20, false));
            missions.add(new Mission(5, "Factorial", "Medium", 25, false));
            adapter.notifyDataSetChanged();
            return;
        }

        ApiClient.getInstance(this).getArray("/missions",
                (JSONArray response) -> {
                    missions.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        missions.add(new Mission(
                                o.optInt("id"),
                                o.optString("title"),
                                o.optString("difficulty"),
                                o.optInt("xp"),
                                o.optBoolean("solved", false)));
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> txtStatus.setText(ErrorUtils.message(error)));
    }
}