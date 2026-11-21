package com.example.codemissions;

import android.graphics.Color;
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

import org.json.JSONException;
import org.json.JSONObject;

public class MissionActivity extends AppCompatActivity {

    private int missionId;
    private int missionXp;
    private TextView txtTitle, txtDesc, txtVerdict, txtMessage, txtXp;
    private EditText edtCode;
    private Button btnRun;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);

        txtTitle = findViewById(R.id.txtMissionTitle);
        txtDesc = findViewById(R.id.txtMissionDescription);
        txtVerdict = findViewById(R.id.txtVerdict);
        txtMessage = findViewById(R.id.txtVerdictMessage);
        txtXp = findViewById(R.id.txtXpGained);
        edtCode = findViewById(R.id.edtCode);
        btnRun = findViewById(R.id.btnRun);
        progress = findViewById(R.id.progressRun);

        missionId = getIntent().getIntExtra("mission_id", -1);
        missionXp = getIntent().getIntExtra("mission_xp", 10);
        txtTitle.setText(getIntent().getStringExtra("mission_title"));

        btnRun.setOnClickListener(v -> runCode());
        loadMission();
    }

    private void loadMission() {
        if (ApiClient.USE_MOCK_DATA) {
            txtDesc.setText("Write a Python program that solves this mission. "
                    + "(Mock mode: the code passes if it contains 'print'.)");
            edtCode.setText("# Write your code here\n");
            return;
        }
        ApiClient.getInstance(this).getObject("/missions/" + missionId,
                response -> {
                    txtTitle.setText(response.optString("title"));
                    txtDesc.setText(response.optString("description"));
                    edtCode.setText(response.optString("starter_code"));
                },
                error -> showResult("error", ErrorUtils.message(error), 0, Session.getXp(this)));
    }

    private void runCode() {
        final String code = edtCode.getText().toString();
        if (code.trim().isEmpty()) {
            Toast.makeText(this, "Write some code first.", Toast.LENGTH_SHORT).show();
            return;
        }
        setRunning(true);

        if (ApiClient.USE_MOCK_DATA) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean ok = code.contains("print");
                boolean firstTime = !Session.isSolved(this, missionId);
                int gained = (ok && firstTime) ? missionXp : 0;
                if (ok) Session.markSolved(this, missionId);
                showResult(ok ? "passed" : "failed",
                        ok ? "All tests passed." : "The output does not match the expected result.",
                        gained,
                        Session.getXp(this) + gained);
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
                        showResult(response.optString("verdict", "error"),
                                response.optString("message", ""),
                                response.optInt("xp_gained", 0),
                                response.optInt("total_xp", Session.getXp(this)));
                    },
                    error -> {
                        setRunning(false);
                        showResult("error", ErrorUtils.message(error), 0, Session.getXp(this));
                    });
        } catch (JSONException e) {
            setRunning(false);
            showResult("error", "Internal error.", 0, Session.getXp(this));
        }
    }

    private void setRunning(boolean running) {
        btnRun.setEnabled(!running);
        progress.setVisibility(running ? View.VISIBLE : View.GONE);
        if (running) {
            txtVerdict.setText("");
            txtMessage.setText("");
            txtXp.setText("");
        }
    }

    private void showResult(String verdict, String message, int xpGained, int totalXp) {
        String label;
        String color;
        switch (verdict) {
            case "passed":  label = "PASSED";              color = "#38A169"; break;
            case "failed":  label = "FAILED";              color = "#DD6B20"; break;
            case "timeout": label = "TIME LIMIT EXCEEDED"; color = "#718096"; break;
            default:        label = "ERROR";               color = "#E53E3E"; break;
        }
        txtVerdict.setText(label);
        txtVerdict.setTextColor(Color.parseColor(color));
        txtMessage.setText(message);
        txtXp.setText(xpGained > 0 ? "+" + xpGained + " XP (total: " + totalXp + " XP)" : "");
        Session.setXp(this, totalXp);
    }
}