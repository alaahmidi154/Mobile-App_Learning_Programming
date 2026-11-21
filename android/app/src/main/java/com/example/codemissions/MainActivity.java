package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private TextView txtStatus;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Session.isLoggedIn(this)) {
            openLevels();
            return;
        }

        setContentView(R.layout.activity_main);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        txtStatus = findViewById(R.id.txtLoginStatus);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        TextView btnForgot = findViewById(R.id.btnForgot);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> register());
        btnForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
    }

    private JSONObject credentials() throws JSONException {
        JSONObject body = new JSONObject();
        body.put("email", edtEmail.getText().toString().trim());
        body.put("password", edtPassword.getText().toString());
        return body;
    }

    private boolean fieldsOk() {
        if (edtEmail.getText().toString().trim().isEmpty()
                || edtPassword.getText().toString().isEmpty()) {
            txtStatus.setText("Please fill in the e-mail and the password.");
            return false;
        }
        return true;
    }

    private void login() {
        if (!fieldsOk()) return;
        txtStatus.setText("");

        if (ApiClient.USE_MOCK_DATA) {
            String email = edtEmail.getText().toString().trim();
            // Mock: an e-mail starting with "admin" gets the admin role
            String role = email.toLowerCase().startsWith("admin") ? "admin" : "player";
            Session.save(this, "mock-token", email, 0, role);
            openLevels();
            return;
        }

        btnLogin.setEnabled(false);
        try {
            ApiClient.getInstance(this).postObject("/auth/login", credentials(),
                    response -> {
                        btnLogin.setEnabled(true);
                        String token = response.optString("token", null);
                        if (token == null) {
                            txtStatus.setText("Invalid server response.");
                            return;
                        }
                        Session.save(this, token,
                                response.optString("email", edtEmail.getText().toString().trim()),
                                response.optInt("xp", 0),
                                response.optString("role", "player"));
                        openLevels();
                    },
                    error -> {
                        btnLogin.setEnabled(true);
                        txtStatus.setText(ErrorUtils.message(error));
                    });
        } catch (JSONException e) {
            btnLogin.setEnabled(true);
            txtStatus.setText("Internal error.");
        }
    }

    private void register() {
        if (!fieldsOk()) return;
        txtStatus.setText("");

        if (ApiClient.USE_MOCK_DATA) {
            Toast.makeText(this, "Account created (mock). You can log in now.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            ApiClient.getInstance(this).postObject("/auth/register", credentials(),
                    response -> Toast.makeText(this,
                            "Account created. You can log in now.",
                            Toast.LENGTH_LONG).show(),
                    error -> txtStatus.setText(ErrorUtils.message(error)));
        } catch (JSONException e) {
            txtStatus.setText("Internal error.");
        }
    }

    private void openLevels() {
        startActivity(new Intent(this, LevelsActivity.class));
        finish();
    }
}