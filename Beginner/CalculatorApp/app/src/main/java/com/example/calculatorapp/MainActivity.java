package com.example.calculatorapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView tvDisplay;

    private double firstOperand = Double.NaN;
    private String currentOp = "";
    private boolean enteringNewNumber = true;

    // Memory
    private double memory = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = findViewById(R.id.tvDisplay);

        // Ensure display shows "0" (from strings)
        tvDisplay.setText(getString(R.string.d_0));
    }

    // ==========================================
    // Digit
    // ==========================================
    public void onDigit(View v) {
        String d = ((Button) v).getText().toString();
        String cur = tvDisplay.getText().toString();

        String zero = getString(R.string.d_0);
        String zero00 = getString(R.string.d_00);

        if (enteringNewNumber || cur.equals(zero) || cur.equals(getString(R.string.error))) {
            // if user tapped 00 when starting, show 0
            tvDisplay.setText(d.equals(zero00) ? zero : d);
            enteringNewNumber = false;
        } else {
            // use a format string resource to concatenate
            tvDisplay.setText(getString(R.string.concat_two, cur, d));
        }
    }

    public void onDot(View v) {
        String cur = tvDisplay.getText().toString();
        String zeroDot = getString(R.string.zero_dot);

        if (enteringNewNumber || cur.equals(getString(R.string.error))) {
            tvDisplay.setText(zeroDot);
            enteringNewNumber = false;
            return;
        }
        if (!cur.contains(".")) {
            tvDisplay.setText(getString(R.string.concat_two, cur, "."));
        }
    }

    // ==========================================
    // Operators
    // ==========================================
    public void onOperator(View v) {
        String op = ((Button) v).getText().toString();

        // map x^y button text to op "pow"
        if (op.equals(getString(R.string.xpow))) {
            currentOp = "pow";
        } else {
            currentOp = op;
        }

        try {
            double displayed = Double.parseDouble(tvDisplay.getText().toString());

            if (!Double.isNaN(firstOperand) && !enteringNewNumber) {
                firstOperand = compute(firstOperand, displayed, currentOp);
                tvDisplay.setText(strip(firstOperand));
            } else {
                firstOperand = displayed;
            }

        } catch (Exception e) {
            firstOperand = 0;
        }

        enteringNewNumber = true;
    }

    public void onEqual(View v) {
        if (currentOp.isEmpty() || Double.isNaN(firstOperand)) return;

        try {
            double second = Double.parseDouble(tvDisplay.getText().toString());
            double result = compute(firstOperand, second, currentOp);

            // Display raw numeric result (easy for further calculations)
            tvDisplay.setText(strip(result));

            firstOperand = Double.NaN;
            currentOp = "";
            enteringNewNumber = true;

        } catch (Exception ignored) {
            tvDisplay.setText(getString(R.string.error));
            enteringNewNumber = true;
        }
    }

    private double compute(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return b == 0 ? Double.NaN : a / b;
            case "pow": return Math.pow(a, b);
            default: return b;
        }
    }

    // ==========================================
    // Scientific Functions
    // ==========================================
    public void onFunction(View v) {
        String fn = ((Button) v).getText().toString();
        double val;

        try {
            val = Double.parseDouble(tvDisplay.getText().toString());
        } catch (Exception e) {
            // if parsing fails, show error
            tvDisplay.setText(getString(R.string.error));
            enteringNewNumber = true;
            return;
        }

        double result = val;

        if (fn.equals(getString(R.string.sin))) {
            result = Math.sin(Math.toRadians(val));
        } else if (fn.equals(getString(R.string.cos))) {
            result = Math.cos(Math.toRadians(val));
        } else if (fn.equals(getString(R.string.tan))) {
            result = Math.tan(Math.toRadians(val));
        } else if (fn.equals(getString(R.string.sqrt))) {
            if (val < 0) {
                tvDisplay.setText(getString(R.string.error));
                enteringNewNumber = true;
                return;
            }
            result = Math.sqrt(val);
        } else if (fn.equals(getString(R.string.log))) {
            if (val <= 0) {
                tvDisplay.setText(getString(R.string.error));
                enteringNewNumber = true;
                return;
            }
            result = Math.log10(val);
        } else if (fn.equals(getString(R.string.ln))) {
            if (val <= 0) {
                tvDisplay.setText(getString(R.string.error));
                enteringNewNumber = true;
                return;
            }
            result = Math.log(val);
        } else if (fn.equals(getString(R.string.x2))) {
            result = val * val;
        }

        tvDisplay.setText(strip(result));
        enteringNewNumber = true;
    }

    // ==========================================
    // Memory Functions
    // ==========================================
    public void onMemory(View v) {
        String cmd = ((Button) v).getText().toString();

        double cur;
        try {
            cur = Double.parseDouble(tvDisplay.getText().toString());
        } catch (Exception e) {
            // nothing to add/subtract if display isn't a number
            return;
        }

        if (cmd.equals(getString(R.string.mc))) {
            memory = 0;
        } else if (cmd.equals(getString(R.string.mr))) {
            tvDisplay.setText(strip(memory));
            enteringNewNumber = true;
        } else if (cmd.equals(getString(R.string.m_plus))) {
            memory += cur;
        } else if (cmd.equals(getString(R.string.m_minus))) {
            memory -= cur;
        }
    }

    // ==========================================
    // Clear / Backspace / Percent
    // ==========================================
    public void onClear(View v) {
        tvDisplay.setText(getString(R.string.d_0));
        firstOperand = Double.NaN;
        currentOp = "";
        enteringNewNumber = true;
    }

    public void onBackspace(View v) {
        String cur = tvDisplay.getText().toString();
        if (cur.length() <= 1 || cur.equals(getString(R.string.error))) {
            tvDisplay.setText(getString(R.string.d_0));
            enteringNewNumber = true;
        } else {
            tvDisplay.setText(cur.substring(0, cur.length() - 1));
        }
    }

    public void onPercent(View v) {
        try {
            double val = Double.parseDouble(tvDisplay.getText().toString());
            tvDisplay.setText(strip(val / 100));
            enteringNewNumber = true;
        } catch (Exception e) {
            tvDisplay.setText(getString(R.string.error));
            enteringNewNumber = true;
        }
    }

    // ==========================================
    // Helper
    // ==========================================
    private String strip(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return getString(R.string.error);
        if (val == (long) val) return String.format("%d", (long) val);
        return String.valueOf(val);
    }
}
