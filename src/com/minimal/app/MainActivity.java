package com.minimal.app;

import android.app.Activity;
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
    private Button addBtn, deleteBtn;
    private boolean passwordVisible = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<String> userList = new ArrayList<>();

    private static final String PREF_NAME = "creds";
    private static final String KEY_USERS = "user_list";
    private static final String KEY_LAST = "last_user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ─── Root layout ───
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0F2027, 0xFF203A43, 0xFF2C5364});
        root.setBackground(bg);

        // ─── Card ───
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(50, 50, 50, 50);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF1E1E1E);
        cardBg.setCornerRadius(36);
        card.setBackground(cardBg);

        // ─── Title ───
        TextView title = new TextView(this);
        title.setText("CITPC CONNECTOR");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        card.addView(title);

        // ─── Saved user spinner ───
        TextView savedLabel = new TextView(this);
        savedLabel.setText("Saved Accounts");
        savedLabel.setTextColor(0xFF999999);
        savedLabel.setTextSize(12);
        savedLabel.setPadding(4, 0, 0, 4);
        card.addView(savedLabel);

        userSpinner = new Spinner(this);
        userSpinner.setLayoutParams(makeMarginParams(0, 0, 0, 8));
        card.addView(userSpinner);

        // ─── Spinner button row ───
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setLayoutParams(makeMarginParams(0, 0, 0, 16));

        addBtn = makeSmallButton("+ Add New");
        deleteBtn = makeSmallButton("Delete");
        deleteBtn.setBackgroundColor(0xFF993333);

        btnRow.addView(addBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        btnRow.addView(deleteBtn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(btnRow);

        // ─── Username field ───
        userField = createField("Username");
        card.addView(userField);

        // ─── Password field + toggle ───
        passField = createField("Password");
        passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        ImageButton toggle = new ImageButton(this);
        toggle.setImageResource(android.R.drawable.ic_menu_view);
        toggle.setBackgroundColor(Color.TRANSPARENT);
        toggle.setOnClickListener(v -> togglePassword());

        LinearLayout passRow = new LinearLayout(this);
        passRow.setOrientation(LinearLayout.HORIZONTAL);
        passRow.setLayoutParams(makeMarginParams(0, 0, 0, 22));
        passRow.addView(passField, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        passRow.addView(toggle);
        card.addView(passRow);

        // ─── Connect button ───
        Button connect = new Button(this);
        connect.setText("CONNECT");
        connect.setOnClickListener(v -> connect());
        card.addView(connect);

        // ─── Status ───
        status = new TextView(this);
        status.setTextColor(0xFFBBBBBB);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 20, 0, 0);
        card.addView(status);

        root.addView(card);
        setContentView(root);

        // ─── Wire up events ───
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

        // Auto-connect if launched from WifiService (has extra flag)
        if (getIntent() != null && getIntent().getBooleanExtra("auto_login", false)) {
            connect();
        }
    }

    // ──────────────────────────────────────────────
    // UI helpers
    // ──────────────────────────────────────────────

    private EditText createField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF777777);
        e.setTextColor(Color.WHITE);
        e.setSingleLine(true);
        e.setPadding(30, 24, 30, 24);

        GradientDrawable fieldBg = new GradientDrawable();
        fieldBg.setColor(0xFF2A2A2A);
        fieldBg.setCornerRadius(28);
        e.setBackground(fieldBg);

        return e;
    }

    private Button makeSmallButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11);
        b.setBackgroundColor(0xFF336633);
        b.setTextColor(Color.WHITE);
        return b;
    }

    private LinearLayout.LayoutParams makeMarginParams(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(l, t, r, b);
        return lp;
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
        if (saved != null) {
            userList = new ArrayList<>(saved);
        } else {
            userList = new ArrayList<>();
        }
    }

    private void refreshSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, userList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userSpinner.setAdapter(adapter);

        // Select last-used user
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String last = sp.getString(KEY_LAST, null);
        if (last != null && userList.contains(last)) {
            userSpinner.setSelection(userList.indexOf(last));
        }
    }

    private void saveCurrentUser() {
        String user = userField.getText().toString().trim();
        String pass = passField.getText().toString();

        if (user.isEmpty()) {
            status.setText("⚠️ Username is empty");
            return;
        }

        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Add to user set
        if (!userList.contains(user)) {
            userList.add(user);
        }

        sp.edit()
                .putStringSet(KEY_USERS, new HashSet<>(userList))
                .putString("pass_" + user, pass)
                .putString(KEY_LAST, user)
                .apply();

        refreshSpinner();
        userSpinner.setSelection(userList.indexOf(user));
        status.setText("✅ Saved: " + user);
    }

    private void deleteSelectedUser() {
        if (userList.isEmpty()) {
            status.setText("⚠️ No account to delete");
            return;
        }

        int pos = userSpinner.getSelectedItemPosition();
        String toDelete = userList.get(pos);

        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userList.remove(pos);

        sp.edit()
                .putStringSet(KEY_USERS, new HashSet<>(userList))
                .remove("pass_" + toDelete)
                .apply();

        refreshSpinner();

        if (userList.isEmpty()) {
            userField.setText("");
            passField.setText("");
        }
        status.setText("🗑️ Deleted: " + toDelete);
    }

    // ──────────────────────────────────────────────
    // Login logic
    // ──────────────────────────────────────────────

    private void connect() {
        String user = userField.getText().toString().trim();
        String pass = passField.getText().toString();

        if (user.isEmpty()) {
            status.setText("⚠️ Enter a username");
            return;
        }

        status.setText("Connecting…");

        executor.execute(() -> {
            boolean ok = false;
            for (int i = 0; i < 3; i++) {
                ok = tryLogin(user, pass);
                if (ok) break;
                sleep(1200);
            }

            // Only save credential on success
            if (ok) {
                saveCredential(user, pass);
            }

            boolean result = ok;
            runOnUiThread(() -> status.setText(result ? "✅ Connected" : "❌ Failed"));
        });
    }

    private boolean tryLogin(String u, String p) {
        HttpURLConnection c = null;
        try {
            URL url = new URL("http://10.100.1.1:8090/login.xml");
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(5000);   // 5s connect timeout
            c.setReadTimeout(5000);      // 5s read timeout
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
                if (line.contains("signed in")) {
                    br.close();
                    return true;
                }
            }
            br.close();

        } catch (Exception ignored) {
            // Network unreachable or timeout — will retry
        } finally {
            if (c != null) {
                c.disconnect();   // Always disconnect
            }
        }
        return false;
    }

    private void saveCredential(String user, String pass) {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        sp.edit()
                .putString("pass_" + user, pass)
                .putString(KEY_LAST, user)
                .apply();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}