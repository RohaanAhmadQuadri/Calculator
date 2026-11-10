package com.example.calculator;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String PREFS = "calc_prefs";
    private static final String KEY_HISTORY = "history";

    private TextView tvExpression, tvResult;
    private ImageButton btnSci;
    private Button btnMode;
    private View sciPanel;

    private final StringBuilder expr = new StringBuilder();
    private final List<String[]> history = new ArrayList<>(); // {expression, result}
    private boolean degMode = true;
    private boolean sciVisible = false;
    private boolean justCalculated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);
        sciPanel = findViewById(R.id.sciPanel);
        btnSci = findViewById(R.id.btnSci);
        btnMode = findViewById(R.id.btnMode);

        // Every calculator button has a tag; the top bar buttons have none
        attachListeners(findViewById(android.R.id.content));

        btnSci.setOnClickListener(v -> toggleScientific());
        findViewById(R.id.btnHistory).setOnClickListener(v -> showHistory());

        loadHistory();
    }

    private void attachListeners(View view) {
        if (view instanceof Button) {
            if (view.getTag() != null) view.setOnClickListener(this);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachListeners(group.getChildAt(i));
            }
        }
    }

    private void toggleScientific() {
        sciVisible = !sciVisible;
        sciPanel.setVisibility(sciVisible ? View.VISIBLE : View.GONE);
        // orange icon = scientific off, white icon = scientific on
        btnSci.setColorFilter(sciVisible ? 0xFFFFFFFF : 0xFFFF9500);
    }

    // ---------- Button clicks ----------

    @Override
    public void onClick(View v) {
        String key = (String) v.getTag();

        // After "=", typing a new number/function starts fresh; an operator continues
        if (justCalculated && startsNewInput(key)) expr.setLength(0);
        justCalculated = false;

        switch (key) {
            case "AC":
                expr.setLength(0);
                tvExpression.setText("");
                tvResult.setText("0");
                return;

            case "DEL":
                deleteLast();
                break;

            case "=":
                calculateFinal();
                return;

            case "+": case "−": case "×": case "÷":
                handleOperator(key);
                break;

            case "%": case "²": case "!": case "^":
                if (endsValue(lastChar())) expr.append(key);
                break;

            case ".":
                if (canAddDecimal()) {
                    if (!Character.isDigit(lastChar())) expr.append("0");
                    expr.append(".");
                }
                break;

            case "(":
                expr.append("(");
                break;

            case ")":
                if (countOpen() > 0 && lastChar() != '(' && !isOperator(lastChar())) expr.append(")");
                break;

            case "π": case "e":
                expr.append(key);
                break;

            case "sin": case "cos": case "tan": case "ln": case "log": case "√":
                expr.append(key).append("(");
                break;

            case "inv": {
                String s = stripTrailing(expr.toString());
                if (!s.isEmpty()) {
                    expr.setLength(0);
                    expr.append("1÷(").append(s).append(")");
                }
                break;
            }

            case "mode":
                degMode = !degMode;
                btnMode.setText(degMode ? "DEG" : "RAD");
                break;

            default: // digits and "00"
                expr.append(key);
                break;
        }

        tvExpression.setText(expr.toString());
        showLivePreview();
    }

    private boolean startsNewInput(String key) {
        return Character.isDigit(key.charAt(0)) || key.equals(".") || key.equals("(")
                || key.equals("π") || key.equals("e") || key.equals("sin") || key.equals("cos")
                || key.equals("tan") || key.equals("ln") || key.equals("log") || key.equals("√");
    }

    // ---------- Input helpers ----------

    private void deleteLast() {
        String[] tokens = {"sin(", "cos(", "tan(", "log(", "ln(", "√("};
        String s = expr.toString();
        for (String t : tokens) {
            if (s.endsWith(t)) {
                expr.setLength(expr.length() - t.length());
                return;
            }
        }
        if (expr.length() > 0) expr.setLength(expr.length() - 1);
    }

    private void handleOperator(String op) {
        char last = lastChar();
        if (expr.length() == 0 || last == '(' || last == '^') {
            if (op.equals("−")) expr.append(op); // allow a leading / negative sign
            return;
        }
        if (isOperator(last)) {
            int n = expr.length();
            char prev = n >= 2 ? expr.charAt(n - 2) : '\0';
            if (n == 1 || prev == '(' || prev == '^') return; // that minus is a sign, keep it
            expr.setCharAt(n - 1, op.charAt(0)); // replace previous operator
        } else if (last != '.') {
            expr.append(op);
        }
    }

    private boolean canAddDecimal() {
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == '.') return false;
            if (!Character.isDigit(c)) break;
        }
        return true;
    }

    private int countOpen() {
        int open = 0;
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') open++;
            else if (expr.charAt(i) == ')') open--;
        }
        return open;
    }

    private char lastChar() {
        return expr.length() == 0 ? '\0' : expr.charAt(expr.length() - 1);
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷';
    }

    /** True if this character ends a number/value (so %, !, ^, x² can follow). */
    private boolean endsValue(char c) {
        return Character.isDigit(c) || c == ')' || c == 'π' || c == 'e'
                || c == '%' || c == '!' || c == '²';
    }

    /** Removes dangling operators, dots, ^ and ( from the end so we can still preview. */
    private String stripTrailing(String s) {
        while (s.length() > 0) {
            char c = s.charAt(s.length() - 1);
            if (isOperator(c) || c == '.' || c == '^' || c == '(') {
                s = s.substring(0, s.length() - 1);
            } else {
                break;
            }
        }
        return s;
    }

    // ---------- Calculation ----------

    private void showLivePreview() {
        String s = stripTrailing(expr.toString());
        if (s.isEmpty()) {
            tvResult.setText("0");
            return;
        }
        try {
            tvResult.setText(format(new Parser(s, degMode).parse()));
        } catch (Exception e) {
            // incomplete expression - keep previous preview
        }
    }

    private void calculateFinal() {
        String s = stripTrailing(expr.toString());
        if (s.isEmpty()) return;
        try {
            String result = format(new Parser(s, degMode).parse());
            addHistory(s, result);
            tvExpression.setText(s + " =");
            tvResult.setText(result);
            expr.setLength(0);
            if (!result.contains("E")) expr.append(result.replace("-", "−"));
            justCalculated = true;
        } catch (ArithmeticException e) {
            tvResult.setText(e.getMessage() != null ? e.getMessage() : "Math error");
            expr.setLength(0);
        } catch (Exception e) {
            tvResult.setText("Error");
            expr.setLength(0);
        }
    }

    private String format(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new ArithmeticException("Math error");
        }
        if (Math.abs(value) >= 1e15) {
            return new BigDecimal(value).round(new MathContext(10)).stripTrailingZeros().toString();
        }
        BigDecimal bd = new BigDecimal(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
        if (bd.scale() < 0) bd = bd.setScale(0);
        return bd.toPlainString();
    }

    // ---------- History ----------

    private void addHistory(String expression, String result) {
        history.add(0, new String[]{expression, result});
        while (history.size() > 50) history.remove(history.size() - 1);
        saveHistory();
    }

    private void saveHistory() {
        StringBuilder sb = new StringBuilder();
        for (String[] h : history) {
            sb.append(h[0]).append('\t').append(h[1]).append('\n');
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_HISTORY, sb.toString()).apply();
    }

    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String data = prefs.getString(KEY_HISTORY, "");
        history.clear();
        for (String line : data.split("\n")) {
            String[] parts = line.split("\t");
            if (parts.length == 2) history.add(parts);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showHistory() {
        final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
        final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

        BottomSheetDialog dialog = new BottomSheetDialog(this);

        // Dark rounded sheet
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(24));
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(0xFF1C1C1E);
        float r = dp(28);
        sheetBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        root.setBackground(sheetBg);

        // Little drag handle
        View handle = new View(this);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0xFF48484A);
        handleBg.setCornerRadius(dp(2));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(40), dp(4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = dp(16);
        root.addView(handle, handleLp);

        // Header: title + "Clear all"
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("History");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));

        if (!history.isEmpty()) {
            TextView clear = new TextView(this);
            clear.setText("Clear all");
            clear.setTextColor(0xFFFF9500);
            clear.setTextSize(16);
            clear.setPadding(dp(12), dp(8), dp(12), dp(8));
            clear.setOnClickListener(v -> {
                history.clear();
                saveHistory();
                dialog.dismiss();
            });
            header.addView(clear);
        }
        root.addView(header);

        // List of cards
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(16), 0, 0);

        if (history.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No calculations yet");
            empty.setTextColor(0xFF8E8E93);
            empty.setTextSize(16);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            list.addView(empty, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        for (int i = 0; i < history.size(); i++) {
            final String[] entry = history.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.END);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(0xFF2C2C2E);
            cardBg.setCornerRadius(dp(18));
            card.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), cardBg, null));
            card.setClickable(true);

            TextView tvE = new TextView(this);
            tvE.setText(entry[0]);
            tvE.setTextColor(0xFF9E9E9E);
            tvE.setTextSize(15);
            tvE.setGravity(Gravity.END);
            card.addView(tvE, new LinearLayout.LayoutParams(MATCH, WRAP));

            TextView tvR = new TextView(this);
            tvR.setText("= " + entry[1]);
            tvR.setTextColor(Color.WHITE);
            tvR.setTextSize(28);
            tvR.setGravity(Gravity.END);
            card.addView(tvR, new LinearLayout.LayoutParams(MATCH, WRAP));

            // Tap a card to reuse its result
            card.setOnClickListener(v -> {
                String res = entry[1];
                expr.setLength(0);
                if (!res.contains("E")) expr.append(res.replace("-", "−"));
                tvExpression.setText(expr.toString());
                tvResult.setText(res);
                justCalculated = true;
                dialog.dismiss();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.bottomMargin = dp(10);
            list.addView(card, lp);
        }

        NestedScrollView scroll = new NestedScrollView(this);
        scroll.addView(list);
        int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.6f);
        root.addView(scroll, new LinearLayout.LayoutParams(MATCH, history.size() > 4 ? maxHeight : WRAP));

        dialog.setContentView(root);

        // Make the default sheet background transparent so our rounded corners show
        View parent = (View) root.getParent();
        if (parent != null) parent.setBackgroundColor(Color.TRANSPARENT);

        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.show();
    }

    // ---------- Expression parser ----------

    /**
     * Handles + - x / ^ % ! x^2, brackets, pi, e, sin cos tan ln log sqrt,
     * a leading minus, and implicit multiplication like 2pi or 3(4+1).
     */
    private static class Parser {
        private final String s;
        private final boolean deg;
        private int pos = 0;

        Parser(String s, boolean deg) {
            this.s = s;
            this.deg = deg;
        }

        double parse() {
            double x = parseExpression();
            if (pos < s.length()) throw new RuntimeException("Unexpected: " + s.charAt(pos));
            return x;
        }

        private char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        private double parseExpression() {
            double x = parseTerm();
            while (pos < s.length()) {
                char c = peek();
                if (c == '+') { pos++; x += parseTerm(); }
                else if (c == '−') { pos++; x -= parseTerm(); }
                else break;
            }
            return x;
        }

        private double parseTerm() {
            double x = parseUnary();
            while (pos < s.length()) {
                char c = peek();
                if (c == '×') {
                    pos++;
                    x *= parseUnary();
                } else if (c == '÷') {
                    pos++;
                    double d = parseUnary();
                    if (d == 0) throw new ArithmeticException("Can't divide by 0");
                    x /= d;
                } else if (startsPrimary()) {
                    x *= parseUnary(); // implicit multiplication
                } else {
                    break;
                }
            }
            return x;
        }

        private boolean startsPrimary() {
            char c = peek();
            return Character.isDigit(c) || c == '.' || c == '(' || c == 'π' || c == 'e' || c == '√'
                    || s.startsWith("sin", pos) || s.startsWith("cos", pos)
                    || s.startsWith("tan", pos) || s.startsWith("ln", pos)
                    || s.startsWith("log", pos);
        }

        private double parseUnary() {
            if (peek() == '−') { pos++; return -parseUnary(); }
            return parsePower();
        }

        private double parsePower() {
            double base = parsePostfix();
            if (peek() == '^') {
                pos++;
                double exp = parseUnary(); // right-associative
                return Math.pow(base, exp);
            }
            return base;
        }

        private double parsePostfix() {
            double x = parsePrimary();
            while (pos < s.length()) {
                char c = peek();
                if (c == '!') { pos++; x = factorial(x); }
                else if (c == '%') { pos++; x /= 100.0; }
                else if (c == '²') { pos++; x *= x; }
                else break;
            }
            return x;
        }

        private double parsePrimary() {
            char c = peek();

            if (c == '(') {
                pos++;
                double x = parseExpression();
                if (peek() == ')') pos++; // brackets left open are closed automatically
                return x;
            }
            if (c == 'π') { pos++; return Math.PI; }
            if (c == 'e') { pos++; return Math.E; }
            if (c == '√') {
                pos++;
                double a = argument();
                if (a < 0) throw new ArithmeticException("Invalid input");
                return Math.sqrt(a);
            }
            if (s.startsWith("sin", pos)) { pos += 3; return Math.sin(toRad(argument())); }
            if (s.startsWith("cos", pos)) { pos += 3; return Math.cos(toRad(argument())); }
            if (s.startsWith("tan", pos)) {
                pos += 3;
                double r = toRad(argument());
                if (Math.abs(Math.cos(r)) < 1e-10) throw new ArithmeticException("Undefined");
                return Math.tan(r);
            }
            if (s.startsWith("log", pos)) {
                pos += 3;
                double a = argument();
                if (a <= 0) throw new ArithmeticException("Invalid input");
                return Math.log10(a);
            }
            if (s.startsWith("ln", pos)) {
                pos += 2;
                double a = argument();
                if (a <= 0) throw new ArithmeticException("Invalid input");
                return Math.log(a);
            }

            // plain number
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (start == pos) throw new RuntimeException("Number expected");
            return Double.parseDouble(s.substring(start, pos));
        }

        private double argument() {
            if (peek() != '(') throw new RuntimeException("( expected");
            pos++;
            double x = parseExpression();
            if (peek() == ')') pos++;
            return x;
        }

        private double toRad(double a) {
            return deg ? Math.toRadians(a) : a;
        }

        private double factorial(double x) {
            if (x < 0 || x != Math.floor(x) || x > 170) throw new ArithmeticException("Invalid input");
            double r = 1;
            for (int i = 2; i <= (int) x; i++) r *= i;
            return r;
        }
    }
}