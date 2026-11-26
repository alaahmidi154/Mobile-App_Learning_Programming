package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

/** Admin: list of all missions. Tap one to edit it, or create a new one. */
public class AdminMissionsActivity extends AppCompatActivity {

    private LinearLayout container;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);
        Screen.fit(this);

        container = findViewById(R.id.listContainer);
        txtStatus = findViewById(R.id.txtListStatus);
        ((TextView) findViewById(R.id.txtListTitle)).setText("Missions");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView add = findViewById(R.id.btnAdd);
        add.setVisibility(View.VISIBLE);
        add.setOnClickListener(v -> startActivity(new Intent(this, AdminMissionEditActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        txtStatus.setText("");
        ApiClient.getInstance(this).getArray("/admin/missions",
                (JSONArray arr) -> {
                    container.removeAllViews();
                    LayoutInflater inf = LayoutInflater.from(this);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject m = arr.optJSONObject(i);
                        if (m == null) continue;
                        View row = inf.inflate(R.layout.item_admin_row, container, false);
                        ((TextView) row.findViewById(R.id.txtRowTitle))
                                .setText(m.optInt("id") + ". " + m.optString("titre"));
                        TextView sub = row.findViewById(R.id.txtRowSub);
                        sub.setText(m.optString("difficulte") + " · +" + m.optInt("xp") + " XP · "
                                + m.optInt("nbTests") + " test(s)");
                        TextView tag = row.findViewById(R.id.txtRowTag);
                        tag.setText(m.optInt("nbTests") == 0 ? "sans test" : "ok");
                        tag.setTextColor(getColor(m.optInt("nbTests") == 0
                                ? R.color.verdict_error : R.color.verdict_success));
                        row.setOnClickListener(v -> {
                            Intent intent = new Intent(this, AdminMissionEditActivity.class);
                            intent.putExtra("mission_json", m.toString());
                            startActivity(intent);
                        });
                        container.addView(row);
                    }
                    if (arr.length() == 0) txtStatus.setText("Aucune mission. Appuie sur « Nouvelle ».");
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }
}
