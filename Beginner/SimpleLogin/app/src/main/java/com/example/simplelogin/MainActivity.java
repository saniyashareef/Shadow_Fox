package com.example.simplelogin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvForgot, tvAttempts;

    private static final String PREFS_NAME = "simple_login_prefs";
    private static final String KEY_LAST_USER = "last_user";
    private static final String KEY_ATTEMPTS_LEFT = "attempts_left";
    private static final String KEY_LOCKED = "locked";
    private static final String KEY_LOCK_TS = "lock_timestamp";

    private static final int MAX_ATTEMPTS = 3;

    // SHORT lock time for testing
    private static final int LOCK_TIME_SECONDS = 10;

    private int attemptsLeft;
    private SharedPreferences prefs;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            updateLockCountdown();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);
        tvAttempts = findViewById(R.id.tvAttempts);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load last user
        String lastUser = prefs.getString(KEY_LAST_USER, "");
        if (!lastUser.isEmpty()) etUsername.setText(lastUser);

        // Load attempts
        attemptsLeft = prefs.getInt(KEY_ATTEMPTS_LEFT, MAX_ATTEMPTS);

        boolean locked = prefs.getBoolean(KEY_LOCKED, false);
        if (locked) {
            btnLogin.setEnabled(false);
            handler.post(countdownRunnable);
        } else {
            updateAttemptsText();
            btnLogin.setEnabled(true);
        }

        btnLogin.setOnClickListener(v -> {
            if (isCurrentlyLocked()) {
                Toast.makeText(MainActivity.this, "Login locked. Please wait.", Toast.LENGTH_SHORT).show();
                return;
            }

            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.equals("admin") && password.equals("1234")) {
                Toast.makeText(MainActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                prefs.edit()
                        .putString(KEY_LAST_USER, username)
                        .putInt(KEY_ATTEMPTS_LEFT, MAX_ATTEMPTS)
                        .putBoolean(KEY_LOCKED, false)
                        .putLong(KEY_LOCK_TS, 0)
                        .apply();

                attemptsLeft = MAX_ATTEMPTS;
                updateAttemptsText();

                Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);

            } else {
                attemptsLeft--;

                if (attemptsLeft <= 0) {
                    attemptsLeft = 0;

                    long now = System.currentTimeMillis();
                    prefs.edit()
                            .putInt(KEY_ATTEMPTS_LEFT, 0)
                            .putBoolean(KEY_LOCKED, true)
                            .putLong(KEY_LOCK_TS, now)
                            .apply();

                    btnLogin.setEnabled(false);
                    handler.post(countdownRunnable);

                    Toast.makeText(MainActivity.this, "Too many attempts. Locked!", Toast.LENGTH_SHORT).show();

                } else {
                    prefs.edit().putInt(KEY_ATTEMPTS_LEFT, attemptsLeft).apply();
                    updateAttemptsText();
                    Toast.makeText(MainActivity.this, "Invalid Credentials!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvForgot.setOnClickListener(v -> showForgotPasswordDialog());
    }

    // Check remaining time and update
    private void updateLockCountdown() {
        long lockTs = prefs.getLong(KEY_LOCK_TS, 0);
        long elapsed = (System.currentTimeMillis() - lockTs) / 1000;
        long remaining = LOCK_TIME_SECONDS - elapsed;

        if (remaining > 0) {
            tvAttempts.setText("Locked. Try again after " + remaining + "s");
            handler.postDelayed(countdownRunnable, 1000);
        } else {
            prefs.edit()
                    .putBoolean(KEY_LOCKED, false)
                    .putInt(KEY_ATTEMPTS_LEFT, MAX_ATTEMPTS)
                    .putLong(KEY_LOCK_TS, 0)
                    .apply();

            attemptsLeft = MAX_ATTEMPTS;
            btnLogin.setEnabled(true);
            updateAttemptsText();
        }
    }

    private boolean isCurrentlyLocked() {
        if (!prefs.getBoolean(KEY_LOCKED, false)) return false;

        long lockTs = prefs.getLong(KEY_LOCK_TS, 0);
        long elapsed = (System.currentTimeMillis() - lockTs) / 1000;

        return elapsed < LOCK_TIME_SECONDS;
    }

    private void updateAttemptsText() {
        tvAttempts.setText("Attempts left: " + attemptsLeft);
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your username or email");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter username or email", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(MainActivity.this, "If account exists, reset link would be sent.", Toast.LENGTH_LONG).show();

            attemptsLeft = MAX_ATTEMPTS;

            prefs.edit()
                    .putInt(KEY_ATTEMPTS_LEFT, attemptsLeft)
                    .putBoolean(KEY_LOCKED, false)
                    .putLong(KEY_LOCK_TS, 0)
                    .apply();

            btnLogin.setEnabled(true);
            updateAttemptsText();
            handler.removeCallbacks(countdownRunnable);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(countdownRunnable);
    }
}
