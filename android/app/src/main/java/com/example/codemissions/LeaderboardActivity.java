package com.example.codemissions;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private final List<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        txtStatus = findViewById(R.id.txtLeaderboardStatus);
        ListView listView = findViewById(R.id.listViewLeaderboard);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        if (ApiClient.USE_MOCK_DATA) {
            rows.clear();
            rows.add("1. alice@test.com - 120 XP");
            rows.add("2. bob@test.com - 85 XP");
            rows.add("3. " + Session.getEmail(this) + " - " + Session.getXp(this) + " XP");
            adapter.notifyDataSetChanged();
            return;
        }

        ApiClient.getInstance(this).getArray("/leaderboard",
                (JSONArray response) -> {
                    rows.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        rows.add((i + 1) + ". " + o.optString("email")
                                + " - " + o.optInt("xp") + " XP");
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> txtStatus.setText(ErrorUtils.message(error)));
    }
}