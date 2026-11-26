package com.example.codemissions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small code editor: monospace, line numbers, Python colours, auto-indent after ':'
 * and a red line for the line where the last run failed.
 */
public class CodeEditText extends AppCompatEditText {

    private static class SyntaxSpan extends ForegroundColorSpan {
        SyntaxSpan(int color) { super(color); }
    }

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "False", "None", "True", "and", "as", "assert", "break", "class", "continue", "def",
            "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import",
            "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try",
            "while", "with", "yield"));
    private static final Set<String> BUILTINS = new HashSet<>(Arrays.asList(
            "print", "input", "int", "str", "float", "len", "range", "list", "dict", "set", "tuple",
            "sum", "min", "max", "abs", "round", "sorted", "reversed", "enumerate", "zip", "map",
            "bool", "type", "isinstance", "open", "any", "all", "pow", "divmod"));

    private static final Pattern TOKENS = Pattern.compile(
            "(#[^\\n]*)"                                                     // 1 comment
            + "|(\"\"\"[\\s\\S]*?(?:\"\"\"|$)|'''[\\s\\S]*?(?:'''|$)"        // 2 strings
            + "|\"(?:\\\\.|[^\"\\\\\\n])*\"?|'(?:\\\\.|[^'\\\\\\n])*'?)"
            + "|\\b(\\d+(?:\\.\\d+)?)\\b"                                    // 3 number
            + "|\\b([A-Za-z_][A-Za-z_0-9]*)\\b");                            // 4 word

    private final float d;
    private final Paint gutterBg = new Paint();
    private final Paint gutterLine = new Paint();
    private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numErr = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint errBg = new Paint();
    private final Paint curBg = new Paint();
    private final int gutterW;

    private int errorLine = -1;      // 1-based, -1 = none
    private boolean formatting;
    private int pendingIndentAt = -1;

    public CodeEditText(Context c) { this(c, null); }

    public CodeEditText(Context c, AttributeSet attrs) {
        super(c, attrs, androidx.appcompat.R.attr.editTextStyle);
        d = c.getResources().getDisplayMetrics().density;
        gutterW = (int) (42 * d);

        Typeface mono = ResourcesCompat.getFont(c, R.font.jetbrains_mono);
        setTypeface(mono);
        setTextSize(14);
        setLineSpacing(0, 1.15f);
        setTextColor(ContextCompat.getColor(c, R.color.editor_text));
        setHintTextColor(ContextCompat.getColor(c, R.color.editor_hint));
        setBackground(null);
        setGravity(Gravity.TOP | Gravity.START);
        setHorizontallyScrolling(true);
        setPadding(gutterW + (int) (12 * d), (int) (12 * d), (int) (16 * d), (int) (12 * d));
        // No suggestions / auto-correct / smart quotes: they break code
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        setSingleLine(false);

        gutterBg.setColor(ContextCompat.getColor(c, R.color.editor_gutter));
        gutterLine.setColor(ContextCompat.getColor(c, R.color.border));
        num.setTypeface(mono);
        num.setTextSize(12 * d);
        num.setTextAlign(Paint.Align.RIGHT);
        num.setColor(ContextCompat.getColor(c, R.color.code_comment));
        numErr.setTypeface(Typeface.create(mono, Typeface.BOLD));
        numErr.setTextSize(12 * d);
        numErr.setTextAlign(Paint.Align.RIGHT);
        numErr.setColor(ContextCompat.getColor(c, R.color.verdict_error));
        errBg.setColor(ContextCompat.getColor(c, R.color.code_error_line));
        curBg.setColor(ContextCompat.getColor(c, R.color.code_current_line));

        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int cnt, int aft) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (formatting) return;
                pendingIndentAt = (before == 0 && count == 1 && s.charAt(start) == '\n') ? start + 1 : -1;
            }

            @Override public void afterTextChanged(Editable e) {
                if (formatting) return;
                formatting = true;
                try {
                    if (pendingIndentAt >= 0) autoIndent(e, pendingIndentAt);
                    pendingIndentAt = -1;
                    highlight(e);
                } finally {
                    formatting = false;
                }
                if (errorLine > 0) { errorLine = -1; }   // the player is editing: forget the old error
            }
        });
    }

    /** Marks a line (1-based) as the one that failed; 0 or less clears it. */
    public void setErrorLine(int line) {
        errorLine = line;
        invalidate();
    }

    public int getErrorLine() { return errorLine; }

    /** Y position (in pixels, inside this view) of a 1-based line, to scroll to it. */
    public int lineTop(int line) {
        Layout l = getLayout();
        if (l == null || line < 1 || line > l.getLineCount()) return 0;
        return getExtendedPaddingTop() + l.getLineTop(line - 1);
    }

    /** Inserts text at the cursor; [cursorBack] = how many characters to move the cursor back. */
    public void insertSnippet(String text, int cursorBack) {
        Editable e = getText();
        if (e == null) return;
        int a = Math.max(0, Math.min(getSelectionStart(), getSelectionEnd()));
        int b = Math.max(0, Math.max(getSelectionStart(), getSelectionEnd()));
        e.replace(a, b, text);
        setSelection(a + text.length() - cursorBack);
    }

    private void autoIndent(Editable e, int pos) {
        int lineStart = pos - 1;
        while (lineStart > 0 && e.charAt(lineStart - 1) != '\n') lineStart--;
        int i = lineStart;
        while (i < pos - 1 && e.charAt(i) == ' ') i++;
        int indent = i - lineStart;
        String prev = e.subSequence(lineStart, pos - 1).toString().trim();
        if (prev.endsWith(":")) indent += 4;
        if (indent > 0) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < indent; k++) sb.append(' ');
            e.insert(pos, sb);
            setSelection(pos + indent);
        }
    }

    private void highlight(Editable e) {
        for (SyntaxSpan s : e.getSpans(0, e.length(), SyntaxSpan.class)) e.removeSpan(s);
        Context c = getContext();
        int cComment = ContextCompat.getColor(c, R.color.code_comment);
        int cString = ContextCompat.getColor(c, R.color.code_string);
        int cNumber = ContextCompat.getColor(c, R.color.code_number);
        int cKeyword = ContextCompat.getColor(c, R.color.code_keyword);
        int cBuiltin = ContextCompat.getColor(c, R.color.code_builtin);

        Matcher m = TOKENS.matcher(e);
        while (m.find()) {
            int color;
            if (m.group(1) != null) color = cComment;
            else if (m.group(2) != null) color = cString;
            else if (m.group(3) != null) color = cNumber;
            else {
                String w = m.group(4);
                if (KEYWORDS.contains(w)) color = cKeyword;
                else if (BUILTINS.contains(w)) color = cBuiltin;
                else continue;
            }
            e.setSpan(new SyntaxSpan(color), m.start(), m.end(), Editable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Layout l = getLayout();
        if (l == null) { super.onDraw(canvas); return; }
        int sx = getScrollX();
        int top = getExtendedPaddingTop();

        // highlighted lines (behind the text)
        int sel = getSelectionStart();
        if (sel >= 0 && isFocused()) {
            int line = l.getLineForOffset(sel);
            canvas.drawRect(sx, top + l.getLineTop(line), sx + getWidth(), top + l.getLineBottom(line), curBg);
        }
        if (errorLine > 0 && errorLine <= l.getLineCount()) {
            int line = errorLine - 1;
            canvas.drawRect(sx, top + l.getLineTop(line), sx + getWidth(), top + l.getLineBottom(line), errBg);
        }

        super.onDraw(canvas);

        // gutter with line numbers (on top, so long lines slide under it)
        int sy = getScrollY();
        canvas.drawRect(sx, sy, sx + gutterW, sy + getHeight(), gutterBg);
        canvas.drawRect(sx + gutterW - 1, sy, sx + gutterW, sy + getHeight(), gutterLine);
        for (int i = 0; i < l.getLineCount(); i++) {
            float y = top + l.getLineBaseline(i);
            canvas.drawText(String.valueOf(i + 1), sx + gutterW - 10 * d, y,
                    (i + 1 == errorLine) ? numErr : num);
        }
    }
}
