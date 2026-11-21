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

public class AdminActivity extends AppCompatActivity {

    private final List<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only admins may open this screen
        if (!Session.isAdmin(this)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_admin);

        txtStatus = findViewById(R.id.txtAdminStatus);
        ListView listView = findViewById(R.id.listViewUsers);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        if (ApiClient.USE_MOCK_DATA) {
            rows.clear();
            rows.add("alice@test.com (joueur) - 120 XP");
            rows.add("bob@test.com (joueur) - 85 XP");
            rows.add("admin@test.com (admin) - 0 XP");
            adapter.notifyDataSetChanged();
            return;
        }

        ApiClient.getInstance(this).getArray("/admin/users",
                (JSONArray response) -> {
                    rows.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject o = response.optJSONObject(i);
                        if (o == null) continue;
                        rows.add(o.optString("email") + " (" + o.optString("role")
                                + ") - " + o.optInt("xp") + " XP");
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> txtStatus.setText(ErrorUtils.message(error)));
    }
}