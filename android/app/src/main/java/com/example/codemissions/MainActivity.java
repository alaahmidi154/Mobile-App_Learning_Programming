package com.example.codemissions;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private View tilName;
    private EditText edtName, edtEmail, edtPassword;
    private TextView txtSubtitle, txtStatus;
    private Button btnSubmit, btnToggleMode;
    private boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already logged in: go straight to the levels list
        if (Session.isLoggedIn(this)) {
            openLevels();
            return;
        }

        setContentView(R.layout.activity_main);

        tilName = findViewById(R.id.tilName);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        txtStatus = findViewById(R.id.txtLoginStatus);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnToggleMode = findViewById(R.id.btnToggleMode);

        // The API has no password-reset endpoint yet: the screen is only reachable in demo mode
        TextView btnForgot = findViewById(R.id.btnForgot);
        if (ApiClient.USE_MOCK_DATA) {
            btnForgot.setVisibility(View.VISIBLE);
            btnForgot.setOnClickListener(v ->
                    startActivity(new Intent(this, ResetPasswordActivity.class)));
        }

        btnSubmit.setOnClickListener(v -> submit());
        btnToggleMode.setOnClickListener(v -> {
            registerMode = !registerMode;
            updateMode();
        });
        updateMode();
    }

    /** Same screen for login and register: only the name field and the texts change. */
    private void updateMode() {
        tilName.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        txtSubtitle.setText(registerMode ? "Créer un compte" : "Connexion");
        btnSubmit.setText(registerMode ? "Créer mon compte" : "Se connecter");
        btnToggleMode.setText(registerMode
                ? "Déjà un compte ? Se connecter"
                : "Pas de compte ? S'inscrire");
        txtStatus.setText("");
    }

    private boolean fieldsOk() {
        String email = edtEmail.getText().toString().trim();
        if (registerMode && edtName.getText().toString().trim().isEmpty()) {
            txtStatus.setText("Entrez un nom d'utilisateur.");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtStatus.setText("E-mail invalide.");
            return false;
        }
        if (edtPassword.getText().toString().isEmpty()) {
            txtStatus.setText("Entrez un mot de passe.");
            return false;
        }
        return true;
    }

    private void setBusy(boolean busy) {
        btnSubmit.setEnabled(!busy);
        btnToggleMode.setEnabled(!busy);
    }

    private void submit() {
        if (!fieldsOk()) return;
        txtStatus.setText("");

        final String email = edtEmail.getText().toString().trim();
        final String name = edtName.getText().toString().trim();

        if (ApiClient.USE_MOCK_DATA) {
            String shown = registerMode ? name : email.split("@")[0];
            // Demo: an e-mail starting with "admin" gets the admin role
            String role = email.toLowerCase().startsWith("admin") ? "admin" : "joueur";
            Session.save(this, "mock-token", shown, 0, 1, role);
            openLevels();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            // API contract: login {email, password}; register {nom, email, password}
            if (registerMode) body.put("nom", name);
            body.put("email", email);
            body.put("password", edtPassword.getText().toString());
        } catch (JSONException e) {
            txtStatus.setText("Erreur interne.");
            return;
        }

        setBusy(true);
        String path = registerMode ? "/auth/register" : "/auth/login";
        ApiClient.getInstance(this).postObject(path, body,
                response -> {
                    setBusy(false);
                    // Both endpoints answer {token, nom, xpTotal, niveauJoueur}
                    String token = response.optString("token", "");
                    if (token.isEmpty()) {
                        txtStatus.setText("Réponse invalide du serveur.");
                        return;
                    }
                    Session.save(this, token,
                            response.optString("nom", name),
                            response.optInt("xpTotal", 0),
                            response.optInt("niveauJoueur", 1));
                    openLevels();
                },
                error -> {
                    setBusy(false);
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }

    private void openLevels() {
        startActivity(new Intent(this, LevelsActivity.class));
        finish();
    }
}