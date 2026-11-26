package com.example.codemissions;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Full-screen popup shown after Tester / Soumettre (success or failure). */
public class ResultDialog {

    public enum Kind { SUCCESS, FAIL }

    private final Activity activity;
    private Kind kind = Kind.SUCCESS;
    private String title = "", big = "", message = "", detail = "";
    private String primaryText = "Continuer", secondaryText = null;
    private Runnable onPrimary, onSecondary;
    private int countTo = -1;     // when >= 0, the big text counts up to "+N XP"

    public ResultDialog(Activity activity) { this.activity = activity; }

    public ResultDialog kind(Kind k) { kind = k; return this; }
    public ResultDialog title(String t) { title = t; return this; }
    public ResultDialog big(String b) { big = b; return this; }
    public ResultDialog countXp(int xp) { countTo = xp; return this; }
    public ResultDialog message(String m) { message = m == null ? "" : m; return this; }
    public ResultDialog detail(String d) { detail = d == null ? "" : d; return this; }
    public ResultDialog primary(String text, Runnable r) { primaryText = text; onPrimary = r; return this; }
    public ResultDialog secondary(String text, Runnable r) { secondaryText = text; onSecondary = r; return this; }

    public void show() {
        Dialog dialog = new Dialog(activity, R.style.Theme_App_Result);
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_result, null, false);
        dialog.setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.setCancelable(true);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(),
                    bars.bottom + (int) (24 * v.getResources().getDisplayMetrics().density));
            return WindowInsetsCompat.CONSUMED;
        });

        boolean ok = kind == Kind.SUCCESS;
        TextView txtTitle = root.findViewById(R.id.txtResultTitle);
        TextView txtBig = root.findViewById(R.id.txtResultBig);
        TextView txtMsg = root.findViewById(R.id.txtResultMsg);
        TextView txtDetail = root.findViewById(R.id.txtResultDetail);
        ImageView icon = root.findViewById(R.id.resultIcon);
        View glow = root.findViewById(R.id.resultGlow);
        View diamond = root.findViewById(R.id.diamond);
        View diamondBack = root.findViewById(R.id.diamondBack);

        txtTitle.setText(title);
        txtTitle.setTextColor(ContextCompat.getColor(activity, ok ? R.color.p_cream : R.color.verdict_error));
        txtMsg.setText(message);
        txtBig.setText(big);
        txtBig.setVisibility(big.isEmpty() && countTo < 0 ? View.GONE : View.VISIBLE);
        txtBig.setTextColor(ContextCompat.getColor(activity, ok ? R.color.p_cream : R.color.text_primary));
        if (!detail.isEmpty()) {
            txtDetail.setText(detail);
            txtDetail.setVisibility(View.VISIBLE);
        }
        icon.setImageResource(ok ? R.drawable.ic_star : R.drawable.ic_x);
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(activity,
                ok ? R.color.p_cream : R.color.verdict_error)));
        glow.setBackgroundResource(ok ? R.drawable.glow_purple : R.drawable.glow_rose);

        TextView primary = root.findViewById(R.id.btnPrimary);
        primary.setText(primaryText);
        primary.setOnClickListener(v -> { dialog.dismiss(); if (onPrimary != null) onPrimary.run(); });
        TextView secondary = root.findViewById(R.id.btnSecondary);
        if (secondaryText == null) secondary.setVisibility(View.GONE);
        else {
            secondary.setText(secondaryText);
            secondary.setOnClickListener(v -> { dialog.dismiss(); if (onSecondary != null) onSecondary.run(); });
        }
        dialog.setOnCancelListener(d -> { if (onSecondary != null && secondaryText != null) onSecondary.run(); });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Entrance: icon pops in, diamonds turn slowly, XP counts up
        icon.setScaleX(0f); icon.setScaleY(0f);
        icon.animate().scaleX(1f).scaleY(1f).setDuration(650).setStartDelay(150)
                .setInterpolator(new OvershootInterpolator(2.2f)).start();
        glow.setAlpha(0f);
        glow.animate().alpha(1f).setDuration(700).start();
        ObjectAnimator.ofFloat(diamond, View.ROTATION, -40f, -10f).setDuration(900).start();
        ObjectAnimator.ofFloat(diamondBack, View.ROTATION, 50f, 14f).setDuration(900).start();
        if (countTo >= 0) {
            ValueAnimator a = ValueAnimator.ofInt(0, countTo);
            a.setDuration(Math.min(1400, 400 + countTo * 8L));
            a.setStartDelay(300);
            a.addUpdateListener(an -> txtBig.setText("+" + an.getAnimatedValue() + " XP"));
            a.start();
        }
    }
}
