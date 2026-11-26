package com.example.codemissions;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Forgot password, 2 steps on one screen:
 *  1. POST /api/auth/forgot-password {email}                  -> the API e-mails a 6-digit code
 *  2. POST /api/auth/reset-password {email, code, newPassword} -> password changed
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private EditText edtEmail, edtCode, edtNewPassword;
    private View cardStep2;
    private Button btnSendCode, btnReset, btnResend;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        Screen.fit(this);

        edtEmail = findViewById(R.id.edtResetEmail);
        edtCode = findViewById(R.id.edtCode);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        cardStep2 = findViewById(R.id.cardStep2);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnReset = findViewById(R.id.btnReset);
        btnResend = findViewById(R.id.btnResend);
        txtStatus = findViewById(R.id.txtResetStatus);

        String email = getIntent().getStringExtra("email");
        if (email != null) edtEmail.setText(email);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> sendCode());
        btnResend.setOnClickListener(v -> sendCode());
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void showStatus(String text, boolean error) {
        txtStatus.setTextColor(ContextCompat.getColor(this,
                error ? R.color.verdict_error : R.color.verdict_success));
        txtStatus.setText(text);
    }

    private void setBusy(boolean busy) {
        btnSendCode.setEnabled(!busy);
        btnResend.setEnabled(!busy);
        btnReset.setEnabled(!busy);
    }

    private String email() {
        return edtEmail.getText().toString().trim();
    }

    private void sendCode() {
        if (!Patterns.EMAIL_ADDRESS.matcher(email()).matches()) {
            showStatus("Entre un e-mail valide.", true);
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("email", email());
        } catch (JSONException e) {
            showStatus("Erreur interne.", true);
            return;
        }

        setBusy(true);
        showStatus("Envoi du code...", false);
        ApiClient.getInstance(this).postObject("/auth/forgot-password", body,
                response -> {
                    setBusy(false);
                    cardStep2.setVisibility(View.VISIBLE);
                    edtCode.requestFocus();
                    showStatus(response.optString("message",
                            "Si cet e-mail existe, un code vient d'être envoyé."), false);
                },
                error -> {
                    setBusy(false);
                    showStatus(ErrorUtils.message(error), true);
                });
    }

    private void resetPassword() {
        String code = edtCode.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString();

        if (code.length() != 6) {
            showStatus("Le code contient 6 chiffres.", true);
            return;
        }
        if (newPassword.length() < 6) {
            showStatus("Le mot de passe doit contenir au moins 6 caractères.", true);
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("email", email());
            body.put("code", code);
            body.put("newPassword", newPassword);
        } catch (JSONException e) {
            showStatus("Erreur interne.", true);
            return;
        }

        setBusy(true);
        ApiClient.getInstance(this).postObject("/auth/reset-password", body,
                response -> {
                    setBusy(false);
                    Toast.makeText(this,
                            response.optString("message", "Mot de passe modifié."),
                            Toast.LENGTH_LONG).show();
                    finish();   // back to the login screen
                },
                error -> {
                    setBusy(false);
                    showStatus(ErrorUtils.message(error), true);
                });
    }
}
