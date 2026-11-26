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
    private Button btnSubmit, btnToggleMode, btnForgot;
    private boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already logged in: go straight to the roadmap
        if (Session.isLoggedIn(this)) {
            openRoadmap();
            return;
        }

        setContentView(R.layout.activity_main);
        Screen.fit(this);

        tilName = findViewById(R.id.tilName);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        txtStatus = findViewById(R.id.txtLoginStatus);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        btnForgot = findViewById(R.id.btnForgot);

        btnForgot.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("email", edtEmail.getText().toString().trim());
            startActivity(intent);
        });
        btnSubmit.setOnClickListener(v -> submit());
        btnToggleMode.setOnClickListener(v -> {
            registerMode = !registerMode;
            updateMode();
        });
        updateMode();

        btnServer = findViewById(R.id.btnServer);
        showServer();
        btnServer.setOnClickListener(v -> askServer());
    }

    private TextView btnServer;

    private void showServer() {
        String shortUrl = ApiClient.getServerShort(this);
        boolean emulator = ApiClient.DEFAULT_BASE_URL.equals(ApiClient.getServerUrl(this));
        btnServer.setText("serveur : " + (emulator ? "émulateur (" + shortUrl + ")" : shortUrl) + "  ·  modifier");
    }

    /** Lets the phone find the PC without rebuilding the app: type the PC's Wi-Fi address. */
    private void askServer() {
        final EditText input = new EditText(this);
        input.setHint("ex : 192.168.1.20");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(ApiClient.getServerShort(this));
        input.setSelectAllOnFocus(true);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Adresse du serveur")
                .setMessage("Adresse IP du PC qui lance l'API (même Wi-Fi). Laisse vide pour l'émulateur.")
                .setView(box)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    ApiClient.setServer(this, input.getText().toString());
                    showServer();
                    txtStatus.setText("");
                })
                .show();
    }

    /** Same screen for login and register: only the name field and the texts change. */
    private void updateMode() {
        tilName.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        btnForgot.setVisibility(registerMode ? View.GONE : View.VISIBLE);
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

        final String name = edtName.getText().toString().trim();

        JSONObject body = new JSONObject();
        try {
            // API contract: login {email, password}; register {nom, email, password}
            if (registerMode) body.put("nom", name);
            body.put("email", edtEmail.getText().toString().trim());
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
                    // Both endpoints answer {token, nom, xpTotal, niveauJoueur, role}
                    String token = response.optString("token", "");
                    if (token.isEmpty()) {
                        txtStatus.setText("Réponse invalide du serveur.");
                        return;
                    }
                    Session.save(this, token,
                            response.optString("nom", name),
                            response.optInt("xpTotal", 0),
                            response.optInt("niveauJoueur", 1),
                            response.optString("role", "joueur"));
                    openRoadmap();
                },
                error -> {
                    setBusy(false);
                    txtStatus.setText(ErrorUtils.message(error));
                });
    }

    private void openRoadmap() {
        startActivity(new Intent(this, RoadmapActivity.class));
        finish();
    }
}
