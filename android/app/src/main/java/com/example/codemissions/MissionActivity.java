package com.example.codemissions;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Mission screen: statement, code editor, Tester / Soumettre, error line and hints. */
public class MissionActivity extends AppCompatActivity {

    /** What the API answers to /test and /submit. */
    private static class RunResult {
        String verdict, message;
        int ligne = -1, xpGagnes, xpTotal, tempsMs;
        boolean apercu;
        boolean detailsVisible;
        String entree, attendu, obtenu;
    }

    private static final String[][] SYMBOLS = {
            {"Tab", "    ", "0"}, {":", ":", "0"}, {"( )", "()", "1"}, {"[ ]", "[]", "1"},
            {"{ }", "{}", "1"}, {"\"", "\"\"", "1"}, {"'", "''", "1"}, {"=", " = ", "0"},
            {"==", " == ", "0"}, {"!=", " != ", "0"}, {"+", " + ", "0"}, {"-", " - ", "0"},
            {"*", " * ", "0"}, {"/", " / ", "0"}, {"//", " // ", "0"}, {"%", " % ", "0"},
            {"<", " < ", "0"}, {">", " > ", "0"}, {"#", "# ", "0"}, {",", ", ", "0"},
            {".", ".", "0"}, {"_", "_", "0"}};

    private int missionId;
    private int missionXp;
    private String missionDifficulty = "";
    private String codeDepart = "";
    private String missionHint;
    private String firstInput, firstOutput;

    private TextView txtTitle, txtMeta, txtDesc, txtExamples;
    private View boxExamples, cardResult, cardHint;
    private TextView txtResultHeader, txtResultBody, txtHintLabel, txtHint, btnNextHint;
    private CodeEditText edtCode;
    private ScrollView scroll;
    private MaterialButton btnTest, btnSubmit;

    private RunResult last;
    private int hintLevel = 0;
    private boolean running = false;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);
        Screen.fit(this);

        txtTitle = findViewById(R.id.txtMissionTitle);
        txtMeta = findViewById(R.id.txtMissionMeta);
        txtDesc = findViewById(R.id.txtMissionDescription);
        txtExamples = findViewById(R.id.txtExamples);
        boxExamples = findViewById(R.id.boxExamples);
        cardResult = findViewById(R.id.cardResult);
        cardHint = findViewById(R.id.cardHint);
        txtResultHeader = findViewById(R.id.txtResultHeader);
        txtResultBody = findViewById(R.id.txtResultBody);
        txtHintLabel = findViewById(R.id.txtHintLabel);
        txtHint = findViewById(R.id.txtHint);
        btnNextHint = findViewById(R.id.btnNextHint);
        edtCode = findViewById(R.id.edtCode);
        scroll = findViewById(R.id.scrollMission);
        btnTest = findViewById(R.id.btnTest);
        btnSubmit = findViewById(R.id.btnSubmit);

        missionId = getIntent().getIntExtra("mission_id", -1);
        missionXp = getIntent().getIntExtra("mission_xp", 0);
        String d = getIntent().getStringExtra("mission_difficulty");
        missionDifficulty = d == null ? "" : d;
        String t = getIntent().getStringExtra("mission_title");
        txtTitle.setText(t == null ? "Mission" : t);
        updateMeta();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnTest.setOnClickListener(v -> run(false));
        btnSubmit.setOnClickListener(v -> run(true));
        findViewById(R.id.btnReset).setOnClickListener(v -> confirmReset());
        findViewById(R.id.btnHint).setOnClickListener(v -> nextHint());
        btnNextHint.setOnClickListener(v -> nextHint());
        buildSymbolBar();

        loadMission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (loaded) {
            drafts().edit().putString("m" + missionId, edtCode.getText().toString()).apply();
        }
    }

    private SharedPreferences drafts() {
        return getSharedPreferences("drafts_" + Session.getName(this), MODE_PRIVATE);
    }

    private void updateMeta() {
        txtMeta.setText(missionDifficulty + " · +" + missionXp + " XP");
        txtMeta.setTextColor(Mission.difficultyColor(this, missionDifficulty));
    }

    // ---------------------------------------------------------------- loading

    private void loadMission() {
        ApiClient.getInstance(this).getObject("/missions/" + missionId,
                (JSONObject o) -> {
                    txtTitle.setText(o.optString("titre", txtTitle.getText().toString()));
                    txtDesc.setText(o.optString("enonce"));
                    codeDepart = o.optString("codeDepart");
                    missionXp = o.optInt("xp", missionXp);
                    String diff = o.optString("difficulte", "");
                    if (!diff.isEmpty()) missionDifficulty = diff;
                    updateMeta();
                    missionHint = o.isNull("indice") ? null : o.optString("indice", null);

                    JSONArray ex = o.optJSONArray("exemples");
                    StringBuilder sb = new StringBuilder();
                    if (ex != null) {
                        for (int i = 0; i < ex.length(); i++) {
                            JSONObject e = ex.optJSONObject(i);
                            if (e == null) continue;
                            String in = e.optString("entree");
                            String out = e.optString("sortieAttendue");
                            if (i == 0) { firstInput = in; firstOutput = out; }
                            if (sb.length() > 0) sb.append("\n\n");
                            sb.append("Entrée :\n").append(in.isEmpty() ? "(aucune)" : in)
                                    .append("\nSortie attendue :\n").append(out);
                        }
                    }
                    if (sb.length() > 0) {
                        txtExamples.setText(sb);
                        boxExamples.setVisibility(View.VISIBLE);
                    }

                    String draft = drafts().getString("m" + missionId, null);
                    edtCode.setText(draft != null && !draft.trim().isEmpty() ? draft : codeDepart);
                    loaded = true;
                },
                error -> {
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    txtDesc.setText(ErrorUtils.message(error));
                });
    }

    // ---------------------------------------------------------------- symbols

    private void buildSymbolBar() {
        LinearLayout row = findViewById(R.id.symbolRow);
        float d = getResources().getDisplayMetrics().density;
        for (String[] s : SYMBOLS) {
            TextView b = new TextView(this);
            b.setText(s[0]);
            b.setTextColor(ContextCompat.getColor(this, R.color.p_cream));
            b.setTextSize(14);
            b.setTypeface(ResourcesCompat.getFont(this, R.font.jetbrains_mono));
            b.setGravity(Gravity.CENTER);
            b.setBackgroundResource(R.drawable.bg_card_alt);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    s[0].length() > 2 ? (int) (52 * d) : (int) (42 * d), (int) (34 * d));
            p.setMargins((int) (3 * d), 0, (int) (3 * d), 0);
            b.setLayoutParams(p);
            b.setOnClickListener(v -> {
                edtCode.requestFocus();
                edtCode.insertSnippet(s[1], Integer.parseInt(s[2]));
            });
            row.addView(b);
        }
    }

    // ---------------------------------------------------------------- reset

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Réinitialiser le code ?")
                .setMessage("Ton code actuel sera remplacé par le code de départ.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Réinitialiser", (dlg, w) -> {
                    edtCode.setText(codeDepart);
                    edtCode.setErrorLine(-1);
                    cardResult.setVisibility(View.GONE);
                    cardHint.setVisibility(View.GONE);
                    last = null;
                    hintLevel = 0;
                })
                .show();
    }

    // ---------------------------------------------------------------- running

    private void setRunning(boolean r) {
        running = r;
        btnTest.setEnabled(!r);
        btnSubmit.setEnabled(!r);
        btnTest.setText(r ? "…" : "Tester");
        btnSubmit.setText(r ? "Correction…" : "Soumettre");
    }

    private void run(boolean submit) {
        if (running) return;
        String code = edtCode.getText().toString();
        if (code.trim().isEmpty()) {
            Toast.makeText(this, "Écris du code d'abord.", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject body = new JSONObject();
        try { body.put("code", code); } catch (JSONException ignored) { }

        setRunning(true);
        ApiClient.getInstance(this).postObject(
                "/missions/" + missionId + (submit ? "/submit" : "/test"), body,
                (JSONObject o) -> {
                    setRunning(false);
                    onResult(parse(o), submit);
                },
                error -> {
                    setRunning(false);
                    if (ErrorUtils.handleUnauthorized(this, error)) return;
                    new ResultDialog(this).kind(ResultDialog.Kind.FAIL)
                            .title("Oups")
                            .message(ErrorUtils.message(error))
                            .primary("OK", null)
                            .show();
                });
    }

    /**
     * The server says "réussi" / "échoué" / "temps dépassé" / "erreur". Accents can arrive in
     * different Unicode forms, so compare without accents and map to the exact words we test.
     */
    private static String canonicalVerdict(String raw) {
        String plain = java.text.Normalizer.normalize(raw == null ? "" : raw, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(java.util.Locale.ROOT);
        switch (plain) {
            case "reussi":         return "réussi";
            case "echoue":         return "échoué";
            case "temps depasse":  return "temps dépassé";
            default:               return "erreur";
        }
    }

    private RunResult parse(JSONObject o) {
        RunResult r = new RunResult();
        r.verdict = canonicalVerdict(o.optString("verdict"));
        r.message = o.optString("message");
        r.ligne = o.isNull("ligne") ? -1 : o.optInt("ligne", -1);
        r.xpGagnes = o.optInt("xpGagnes");
        r.xpTotal = o.optInt("xpTotal", Session.getXp(this));
        r.tempsMs = o.optInt("tempsExecutionMs");
        r.apercu = o.optBoolean("apercu");
        JSONObject det = o.optJSONObject("details");
        if (det != null) {
            r.detailsVisible = det.optBoolean("visible");
            r.entree = det.isNull("entree") ? null : det.optString("entree");
            r.attendu = det.isNull("attendu") ? null : det.optString("attendu");
            r.obtenu = det.isNull("obtenu") ? null : det.optString("obtenu");
        }
        return r;
    }

    private void onResult(RunResult r, boolean submit) {
        last = r;
        hintLevel = 0;
        cardHint.setVisibility(View.GONE);
        boolean ok = "réussi".equals(r.verdict);

        showInline(r);
        edtCode.setErrorLine(!ok && r.ligne > 0 ? r.ligne : -1);

        if (submit && ok) {
            int oldLevel = Session.getLevel(this);
            Session.setProgress(this, r.xpTotal, Session.levelFor(r.xpTotal));
            int newLevel = Session.levelFor(r.xpTotal);
            String msg = r.xpGagnes > 0
                    ? "Tu as " + r.xpTotal + " XP au total."
                    : "Mission déjà réussie : pas de XP supplémentaire.";
            if (newLevel > oldLevel) msg += "\nNouveau niveau : " + newLevel + " !";
            ResultDialog dlg = new ResultDialog(this).kind(ResultDialog.Kind.SUCCESS)
                    .title(r.xpGagnes > 0 ? "Mission réussie !" : "Bien joué !")
                    .message(msg)
                    .primary("Continuer", this::finish)          // back to the roadmap
                    .secondary("Revoir mon code", null);
            if (r.xpGagnes > 0) dlg.countXp(r.xpGagnes); else dlg.big("0 XP");
            dlg.show();
        } else if (ok) {
            new ResultDialog(this).kind(ResultDialog.Kind.SUCCESS)
                    .title("Les exemples passent")
                    .big("Exemples OK")
                    .message("Ton code donne la bonne réponse sur les exemples. "
                            + "Soumets-le pour le tester sur tous les cas et gagner les XP.")
                    .primary("Soumettre", () -> run(true))
                    .secondary("Fermer", null)
                    .show();
        } else {
            String title;
            switch (r.verdict == null ? "" : r.verdict) {
                case "erreur":        title = r.ligne > 0 ? "Erreur ligne " + r.ligne : "Erreur dans ton code"; break;
                case "temps dépassé": title = "Trop lent"; break;
                default:              title = "Pas encore"; break;
            }
            new ResultDialog(this).kind(ResultDialog.Kind.FAIL)
                    .title(title)
                    .message(shortFail(r))
                    .detail(detailText(r))
                    .primary("Corriger mon code", this::scrollToProblem)
                    .secondary("Voir un indice", this::nextHint)
                    .show();
        }
    }

    // ---------------------------------------------------------------- explanations

    private String shortFail(RunResult r) {
        if ("erreur".equals(r.verdict)) return HintHelper.explain(r.message);
        if ("temps dépassé".equals(r.verdict))
            return "Ton programme a dépassé la limite de temps (5 s). Une boucle ne s'arrête peut-être jamais.";
        if (r.apercu) return "Un des exemples ne donne pas la bonne sortie.";
        return "Ta sortie ne correspond pas à ce qui est attendu pour au moins un test.";
    }

    private String detailText(RunResult r) {
        if ("erreur".equals(r.verdict)) return r.message;
        if ("échoué".equals(r.verdict) && r.detailsVisible) {
            return "Entrée   : " + oneLine(r.entree)
                    + "\nAttendu  : " + oneLine(r.attendu)
                    + "\nObtenu   : " + (r.obtenu == null || r.obtenu.isEmpty() ? "(rien)" : oneLine(r.obtenu));
        }
        if ("échoué".equals(r.verdict)) {
            return "Un test caché a échoué : pense aux cas limites (0, nombres négatifs, mot sans voyelle…).";
        }
        return "";
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.trim().replace("\n", " ↵ ");
    }

    private void showInline(RunResult r) {
        cardResult.setVisibility(View.VISIBLE);
        boolean ok = "réussi".equals(r.verdict);
        String header;
        if (ok) header = r.apercu ? "Exemples réussis" : "Réussi";
        else if ("erreur".equals(r.verdict)) header = r.ligne > 0 ? "Erreur à la ligne " + r.ligne : "Erreur";
        else if ("temps dépassé".equals(r.verdict)) header = "Temps dépassé";
        else header = "Sortie incorrecte";
        txtResultHeader.setText(header);
        txtResultHeader.setTextColor(ContextCompat.getColor(this,
                ok ? R.color.verdict_success : R.color.verdict_error));

        StringBuilder sb = new StringBuilder();
        if (ok) sb.append("Exécuté en ").append(r.tempsMs).append(" ms.");
        else {
            String d = detailText(r);
            if (!d.isEmpty()) sb.append(d).append("\n\n");
            sb.append(shortFail(r));
        }
        txtResultBody.setText(sb.toString().trim());
    }

    private void scrollToProblem() {
        edtCode.requestFocus();
        if (edtCode.getErrorLine() > 0) {
            final int y = ((View) edtCode.getParent()).getTop() + edtCode.lineTop(edtCode.getErrorLine());
            scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, y - 160)));
        }
    }

    // ---------------------------------------------------------------- hints

    private void nextHint() {
        if (hintLevel < 3) hintLevel++;
        String text = HintHelper.hint(hintLevel,
                last == null ? null : last.verdict,
                last == null ? null : last.message,
                missionHint, firstInput, firstOutput);
        txtHintLabel.setText("INDICE " + hintLevel + "/3");
        txtHint.setText(text);
        btnNextHint.setVisibility(hintLevel < 3 ? View.VISIBLE : View.GONE);
        cardHint.setVisibility(View.VISIBLE);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
