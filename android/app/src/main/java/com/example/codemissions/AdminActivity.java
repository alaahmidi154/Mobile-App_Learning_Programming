package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/** Admin home: a few numbers and the way to the missions and players screens. */
public class AdminActivity extends AppCompatActivity {

    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        Screen.fit(this);

        if (!Session.isAdmin(this)) {      // a normal player must never see this screen
            finish();
            return;
        }
        txtStatus = findViewById(R.id.txtAdminStatus);
        ((TextView) findViewById(R.id.txtAdminWho)).setText(Session.getName(this) + " · admin");

        BottomNav.setup(this, BottomNav.ADMIN);
        findViewById(R.id.cardAdminMissions).setOnClickListener(v ->
                startActivity(new Intent(this, AdminMissionsActivity.class)));
        findViewById(R.id.cardAdminUsers).setOnClickListener(v ->
                startActivity(new Intent(this, AdminUsersActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Session.isAdmin(this)) return;
        txtStatus.setText("");
        ApiClient.getInstance(this).getObject("/admin/stats",
                (JSONObject o) -> {
                    ((TextView) findViewById(R.id.txtStatPlayers)).setText(String.valueOf(o.optInt("joueurs")));
                    ((TextView) findViewById(R.id.txtStatMissions)).setText(String.valueOf(o.optInt("missions")));
                    ((TextView) findViewById(R.id.txtStatSubs)).setText(String.valueOf(o.optInt("soumissions")));
                    ((TextView) findViewById(R.id.txtStatSuccess)).setText(String.valueOf(o.optInt("reussies")));
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }
}
