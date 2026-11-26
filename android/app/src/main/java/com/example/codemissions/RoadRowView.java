package com.example.codemissions;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * One step of the roadmap: a glowing node on a winding path, with the mission title beside it.
 * The path continues above and below so that consecutive rows join into one curve.
 */
public class RoadRowView extends View {

    private static final float ROW_DP = 124f;
    private static final float NODE_DP = 28f;     // node radius

    private final float d;
    private final Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pTitle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pNum = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Drawable checkIcon, lockIcon;

    private Mission mission;
    private int number;
    private float xFrac = 0.3f, prevFrac = Float.NaN, nextFrac = Float.NaN;
    private boolean current, aboveDone, belowDone;
    private float pulse = 0f;
    private ValueAnimator animator;

    public RoadRowView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        Typeface rubik = ResourcesCompat.getFont(c, R.font.rubik);
        Typeface mono = ResourcesCompat.getFont(c, R.font.jetbrains_mono);
        pTitle.setTypeface(Typeface.create(rubik, Typeface.BOLD));
        pTitle.setTextSize(15 * d);
        pTitle.setColor(ContextCompat.getColor(c, R.color.text_primary));
        pSub.setTypeface(rubik);
        pSub.setTextSize(12 * d);
        pNum.setTypeface(Typeface.create(mono, Typeface.BOLD));
        pNum.setTextSize(18 * d);
        pNum.setTextAlign(Paint.Align.CENTER);
        pLine.setStyle(Paint.Style.STROKE);
        pLine.setStrokeCap(Paint.Cap.ROUND);
        pLine.setStrokeWidth(4 * d);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(3 * d);
        checkIcon = DrawableCompat.wrap(ContextCompat.getDrawable(c, R.drawable.ic_check).mutate());
        lockIcon = DrawableCompat.wrap(ContextCompat.getDrawable(c, R.drawable.ic_lock).mutate());
    }

    public void bind(Mission m, int number, float x, float prev, float next,
                     boolean current, boolean aboveDone) {
        this.mission = m;
        this.number = number;
        this.xFrac = x;
        this.prevFrac = prev;
        this.nextFrac = next;
        this.current = current;
        this.aboveDone = aboveDone;
        this.belowDone = m.isSolved();
        updateAnimator();
        invalidate();
    }

    private void updateAnimator() {
        if (current && isAttachedToWindow()) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setDuration(1300);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.addUpdateListener(a -> { pulse = (float) a.getAnimatedValue(); invalidate(); });
            }
            if (!animator.isStarted()) animator.start();
        } else if (animator != null) {
            animator.cancel();
            animator = null;
            pulse = 0f;
        }
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); updateAnimator(); }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) { animator.cancel(); animator = null; }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), (int) (ROW_DP * d));
    }

    private float nodeX(float frac, float w) {
        float pad = 56 * d;
        return pad + frac * (w - 2 * pad);
    }

    private void segment(Canvas c, float x0, float y0, float x1, float y1, boolean done) {
        path.reset();
        path.moveTo(x0, y0);
        float my = (y0 + y1) / 2f;
        path.cubicTo(x0, my, x1, my, x1, y1);
        if (done) {
            pLine.setPathEffect(null);
            pLine.setColor(ContextCompat.getColor(getContext(), R.color.py_blue_light));
        } else {
            pLine.setPathEffect(new DashPathEffect(new float[]{4 * d, 10 * d}, 0));
            pLine.setColor(ContextCompat.getColor(getContext(), R.color.border));
        }
        c.drawPath(path, pLine);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (mission == null) return;
        Context ctx = getContext();
        float w = getWidth(), h = getHeight();
        float cx = nodeX(xFrac, w), cy = h / 2f, r = NODE_DP * d;

        // path above / below (the halves of two rows meet at the same point on the row border)
        if (!Float.isNaN(prevFrac)) {
            float mx = (cx + nodeX(prevFrac, w)) / 2f;
            segment(c, mx, 0, cx, cy - r, aboveDone);
        }
        if (!Float.isNaN(nextFrac)) {
            float mx = (cx + nodeX(nextFrac, w)) / 2f;
            segment(c, cx, cy + r, mx, h, belowDone);
        }

        int white = ContextCompat.getColor(ctx, R.color.text_primary);
        int dark = ContextCompat.getColor(ctx, R.color.bg);
        int muted = ContextCompat.getColor(ctx, R.color.text_muted);
        boolean solved = mission.isSolved(), open = mission.isUnlocked();

        if (current) {   // soft pulsing halo
            pGlow.setColor(ContextCompat.getColor(ctx, R.color.py_yellow));
            pGlow.setAlpha((int) (70 - 40 * pulse));
            c.drawCircle(cx, cy, r + (8 + 8 * pulse) * d, pGlow);
        } else if (solved) {
            pGlow.setColor(ContextCompat.getColor(ctx, R.color.verdict_success));
            pGlow.setAlpha(40);
            c.drawCircle(cx, cy, r + 8 * d, pGlow);
        }

        if (solved) {
            pFill.setColor(ContextCompat.getColor(ctx, R.color.verdict_success));
            c.drawCircle(cx, cy, r, pFill);
            drawIcon(c, checkIcon, cx, cy, dark, 26);
        } else if (current) {
            pFill.setColor(ContextCompat.getColor(ctx, R.color.py_yellow));
            c.drawCircle(cx, cy, r, pFill);
            pNum.setColor(dark);
            c.drawText(String.valueOf(number), cx, cy + 6.5f * d, pNum);
        } else if (open) {
            pFill.setColor(ContextCompat.getColor(ctx, R.color.surface_alt));
            c.drawCircle(cx, cy, r, pFill);
            pStroke.setColor(ContextCompat.getColor(ctx, R.color.py_blue_light));
            c.drawCircle(cx, cy, r - 1.5f * d, pStroke);
            pNum.setColor(white);
            c.drawText(String.valueOf(number), cx, cy + 6.5f * d, pNum);
        } else {
            pFill.setColor(ContextCompat.getColor(ctx, R.color.surface));
            c.drawCircle(cx, cy, r, pFill);
            pStroke.setColor(ContextCompat.getColor(ctx, R.color.border));
            c.drawCircle(cx, cy, r - 1.5f * d, pStroke);
            drawIcon(c, lockIcon, cx, cy, muted, 22);
        }

        // label on the side with more room
        boolean labelRight = xFrac <= 0.5f;
        float gap = 16 * d, edge = 18 * d;
        float labelW = labelRight ? w - (cx + r + gap) - edge : (cx - r - gap) - edge;
        if (labelW < 40 * d) return;
        float left = labelRight ? cx + r + gap : cx - r - gap - labelW;

        pTitle.setAlpha(open ? 255 : 120);
        StaticLayout title = StaticLayout.Builder
                .obtain(mission.getTitle(), 0, mission.getTitle().length(), pTitle, (int) labelW)
                .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END)
                .setAlignment(labelRight ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_OPPOSITE)
                .build();

        String diff = mission.getDifficulty();
        String xp = "  +" + mission.getXp() + " XP";
        pSub.setColor(Mission.difficultyColor(ctx, diff));
        pSub.setAlpha(open ? 255 : 120);
        float wd = pSub.measureText(diff), wx = pSub.measureText(xp);
        float subH = 18 * d;

        float top = cy - (title.getHeight() + subH) / 2f;
        c.save();
        c.translate(left, top);
        title.draw(c);
        c.restore();

        float baseline = top + title.getHeight() + 14 * d;
        float sx = labelRight ? left : left + labelW - (wd + wx);
        c.drawText(diff, sx, baseline, pSub);
        pSub.setColor(ContextCompat.getColor(ctx, R.color.py_yellow));
        pSub.setAlpha(open ? 255 : 120);
        c.drawText(xp, sx + wd, baseline, pSub);
    }

    private void drawIcon(Canvas c, Drawable icon, float cx, float cy, int color, int sizeDp) {
        DrawableCompat.setTint(icon, color);
        int half = (int) (sizeDp * d / 2);
        icon.setBounds((int) cx - half, (int) cy - half, (int) cx + half, (int) cy + half);
        icon.draw(c);
    }
}
