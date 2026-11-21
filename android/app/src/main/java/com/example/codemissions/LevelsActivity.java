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

        if (Session.isAdmin(this)) {
            btnAdmin.setVisibility(View.VISIBLE);
            btnAdmin.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminActivity.class)));
        }

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
        txtUserInfo.setText(Session.getEmail(this) + " (" + Session.getRole(this)
                + ") - " + Session.getXp(this) + " XP");
        loadLevels();
    }

    private void loadLevels() {
        txtStatus.setText("");
        if (ApiClient.USE_MOCK_DATA) {
            levels.clear();
            levels.addAll(MockData.levels(this));
            adapter.notifyDataSetChanged();
            return;
        }

        ApiClient.getInstance(this).getArray("/levels",
                (JSONArray response) -> {
                    levels.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        levels.add(new Level(
                                o.optInt("level"),
                                o.optString("name"),
                                o.optInt("total"),
                                o.optInt("completed"),
                                true));
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> txtStatus.setText(ErrorUtils.message(error)));
    }
}