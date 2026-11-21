package com.example.codemissions;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;

public class MissionActivity extends AppCompatActivity {

    private int missionId;
    private int missionXp;
    private String missionDifficulty;
    private TextView txtTitle, txtMeta, txtDesc;
    private EditText edtCode;
    private Button btnRun;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);

        txtTitle = findViewById(R.id.txtMissionTitle);
        txtMeta = findViewById(R.id.txtMissionMeta);
        txtDesc = findViewById(R.id.txtMissionDescription);
        edtCode = findViewById(R.id.edtCode);
        btnRun = findViewById(R.id.btnRun);
        progress = findViewById(R.id.progressRun);

        missionId = getIntent().getIntExtra("mission_id", -1);
        missionXp = getIntent().getIntExtra("mission_xp", 0);
        missionDifficulty = getIntent().getStringExtra("mission_difficulty");
        txtTitle.setText(getIntent().getStringExtra("mission_title"));
        updateMeta();

        btnRun.setOnClickListener(v -> runCode());
        loadMission();
    }

    private void updateMeta() {
        String difficulty = missionDifficulty == null ? "" : missionDifficulty;
        txtMeta.setText(difficulty + " · +" + missionXp + " XP");
        txtMeta.setTextColor(Mission.difficultyColor(this, difficulty));
    }

    private void loadMission() {
        if (ApiClient.USE_MOCK_DATA) {
            txtDesc.setText("Écrivez un programme Python qui résout cette mission. "
                    + "(Mode démo : le code est accepté s'il contient 'print'.)");
            edtCode.setText("# Écrivez votre code ici\n");
            return;
        }
        ApiClient.getInstance(this).getObject("/missions/" + missionId,
                response -> {
                    // API contract: id, titre, enonce, codeDepart, xp
                    txtTitle.setText(response.optString("titre"));
                    txtDesc.setText(response.optString("enonce"));
                    edtCode.setText(response.optString("codeDepart"));
                    missionXp = response.optInt("xp", missionXp);
                    updateMeta();
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtDesc.setText(ErrorUtils.message(error));
                });
    }

    private void runCode() {
        final String code = edtCode.getText().toString();
        if (code.trim().isEmpty()) {
            Toast.makeText(this, "Écrivez du code d'abord.", Toast.LENGTH_SHORT).show();
            return;
        }
        setRunning(true);

        if (ApiClient.USE_MOCK_DATA) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean ok = code.contains("print");
                boolean firstTime = !Session.isSolved(this, missionId);
                if (ok) Session.markSolved(this, missionId);
                showVerdict(ok ? "réussi" : "échoué",
                        ok ? "Bravo !" : "Sortie incorrecte.",
                        (ok && firstTime) ? missionXp : 0,
                        Session.getXp(this) + ((ok && firstTime) ? missionXp : 0),
                        ok ? 140 : 0);
                setRunning(false);
            }, 1200);
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("code", code);
            ApiClient.getInstance(this).postObject("/missions/" + missionId + "/submit", body,
                    response -> {
                        setRunning(false);
                        // API contract: verdict, tempsExecutionMs, xpGagnes, xpTotal, message
                        showVerdict(response.optString("verdict", "erreur"),
                                response.optString("message", ""),
                                response.optInt("xpGagnes", 0),
                                response.optInt("xpTotal", Session.getXp(this)),
                                response.optInt("tempsExecutionMs", 0));
                    },
                    error -> {
                        setRunning(false);
                        if (ErrorUtils.handleUnauthorized(this, error)) return;
                        // Network error / 503 "service unavailable": not a verdict about the code
                        showDialog("PROBLÈME", R.color.verdict_timeout,
                                ErrorUtils.message(error), "", "");
                    });
        } catch (JSONException e) {
            setRunning(false);
            showDialog("PROBLÈME", R.color.verdict_timeout, "Erreur interne.", "", "");
        }
    }

    private void setRunning(boolean running) {
        btnRun.setEnabled(!running);
        progress.setVisibility(running ? View.VISIBLE : View.GONE);
    }

    private void showVerdict(String verdict, String message, int xpGained, int totalXp, int timeMs) {
        String v = Normalizer.normalize(verdict, Normalizer.Form.NFC);
        String label;
        int color;
        switch (v) {
            case "réussi":        label = "RÉUSSI";        color = R.color.verdict_success; break;
            case "échoué":        label = "ÉCHOUÉ";        color = R.color.verdict_fail;    break;
            case "temps dépassé": label = "TEMPS DÉPASSÉ"; color = R.color.verdict_timeout; break;
            default:              label = "ERREUR";        color = R.color.verdict_error;   break;
        }

        int oldLevel = Session.getLevel(this);
        int newLevel = Session.levelFor(totalXp);
        Session.setProgress(this, totalXp, newLevel);

        String xpLine = "";
        if (xpGained > 0) {
            xpLine = "+" + xpGained + " XP (total : " + totalXp + " XP)";
            if (newLevel > oldLevel) xpLine += "\nNiveau supérieur ! Niveau " + newLevel;
        }
        String timeLine = timeMs > 0 ? "Temps d'exécution : " + timeMs + " ms" : "";
        showDialog(label, color, message, xpLine, timeLine);
    }

    private void showDialog(String label, int colorRes, String message, String xpLine, String timeLine) {
        View view = getLayoutInflater().inflate(R.layout.dialog_verdict, null);
        TextView txtLabel = view.findViewById(R.id.txtDialogVerdict);
        txtLabel.setText(label);
        txtLabel.setTextColor(ContextCompat.getColor(this, colorRes));
        setTextOrHide((TextView) view.findViewById(R.id.txtDialogMessage), message);
        setTextOrHide((TextView) view.findViewById(R.id.txtDialogXp), xpLine);
        setTextOrHide((TextView) view.findViewById(R.id.txtDialogTime), timeLine);

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton("OK", null)
                .show();
    }

    private static void setTextOrHide(TextView tv, String text) {
        if (text == null || text.isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        }
    }
}
