package com.example.codemissions;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private final List<LeaderboardAdapter.Row> restRows = new ArrayList<>();
    private LeaderboardAdapter adapter;
    private TextView txtStatus, txtMyRank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        Screen.fit(this);

        txtStatus = findViewById(R.id.txtLeaderboardStatus);
        txtMyRank = findViewById(R.id.txtMyRank);
        ListView listView = findViewById(R.id.listViewLeaderboard);
        adapter = new LeaderboardAdapter(this, restRows);
        listView.setAdapter(adapter);

        BottomNav.setup(this, BottomNav.LEADERBOARD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        txtStatus.setText("");
        ApiClient.getInstance(this).getArray("/leaderboard",
                (JSONArray response) -> {
                    List<LeaderboardAdapter.Row> all = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        // API contract: rang, nom, xpTotal, niveauJoueur
                        all.add(new LeaderboardAdapter.Row(
                                o.optInt("rang", i + 1),
                                o.optString("nom"),
                                o.optInt("xpTotal"),
                                o.optInt("niveauJoueur", 1)));
                    }
                    showPodium(all);

                    restRows.clear();
                    for (int i = 3; i < all.size(); i++) restRows.add(all.get(i));
                    adapter.notifyDataSetChanged();

                    String me = Session.getName(this);
                    txtMyRank.setText("Classement");
                    for (LeaderboardAdapter.Row r : all) {
                        if (me.equals(r.nom)) {
                            txtMyRank.setText("Tu es #" + r.rang + " · " + r.xp + " XP");
                            break;
                        }
                    }
                    if (all.isEmpty()) txtStatus.setText("Aucun joueur pour le moment.");
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }

    private void showPodium(List<LeaderboardAdapter.Row> all) {
        bindPodium(all, 0, R.id.podium1, R.id.podium1Avatar, R.id.podium1Name, R.id.podium1Xp);
        bindPodium(all, 1, R.id.podium2, R.id.podium2Avatar, R.id.podium2Name, R.id.podium2Xp);
        bindPodium(all, 2, R.id.podium3, R.id.podium3Avatar, R.id.podium3Name, R.id.podium3Xp);
    }

    private void bindPodium(List<LeaderboardAdapter.Row> all, int index,
                            int boxId, int avatarId, int nameId, int xpId) {
        View box = findViewById(boxId);
        if (index >= all.size()) {
            box.setVisibility(View.INVISIBLE);
            return;
        }
        LeaderboardAdapter.Row r = all.get(index);
        box.setVisibility(View.VISIBLE);
        String initial = r.nom == null || r.nom.isEmpty() ? "?" : r.nom.substring(0, 1).toUpperCase();
        ((TextView) findViewById(avatarId)).setText(initial);
        ((TextView) findViewById(nameId)).setText(r.nom);
        ((TextView) findViewById(xpId)).setText(r.xp + " XP");
    }
}
