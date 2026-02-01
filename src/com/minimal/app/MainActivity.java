package com.minimal.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ===== Root =====
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(Color.parseColor("#121212"));

        // ===== Card =====
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(48, 48, 48, 48);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1E1E1E"));
        cardBg.setCornerRadius(32);
        card.setBackground(cardBg);
        card.setElevation(12);

        // ===== Title =====
        TextView title = new TextView(this);
        title.setText("Login");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 32);
        title.setGravity(Gravity.CENTER);

        // ===== Username =====
        EditText username = inputField("Username");

        // ===== Password =====
        EditText password = inputField("Password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        // ===== Login Button =====
        Button login = new Button(this);
        login.setText("SIGN IN");
        login.setAllCaps(false);
        login.setTypeface(Typeface.DEFAULT_BOLD);
        login.setTextColor(Color.BLACK);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#00E676"));
        btnBg.setCornerRadius(50);
        login.setBackground(btnBg);
        login.setElevation(8);

        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 32, 0, 0);
        login.setLayoutParams(btnParams);

        // ===== Assemble =====
        card.addView(title);
        card.addView(username);
        card.addView(password);
        card.addView(login);

        root.addView(card,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    // ===== Input Field Factory =====
    EditText inputField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.parseColor("#888888"));
        e.setTextColor(Color.WHITE);
        e.setTypeface(Typeface.DEFAULT);
        e.setPadding(32, 24, 32, 24);
        e.setSingleLine(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2A2A2A"));
        bg.setCornerRadius(24);
        bg.setStroke(2, Color.parseColor("#333333"));
        e.setBackground(bg);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 24);
        e.setLayoutParams(p);

        return e;
    }
}

