package com.example.codemissions;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText edtEmail, edtCode, edtNewPassword;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        edtEmail = findViewById(R.id.edtResetEmail);
        edtCode = findViewById(R.id.edtResetCode);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        txtStatus = findViewById(R.id.txtResetStatus);
        Button btnSend = findViewById(R.id.btnSendCode);
        Button btnReset = findViewById(R.id.btnResetPassword);

        btnSend.setOnClickListener(v -> sendCode());
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void show(String message, boolean error) {
        txtStatus.setTextColor(Color.parseColor(error ? "#E53E3E" : "#38A169"));
        txtStatus.setText(message);
    }

    private void sendCode() {
        String email = edtEmail.getText().toString().trim();
        if (email.isEmpty()) {
            show("Entrez d'abord votre e-mail.", true);
            return;
        }
        if (ApiClient.USE_MOCK_DATA) {
            show("Mode démo : utilisez le code 123456.", false);
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            ApiClient.getInstance(this).postObject("/auth/forgot-password", body,
                    response -> show(response.optString("message",
                            "Si cet e-mail existe, un code a été envoyé."), false),
                    error -> show(ErrorUtils.message(error), true));
        } catch (JSONException e) {
            show("Erreur interne.", true);
        }
    }

    private void resetPassword() {
        String email = edtEmail.getText().toString().trim();
        String code = edtCode.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString();

        if (email.isEmpty() || code.isEmpty() || newPassword.isEmpty()) {
            show("Remplissez tous les champs.", true);
            return;
        }
        if (newPassword.length() < 6) {
            show("Le mot de passe doit contenir au moins 6 caractères.", true);
            return;
        }
        if (ApiClient.USE_MOCK_DATA) {
            if ("123456".equals(code)) {
                Toast.makeText(this, "Mot de passe réinitialisé (démo).", Toast.LENGTH_LONG).show();
                finish();
            } else {
                show("Code invalide.", true);
            }
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("code", code);
            body.put("new_password", newPassword);
            ApiClient.getInstance(this).postObject("/auth/reset-password", body,
                    response -> {
                        Toast.makeText(this,
                                response.optString("message", "Mot de passe réinitialisé."),
                                Toast.LENGTH_LONG).show();
                        finish();
                    },
                    error -> show(ErrorUtils.message(error), true));
        } catch (JSONException e) {
            show("Erreur interne.", true);
        }
    }
}