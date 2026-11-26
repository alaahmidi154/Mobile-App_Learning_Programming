package com.example.codemissions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Admin: the players. Tap one to change the role, reset the progress or delete the account. */
public class AdminUsersActivity extends AppCompatActivity {

    private LinearLayout container;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);
        Screen.fit(this);

        container = findViewById(R.id.listContainer);
        txtStatus = findViewById(R.id.txtListStatus);
        ((TextView) findViewById(R.id.txtListTitle)).setText("Joueurs");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        txtStatus.setText("");
        ApiClient.getInstance(this).getArray("/admin/users",
                (JSONArray arr) -> {
                    container.removeAllViews();
                    LayoutInflater inf = LayoutInflater.from(this);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject u = arr.optJSONObject(i);
                        if (u == null) continue;
                        View row = inf.inflate(R.layout.item_admin_row, container, false);
                        ((TextView) row.findViewById(R.id.txtRowTitle)).setText(u.optString("nom"));
                        ((TextView) row.findViewById(R.id.txtRowSub)).setText(u.optString("email")
                                + " · " + u.optInt("xpTotal") + " XP · niv. " + u.optInt("niveauJoueur"));
                        TextView tag = row.findViewById(R.id.txtRowTag);
                        boolean admin = "admin".equals(u.optString("role"));
                        tag.setText(admin ? "admin" : "joueur");
                        tag.setTextColor(getColor(admin ? R.color.verdict_fail : R.color.p_cream));
                        row.setOnClickListener(v -> actions(u));
                        container.addView(row);
                    }
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }

    private void actions(JSONObject u) {
        final int id = u.optInt("id");
        final boolean admin = "admin".equals(u.optString("role"));
        String[] items = {
                admin ? "Rendre simple joueur" : "Promouvoir administrateur",
                "Réinitialiser la progression",
                "Supprimer le compte"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(u.optString("nom"))
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        JSONObject body = new JSONObject();
                        try { body.put("role", admin ? "joueur" : "admin"); } catch (JSONException ignored) { }
                        call(Request.Method.PUT, "/admin/users/" + id + "/role", body);
                    } else if (which == 1) {
                        confirm("Remettre à zéro l'XP et les soumissions de " + u.optString("nom") + " ?",
                                () -> call(Request.Method.POST, "/admin/users/" + id + "/reset-progress", null));
                    } else {
                        confirm("Supprimer définitivement le compte de " + u.optString("nom") + " ?",
                                () -> call(Request.Method.DELETE, "/admin/users/" + id, null));
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void confirm(String message, Runnable yes) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmer")
                .setMessage(message)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Oui", (d, w) -> yes.run())
                .show();
    }

    private void call(int method, String path, JSONObject body) {
        ApiClient.getInstance(this).send(method, path, body,
                o -> {
                    Toast.makeText(this, "Fait.", Toast.LENGTH_SHORT).show();
                    load();
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    Toast.makeText(this, ErrorUtils.message(error), Toast.LENGTH_LONG).show();
                });
    }
}
