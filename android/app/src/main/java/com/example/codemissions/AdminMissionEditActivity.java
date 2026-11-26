package com.example.codemissions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Admin: create or edit a mission, and manage its tests. */
public class AdminMissionEditActivity extends AppCompatActivity {

    private static final String[] LEVELS = {"Débutant", "Intermédiaire", "Avancé"};

    private int missionId = -1;                 // -1 = not saved yet
    private String difficulty = LEVELS[0];

    private EditText edtTitre, edtEnonce, edtXp, edtCode, edtIndice, edtTestIn, edtTestOut;
    private TextView txtStatus;
    private View btnDelete, cardTests;
    private TextView[] chips;
    private LinearLayout containerTests;
    private SwitchCompat switchVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_mission);
        Screen.fit(this);

        edtTitre = findViewById(R.id.edtTitre);
        edtEnonce = findViewById(R.id.edtEnonce);
        edtXp = findViewById(R.id.edtXp);
        edtCode = findViewById(R.id.edtCodeDepart);
        edtIndice = findViewById(R.id.edtIndice);
        edtTestIn = findViewById(R.id.edtTestIn);
        edtTestOut = findViewById(R.id.edtTestOut);
        txtStatus = findViewById(R.id.txtEditStatus);
        btnDelete = findViewById(R.id.btnDelete);
        cardTests = findViewById(R.id.cardTests);
        containerTests = findViewById(R.id.containerTests);
        switchVisible = findViewById(R.id.switchVisible);
        chips = new TextView[]{findViewById(R.id.chipEasy), findViewById(R.id.chipMid), findViewById(R.id.chipHard)};

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            chips[i].setOnClickListener(v -> selectLevel(LEVELS[idx]));
        }
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Supprimer la mission ?")
                .setMessage("Ses tests et toutes les soumissions des joueurs seront supprimés.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer", (d, w) -> send(Request.Method.DELETE,
                        "/admin/missions/" + missionId, null, o -> finish()))
                .show());
        findViewById(R.id.btnAddTest).setOnClickListener(v -> addTest());

        String json = getIntent().getStringExtra("mission_json");
        if (json != null) {
            try {
                JSONObject m = new JSONObject(json);
                missionId = m.optInt("id", -1);
                ((TextView) findViewById(R.id.txtEditTitle)).setText("Mission " + missionId);
                edtTitre.setText(m.optString("titre"));
                edtEnonce.setText(m.optString("enonce"));
                edtXp.setText(String.valueOf(m.optInt("xp", 50)));
                edtCode.setText(m.optString("codeDepart"));
                edtIndice.setText(m.isNull("indice") ? "" : m.optString("indice"));
                selectLevel(m.optString("difficulte", LEVELS[0]));
                showTests();
            } catch (JSONException e) {
                selectLevel(LEVELS[0]);
            }
        } else {
            edtXp.setText("50");
            edtCode.setText("n = int(input())\n# Écris ta solution ici");
            selectLevel(LEVELS[0]);
        }
    }

    private void selectLevel(String level) {
        difficulty = Mission.levelName(Mission.levelOf(level));   // normalises the accents
        for (int i = 0; i < chips.length; i++) {
            boolean on = LEVELS[i].equals(difficulty);
            chips[i].setBackgroundResource(on ? R.drawable.bg_cta : R.drawable.bg_pill);
            chips[i].setTextColor(ContextCompat.getColor(this, on ? R.color.p_deep : R.color.text_primary));
        }
    }

    private void showTests() {
        cardTests.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
        loadTests();
    }

    // ---------------------------------------------------------------- mission

    private void save() {
        txtStatus.setText("");
        JSONObject body = new JSONObject();
        try {
            body.put("titre", edtTitre.getText().toString());
            body.put("enonce", edtEnonce.getText().toString());
            body.put("difficulte", difficulty);
            body.put("xp", parseInt(edtXp.getText().toString()));
            body.put("codeDepart", edtCode.getText().toString());
            body.put("indice", edtIndice.getText().toString());
        } catch (JSONException ignored) { }

        boolean creating = missionId < 0;
        send(creating ? Request.Method.POST : Request.Method.PUT,
                creating ? "/admin/missions" : "/admin/missions/" + missionId, body,
                o -> {
                    Toast.makeText(this, "Enregistré.", Toast.LENGTH_SHORT).show();
                    if (creating) {
                        missionId = o.optInt("id", -1);
                        ((TextView) findViewById(R.id.txtEditTitle)).setText("Mission " + missionId);
                        showTests();
                        txtStatus.setText("");
                        Toast.makeText(this, "Ajoute maintenant les tests de la mission.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        finish();
                    }
                });
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    // ---------------------------------------------------------------- tests

    private void loadTests() {
        ApiClient.getInstance(this).getArray("/admin/missions/" + missionId + "/tests",
                (JSONArray arr) -> {
                    containerTests.removeAllViews();
                    LayoutInflater inf = LayoutInflater.from(this);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject t = arr.optJSONObject(i);
                        if (t == null) continue;
                        View row = inf.inflate(R.layout.item_admin_row, containerTests, false);
                        boolean visible = t.optBoolean("visible");
                        ((TextView) row.findViewById(R.id.txtRowTitle))
                                .setText(oneLine(t.optString("entree")) + "  →  " + oneLine(t.optString("sortieAttendue")));
                        ((TextView) row.findViewById(R.id.txtRowSub)).setText("Appuie pour supprimer");
                        TextView tag = row.findViewById(R.id.txtRowTag);
                        tag.setText(visible ? "visible" : "caché");
                        row.setBackgroundResource(R.drawable.bg_card_alt);
                        row.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                                .setTitle("Supprimer ce test ?")
                                .setNegativeButton("Annuler", null)
                                .setPositiveButton("Supprimer", (d, w) -> send(Request.Method.DELETE,
                                        "/admin/tests/" + t.optInt("id"), null, o -> loadTests()))
                                .show());
                        containerTests.addView(row);
                    }
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }

    private static String oneLine(String s) {
        String t = s == null ? "" : s.trim().replace("\n", " ↵ ");
        return t.isEmpty() ? "(vide)" : t;
    }

    private void addTest() {
        JSONObject body = new JSONObject();
        try {
            body.put("entree", edtTestIn.getText().toString());
            body.put("sortieAttendue", edtTestOut.getText().toString());
            body.put("visible", switchVisible.isChecked());
        } catch (JSONException ignored) { }
        send(Request.Method.POST, "/admin/missions/" + missionId + "/tests", body, o -> {
            edtTestIn.setText("");
            edtTestOut.setText("");
            loadTests();
        });
    }

    // ---------------------------------------------------------------- network

    private void send(int method, String path, JSONObject body,
                      com.android.volley.Response.Listener<JSONObject> ok) {
        ApiClient.getInstance(this).send(method, path, body, ok,
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }
}
