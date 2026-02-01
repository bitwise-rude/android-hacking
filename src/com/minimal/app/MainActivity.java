package com.minimal.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private EditText userField, passField;
    private TextView status;
    private Spinner userSpinner;
    private boolean passwordVisible = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<String> userList = new ArrayList<>();

    private static final String PREF_NAME = "creds";
    private static final String KEY_USERS  = "user_list";
    private static final String KEY_LAST   = "last_user";

    // ─── Colors ───
    private static final int C_BG_TOP      = 0xFF0A0E1A;
    private static final int C_BG_BOT      = 0xFF1A2240;
    private static final int C_CARD        = 0xFF141824;
    private static final int C_CARD_BORDER = 0xFF2A3050;
    private static final int C_FIELD_BG    = 0xFF1E2336;
    private static final int C_FIELD_BORDER= 0xFF2E3555;
    private static final int C_TEXT        = 0xFFE8EAF0;
    private static final int C_TEXT_DIM    = 0xFF6B7280;
    private static final int C_ACCENT      = 0xFF4F8CFF;
    private static final int C_ACCENT_DARK = 0xFF3A6FD8;
    private static final int C_SUCCESS     = 0xFF34D399;
    private static final int C_ERROR       = 0xFFF87171;
    private static final int C_DELETE      = 0xFF4A2030;
    private static final int C_DELETE_TEXT = 0xFFF87171;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ─── Root ───
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{C_BG_TOP, C_BG_BOT}));

        // ─── Card ───
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(28), dp(32), dp(28), dp(32));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(C_CARD);
        cardBg.setCornerRadius(dp(20));
        cardBg.setStroke(dp(1), C_CARD_BORDER);
        card.setBackground(cardBg);

        // ─── Icon row (wifi icon text + title) ───
        TextView icon = new TextView(this);
        icon.setText("\uD83D\uDCF5");   // wifi emoji
        icon.setTextSize(32);
        icon.setGravity(Gravity.CENTER);
        icon.setLayoutParams(marginParams(0, 0, 0, dp(4)));
        card.addView(icon);

        TextView title = new TextView(this);
        title.setText("CITPC Auto Login");
        title.setTextColor(C_TEXT);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(marginParams(0, 0, 0, dp(2)));
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Auto WiFi Login");
        subtitle.setTextColor(C_TEXT_DIM);
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLayoutParams(marginParams(0, 0, 0, dp(28)));
        card.addView(subtitle);

        // ─── Accounts section label ───
        card.addView(sectionLabel("Saved Accounts"));

        // ─── Spinner ───
        userSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        userSpinner.setLayoutParams(marginParams(0, dp(6), 0, dp(6)));
        // Spinner styling via a custom background
        GradientDrawable spinnerBg = new GradientDrawable();
        spinnerBg.setColor(C_FIELD_BG);
        spinnerBg.setCornerRadius(dp(12));
        spinnerBg.setStroke(dp(1), C_FIELD_BORDER);
        userSpinner.setBackground(spinnerBg);
        card.addView(userSpinner);

        // ─── Add / Delete row ───
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setLayoutParams(marginParams(0, dp(8), 0, dp(24)));

        Button addBtn = styledButton("+ Save", C_ACCENT, C_ACCENT_DARK, Color.WHITE);
        Button deleteBtn = styledButton("Delete", C_DELETE, C_DELETE, C_DELETE_TEXT);

        btnRow.addView(addBtn, new LinearLayout.LayoutParams(0, dp(38), 1));
        View spacer = new View(this);
        btnRow.addView(spacer, new LinearLayout.LayoutParams(dp(8), 0));
        btnRow.addView(deleteBtn, new LinearLayout.LayoutParams(0, dp(38), 1));
        card.addView(btnRow);

        // ─── Credentials section label ───
        card.addView(sectionLabel("Credentials"));

        // ─── Username ───
        userField = styledField("Username");
        userField.setLayoutParams(marginParams(0, dp(6), 0, dp(12)));
        card.addView(userField);

        // ─── Password + toggle ───
        passField = styledField("Password");
        passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        ImageButton toggle = new ImageButton(this);
        toggle.setImageResource(android.R.drawable.ic_menu_view);
        toggle.setBackgroundColor(Color.TRANSPARENT);
        toggle.setColorFilter(C_TEXT_DIM);
        toggle.setOnClickListener(v -> togglePassword());

        LinearLayout passRow = new LinearLayout(this);
        passRow.setOrientation(LinearLayout.HORIZONTAL);
        passRow.setLayoutParams(marginParams(0, dp(0), 0, dp(28)));

        GradientDrawable passRowBg = new GradientDrawable();
        passRowBg.setColor(C_FIELD_BG);
        passRowBg.setCornerRadius(dp(12));
        passRowBg.setStroke(dp(1), C_FIELD_BORDER);
        passRow.setBackground(passRowBg);

        // Reset passField background since the row itself is the container now
        passField.setBackground(null);
        passField.setPadding(dp(16), dp(14), dp(4), dp(14));

        passRow.addView(passField, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        passRow.addView(toggle, new LinearLayout.LayoutParams(dp(44), LinearLayout.LayoutParams.MATCH_PARENT));
        card.addView(passRow);

        // ─── Status ───
        status = new TextView(this);
        status.setTextColor(C_TEXT_DIM);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setLayoutParams(marginParams(0, dp(4), 0, 0));
        card.addView(status);

        root.addView(card);
        setContentView(root);

        // ─── Data + events ───
        loadUsers();
        refreshSpinner();

        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selected = userList.get(pos);
                userField.setText(selected);
                SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                passField.setText(sp.getString("pass_" + selected, ""));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        addBtn.setOnClickListener(v -> saveCurrentUser());
        deleteBtn.setOnClickListener(v -> deleteSelectedUser());

        // ─── Auto-connect on app open ───
        autoConnect();
    }

    // ──────────────────────────────────────────────
    // UI builders
    // ──────────────────────────────────────────────

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams marginParams(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setTextColor(C_ACCENT);
        tv.setTextSize(10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(2), 0, 0, dp(6));
        return tv;
    }

    private EditText styledField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(C_TEXT_DIM);
        e.setTextColor(C_TEXT);
        e.setSingleLine(true);
        e.setPadding(dp(16), dp(14), dp(16), dp(14));
        e.setTextSize(14);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_FIELD_BG);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), C_FIELD_BORDER);
        e.setBackground(bg);

        return e;
    }

    private Button styledButton(String text, int bgColor, int pressColor, int textColor) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(10));
        b.setBackground(bg);

        return b;
    }

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        passField.setInputType(passwordVisible
                ? InputType.TYPE_CLASS_TEXT
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passField.setSelection(passField.getText().length());
    }

    // ──────────────────────────────────────────────
    // Multi-user management
    // ──────────────────────────────────────────────

    private void loadUsers() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Set<String> saved = sp.getStringSet(KEY_USERS, null);
        userList = saved != null ? new ArrayList<>(saved) : new ArrayList<>();
    }

    private void refreshSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, userList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userSpinner.setAdapter(adapter);

        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String last = sp.getString(KEY_LAST, null);
        if (last != null && userList.contains(last)) {
            userSpinner.setSelection(userList.indexOf(last));
        }
    }

    private void saveCurrentUser() {
        String user = userField.getText().toString().trim();
        String pass = passField.getText().toString();
        if (user.isEmpty()) { setStatus("Username is empty", C_ERROR); return; }

        if (!userList.contains(user)) userList.add(user);

        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putStringSet(KEY_USERS, new HashSet<>(userList))
                .putString("pass_" + user, pass)
                .putString(KEY_LAST, user)
                .apply();

        refreshSpinner();
        userSpinner.setSelection(userList.indexOf(user));
        setStatus("Saved: " + user, C_SUCCESS);
    }

    private void deleteSelectedUser() {
        if (userList.isEmpty()) { setStatus("No account to delete", C_ERROR); return; }

        int pos = userSpinner.getSelectedItemPosition();
        String toDelete = userList.get(pos);
        userList.remove(pos);

        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putStringSet(KEY_USERS, new HashSet<>(userList))
                .remove("pass_" + toDelete)
                .apply();

        refreshSpinner();
        if (userList.isEmpty()) { userField.setText(""); passField.setText(""); }
        setStatus("Deleted: " + toDelete, C_TEXT_DIM);
    }

    // ──────────────────────────────────────────────
    // Login
    // ──────────────────────────────────────────────

    private void autoConnect() {
        String user = userField.getText().toString().trim();
        String pass = passField.getText().toString();
        
        if (user.isEmpty()) {
            setStatus("Add an account to auto-login", C_TEXT_DIM);
            return;
        }

        setStatus("Auto-connecting to CITPC WiFi…", C_ACCENT);

        executor.execute(() -> {
            boolean ok = false;
            for (int i = 0; i < 3; i++) {
                ok = tryLogin(user, pass);
                if (ok) break;
                sleep(1200);
            }
            if (ok) {
                getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                        .putString("pass_" + user, pass)
                        .putString(KEY_LAST, user)
                        .apply();
            }
            boolean result = ok;
            runOnUiThread(() -> {
                if (result) {
                    setStatus("✓ Connected successfully as " + user, C_SUCCESS);
                } else {
                    setStatus("✗ Connection failed — verify credentials", C_ERROR);
                }
            });
        });
    }

    private boolean tryLogin(String u, String p) {
        HttpURLConnection c = null;
        try {
            URL url = new URL("http://10.100.1.1:8090/login.xml");
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setDoOutput(true);
            c.setRequestMethod("POST");

            String data = "mode=191&username=" + u + "&password=" + p + "&a=" + System.currentTimeMillis();
            OutputStream os = c.getOutputStream();
            os.write(data.getBytes("UTF-8"));
            os.flush();
            os.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("signed in")) { br.close(); return true; }
            }
            br.close();
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.disconnect();
        }
        return false;
    }

    private void setStatus(String msg, int color) {
        status.setText(msg);
        status.setTextColor(color);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}