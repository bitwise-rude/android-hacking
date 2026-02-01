package com.minimal.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
    private TextView status, icon, connectionStatus;
    private Spinner userSpinner;
    private Button connectBtn;
    private LinearLayout savedAccountsCard, credentialsCard;
    private boolean passwordVisible = false;
    private boolean isConnecting = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<String> userList = new ArrayList<>();

    private static final String PREF_NAME = "creds";
    private static final String KEY_USERS  = "user_list";
    private static final String KEY_LAST   = "last_user";

    // ─── Modern Vibrant Colors ───
    private static final int C_BG_TOP      = 0xFF6366F1;  // Vibrant indigo
    private static final int C_BG_BOT      = 0xFF8B5CF6;  // Purple
    private static final int C_CARD        = 0xFFFFFFFF;  // White cards
    private static final int C_CARD_SHADOW = 0x40000000;  // Shadow
    private static final int C_FIELD_BG    = 0xFFF8F9FF;  // Light background
    private static final int C_FIELD_BORDER= 0xFFE0E7FF;  // Light border
    private static final int C_TEXT        = 0xFF1E293B;  // Dark text
    private static final int C_TEXT_DIM    = 0xFF64748B;  // Gray text
    private static final int C_ACCENT      = 0xFF6366F1;  // Indigo
    private static final int C_ACCENT_DARK = 0xFF4F46E5;  // Dark indigo
    private static final int C_SUCCESS     = 0xFF10B981;  // Green
    private static final int C_ERROR       = 0xFFEF4444;  // Red
    private static final int C_DELETE      = 0xFFFEE2E2;  // Light red bg
    private static final int C_DELETE_TEXT = 0xFFDC2626;  // Red text
    private static final int C_SPINNER_BG  = 0xFF6366F1;  // Vibrant spinner
    private static final int C_SPINNER_TEXT= 0xFFFFFFFF;  // White text

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ─── Scrollable Root ───
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(40));
        
        GradientDrawable rootBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{C_BG_TOP, C_BG_BOT});
        root.setBackground(rootBg);

        // ─── Header Section ───
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(20), 0, dp(30));

        // Animated WiFi Icon
        icon = new TextView(this);
        icon.setText("📶");
        icon.setTextSize(56);
        icon.setGravity(Gravity.CENTER);
        header.addView(icon);

        TextView title = new TextView(this);
        title.setText("CITPC WiFi");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(marginParams(0, dp(12), 0, dp(4)));
        header.addView(title);

        connectionStatus = new TextView(this);
        connectionStatus.setText("Ready to connect");
        connectionStatus.setTextColor(0xCCFFFFFF);
        connectionStatus.setTextSize(14);
        connectionStatus.setGravity(Gravity.CENTER);
        header.addView(connectionStatus);

        root.addView(header);

        // ─── Saved Accounts Card ───
        savedAccountsCard = createModernCard();
        savedAccountsCard.setLayoutParams(marginParams(0, 0, 0, dp(16)));

        TextView accountsLabel = new TextView(this);
        accountsLabel.setText("SAVED ACCOUNTS");
        accountsLabel.setTextColor(C_ACCENT);
        accountsLabel.setTextSize(11);
        accountsLabel.setTypeface(Typeface.DEFAULT_BOLD);
        accountsLabel.setLetterSpacing(0.1f);
        accountsLabel.setLayoutParams(marginParams(0, 0, 0, dp(12)));
        savedAccountsCard.addView(accountsLabel);

        // Modern Spinner Container
        LinearLayout spinnerContainer = new LinearLayout(this);
        spinnerContainer.setOrientation(LinearLayout.HORIZONTAL);
        spinnerContainer.setGravity(Gravity.CENTER_VERTICAL);
        spinnerContainer.setLayoutParams(marginParams(0, 0, 0, dp(12)));
        
        GradientDrawable spinnerBg = new GradientDrawable();
        spinnerBg.setCornerRadius(dp(12));
        spinnerBg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        spinnerBg.setColors(new int[]{0xFF6366F1, 0xFF8B5CF6});
        spinnerBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        spinnerContainer.setBackground(spinnerBg);
        spinnerContainer.setPadding(dp(16), dp(4), dp(16), dp(4));
        spinnerContainer.setElevation(dp(3));

        TextView spinnerIcon = new TextView(this);
        spinnerIcon.setText("👤");
        spinnerIcon.setTextSize(20);
        spinnerIcon.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        spinnerContainer.addView(spinnerIcon);

        userSpinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        userSpinner.setBackgroundColor(Color.TRANSPARENT);
        userSpinner.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        userSpinner.setLayoutParams(spinnerParams);
        spinnerContainer.addView(userSpinner);

        savedAccountsCard.addView(spinnerContainer);

        // Action Buttons Row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setLayoutParams(marginParams(0, 0, 0, 0));

        Button addBtn = createModernButton("💾 Save Account", C_SUCCESS, Color.WHITE);
        Button deleteBtn = createModernButton("🗑️ Delete", C_ERROR, Color.WHITE);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        btnRow.addView(addBtn, btnParams);
        
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(dp(12), 0);
        btnRow.addView(spacer, spacerParams);
        
        btnRow.addView(deleteBtn, btnParams);
        savedAccountsCard.addView(btnRow);

        root.addView(savedAccountsCard);

        // ─── Credentials Card ───
        credentialsCard = createModernCard();
        credentialsCard.setLayoutParams(marginParams(0, 0, 0, dp(20)));

        TextView credsLabel = new TextView(this);
        credsLabel.setText("LOGIN CREDENTIALS");
        credsLabel.setTextColor(C_ACCENT);
        credsLabel.setTextSize(11);
        credsLabel.setTypeface(Typeface.DEFAULT_BOLD);
        credsLabel.setLetterSpacing(0.1f);
        credsLabel.setLayoutParams(marginParams(0, 0, 0, dp(12)));
        credentialsCard.addView(credsLabel);

        // Username Field
        userField = createModernField("👤 Username", false);
        userField.setLayoutParams(marginParams(0, 0, 0, dp(12)));
        credentialsCard.addView(userField);

        // Password Field with Toggle
        LinearLayout passContainer = createPasswordField();
        passContainer.setLayoutParams(marginParams(0, 0, 0, 0));
        credentialsCard.addView(passContainer);

        root.addView(credentialsCard);

        // ─── Big Connect Button ───
        connectBtn = createBigConnectButton();
        connectBtn.setLayoutParams(marginParams(0, 0, 0, dp(16)));
        root.addView(connectBtn);

        // ─── Status Card ───
        LinearLayout statusCard = createModernCard();
        statusCard.setLayoutParams(marginParams(0, 0, 0, dp(30)));
        statusCard.setPadding(dp(20), dp(16), dp(20), dp(16));

        status = new TextView(this);
        status.setText("Tap connect to login to WiFi");
        status.setTextColor(C_TEXT_DIM);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        statusCard.addView(status);

        root.addView(statusCard);

        // ─── Footer ───
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(10), 0, 0);

        TextView footerText = new TextView(this);
        footerText.setText("Made by Meyan with adhikari-droid");
        footerText.setTextColor(0xCCFFFFFF);
        footerText.setTextSize(12);
        footerText.setGravity(Gravity.CENTER);
        footerText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        footer.addView(footerText);

        TextView versionText = new TextView(this);
        versionText.setText("v2.0 • Auto Login Enabled");
        versionText.setTextColor(0x88FFFFFF);
        versionText.setTextSize(10);
        versionText.setGravity(Gravity.CENTER);
        versionText.setLayoutParams(marginParams(0, dp(4), 0, 0));
        footer.addView(versionText);

        root.addView(footer);

        scrollView.addView(root);
        setContentView(scrollView);

        // ─── Entrance Animations ───
        animateEntrance(header, 0);
        animateEntrance(savedAccountsCard, 100);
        animateEntrance(credentialsCard, 200);
        animateEntrance(connectBtn, 300);
        animateEntrance(statusCard, 400);
        animateEntrance(footer, 500);

        // ─── Data + Events ───
        loadUsers();
        refreshSpinner();

        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (userList.isEmpty()) return;
                String selected = userList.get(pos);
                userField.setText(selected);
                SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                passField.setText(sp.getString("pass_" + selected, ""));
                pulseView(userField);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        addBtn.setOnClickListener(v -> {
            animateButtonPress(addBtn);
            saveCurrentUser();
        });
        
        deleteBtn.setOnClickListener(v -> {
            animateButtonPress(deleteBtn);
            deleteSelectedUser();
        });

        connectBtn.setOnClickListener(v -> {
            animateButtonPress(connectBtn);
            manualConnect();
        });

        // ─── AUTO-CONNECT ON STARTUP ───
        connectBtn.postDelayed(() -> {
            if (!userField.getText().toString().trim().isEmpty()) {
                connectBtn.performClick();
            }
        }, 800);
    }

    // ──────────────────────────────────────────────
    // Modern UI Components
    // ──────────────────────────────────────────────

    private LinearLayout createModernCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(24), dp(24), dp(24));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_CARD);
        bg.setCornerRadius(dp(20));
        card.setBackground(bg);
        card.setElevation(dp(8));
        
        return card;
    }

    private EditText createModernField(String hint, boolean isPassword) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setHintTextColor(C_TEXT_DIM);
        field.setTextColor(C_TEXT);
        field.setSingleLine(true);
        field.setPadding(dp(18), dp(16), dp(18), dp(16));
        field.setTextSize(15);
        
        if (isPassword) {
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_FIELD_BG);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(2), C_FIELD_BORDER);
        field.setBackground(bg);

        return field;
    }

    private LinearLayout createPasswordField() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_FIELD_BG);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(2), C_FIELD_BORDER);
        container.setBackground(bg);

        passField = new EditText(this);
        passField.setHint("🔒 Password");
        passField.setHintTextColor(C_TEXT_DIM);
        passField.setTextColor(C_TEXT);
        passField.setSingleLine(true);
        passField.setPadding(dp(18), dp(16), dp(4), dp(16));
        passField.setTextSize(15);
        passField.setBackground(null);
        passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        ImageButton toggle = new ImageButton(this);
        toggle.setImageResource(android.R.drawable.ic_menu_view);
        toggle.setBackgroundColor(Color.TRANSPARENT);
        toggle.setColorFilter(C_ACCENT);
        toggle.setPadding(dp(12), dp(12), dp(12), dp(12));
        toggle.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            passField.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passField.setSelection(passField.getText().length());
            pulseView(toggle);
        });

        container.addView(passField, new LinearLayout.LayoutParams(0, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        container.addView(toggle, new LinearLayout.LayoutParams(
                dp(48), LinearLayout.LayoutParams.MATCH_PARENT));

        return container;
    }

    private Button createModernButton(String text, int color, int textColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTextColor(textColor);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setAllCaps(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        btn.setElevation(dp(4));

        return btn;
    }

    private Button createBigConnectButton() {
        Button btn = new Button(this);
        btn.setText("⚡ Connect to WiFi");
        btn.setTextSize(18);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setAllCaps(false);
        btn.setPadding(dp(32), dp(20), dp(32), dp(20));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        bg.setColors(new int[]{0xFF10B981, 0xFF059669});
        bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        btn.setBackground(bg);
        btn.setElevation(dp(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btn.setLayoutParams(params);

        return btn;
    }

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

    // ──────────────────────────────────────────────
    // Premium Animations
    // ──────────────────────────────────────────────

    private void animateEntrance(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(dp(40));
        view.animate()
                .alpha(1f)
                .translationY(0)
                .setStartDelay(delay)
                .setDuration(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void animateButtonPress(View button) {
        button.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(80)
                .withEndAction(() -> button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(new OvershootInterpolator())
                        .setDuration(300)
                        .start())
                .start();
    }

    private void pulseView(View view) {
        view.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(150)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start())
                .start();
    }

    private void animateWifiIcon(boolean connecting) {
        if (connecting) {
            ObjectAnimator rotation = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f);
            rotation.setDuration(2000);
            rotation.setRepeatCount(ValueAnimator.INFINITE);
            rotation.start();

            ObjectAnimator pulse = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.15f, 1f);
            ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.15f, 1f);
            pulse.setDuration(1000);
            pulseY.setDuration(1000);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulseY.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
            pulseY.start();
        } else {
            icon.animate().rotation(0).scaleX(1f).scaleY(1f).setDuration(400).start();
        }
    }

    private void animateSuccess() {
        icon.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .setDuration(200)
                .withEndAction(() -> icon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .setDuration(500)
                        .start())
                .start();

        status.setAlpha(0f);
        status.setScaleX(0.8f);
        status.setScaleY(0.8f);
        status.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .start();

        // Flash green on cards
        flashCard(savedAccountsCard, C_SUCCESS);
        flashCard(credentialsCard, C_SUCCESS);
    }

    private void animateError() {
        ObjectAnimator shake = ObjectAnimator.ofFloat(icon, "translationX", 0, -15, 15, -15, 15, 0);
        shake.setDuration(500);
        shake.start();

        status.setAlpha(0f);
        status.animate().alpha(1f).setDuration(400).start();

        // Flash red on cards
        flashCard(savedAccountsCard, C_ERROR);
        flashCard(credentialsCard, C_ERROR);
    }

    private void flashCard(View card, int color) {
        int originalColor = C_CARD;
        ValueAnimator colorAnim = ValueAnimator.ofArgb(originalColor, 
                Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)), 
                originalColor);
        colorAnim.setDuration(600);
        colorAnim.addUpdateListener(animator -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor((int) animator.getAnimatedValue());
            bg.setCornerRadius(dp(20));
            card.setBackground(bg);
            card.setElevation(dp(8));
        });
        colorAnim.start();
    }

    private void setConnectingState(boolean connecting) {
        isConnecting = connecting;
        connectBtn.setEnabled(!connecting);
        
        if (connecting) {
            connectBtn.setText("⏳ Connecting...");
            connectionStatus.setText("Authenticating...");
            connectionStatus.setTextColor(0xFFFFFFFF);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            bg.setColors(new int[]{0xFF6366F1, 0xFF8B5CF6});
            bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            connectBtn.setBackground(bg);
        } else {
            connectBtn.setText("⚡ Connect to WiFi");
            
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            bg.setColors(new int[]{0xFF10B981, 0xFF059669});
            bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            connectBtn.setBackground(bg);
        }
        
        animateWifiIcon(connecting);
    }

    // ──────────────────────────────────────────────
    // User Management
    // ──────────────────────────────────────────────

    private void loadUsers() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Set<String> saved = sp.getStringSet(KEY_USERS, null);
        userList = saved != null ? new ArrayList<>(saved) : new ArrayList<>();
    }

    private void refreshSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, userList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                view.setTextSize(16);
                view.setTypeface(Typeface.DEFAULT_BOLD);
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(C_TEXT);
                view.setTextSize(15);
                view.setPadding(dp(16), dp(12), dp(16), dp(12));
                return view;
            }
        };
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
        if (user.isEmpty()) { 
            setStatus("⚠️ Username cannot be empty", C_ERROR);
            animateError();
            return; 
        }

        if (!userList.contains(user)) userList.add(user);

        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putStringSet(KEY_USERS, new HashSet<>(userList))
                .putString("pass_" + user, pass)
                .putString(KEY_LAST, user)
                .apply();

        refreshSpinner();
        userSpinner.setSelection(userList.indexOf(user));
        setStatus("✅ Account saved: " + user, C_SUCCESS);
        animateSuccess();
    }

    private void deleteSelectedUser() {
        if (userList.isEmpty()) { 
            setStatus("⚠️ No account to delete", C_ERROR);
            animateError();
            return; 
        }

        int pos = userSpinner.getSelectedItemPosition();
        String toDelete = userList.get(pos);
        userList.remove(pos);

        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                .putStringSet(KEY_USERS, new HashSet<>(userList))
                .remove("pass_" + toDelete)
                .apply();

        refreshSpinner();
        if (userList.isEmpty()) { 
            userField.setText(""); 
            passField.setText(""); 
        }
        setStatus("🗑️ Deleted: " + toDelete, C_TEXT_DIM);
        pulseView(savedAccountsCard);
    }

    // ──────────────────────────────────────────────
    // Connection Logic
    // ──────────────────────────────────────────────

    private void manualConnect() {
        String user = userField.getText().toString().trim();
        String pass = passField.getText().toString();
        
        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("⚠️ Please enter username and password", C_ERROR);
            animateError();
            return;
        }

        performConnection(user, pass);
    }

    private void performConnection(String user, String pass) {
        setConnectingState(true);
        setStatus("🔄 Connecting to CITPC WiFi...", C_ACCENT);

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
                setConnectingState(false);
                if (result) {
                    setStatus("✅ Connected successfully as " + user, C_SUCCESS);
                    connectionStatus.setText("Connected • " + user);
                    connectionStatus.setTextColor(0xFF10B981);
                    animateSuccess();
                } else {
                    setStatus("❌ Connection failed — check credentials", C_ERROR);
                    connectionStatus.setText("Connection failed");
                    connectionStatus.setTextColor(C_ERROR);
                    animateError();
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