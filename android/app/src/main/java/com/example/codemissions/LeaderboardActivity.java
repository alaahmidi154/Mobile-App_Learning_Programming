package com.example.codemissions;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private final List<LeaderboardAdapter.Row> rows = new ArrayList<>();
    private LeaderboardAdapter adapter;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        txtStatus = findViewById(R.id.txtLeaderboardStatus);
        ListView listView = findViewById(R.id.listViewLeaderboard);
        adapter = new LeaderboardAdapter(this, rows);
        listView.setAdapter(adapter);

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        if (ApiClient.USE_MOCK_DATA) {
            rows.clear();
            rows.add(new LeaderboardAdapter.Row(1, "Alice", 220, 3));
            rows.add(new LeaderboardAdapter.Row(2, "Bob", 150, 2));
            rows.add(new LeaderboardAdapter.Row(3, Session.getName(this),
                    Session.getXp(this), Session.getLevel(this)));
            adapter.notifyDataSetChanged();
            return;
        }

        ApiClient.getInstance(this).getArray("/leaderboard",
                (JSONArray response) -> {
                    rows.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        // API contract: rang, nom, xpTotal, niveauJoueur
                        rows.add(new LeaderboardAdapter.Row(
                                o.optInt("rang", i + 1),
                                o.optString("nom"),
                                o.optInt("xpTotal"),
                                o.optInt("niveauJoueur", 1)));
                    }
                    adapter.notifyDataSetChanged();
                    if (rows.isEmpty()) txtStatus.setText("Aucun joueur pour le moment.");
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }
}
