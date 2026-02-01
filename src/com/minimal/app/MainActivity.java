package com.minimal.app;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    private static final String TARGET_SSID = "CITPC-WIFI";
    private static final int MAX_RETRY = 3;

    private EditText userField, passField;
    private TextView status;
    private boolean passwordVisible;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // Request location permission for SSID check (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40,40,40,40);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0F2027,0xFF203A43,0xFF2C5364});
        root.setBackground(bg);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(50,50,50,50);

        GradientDrawable cb = new GradientDrawable();
        cb.setColor(0xFF1E1E1E);
        cb.setCornerRadius(36);
        card.setBackground(cb);

        TextView title = new TextView(this);
        title.setText("CITPC CONNECTOR");
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,30);

        userField = field("Username");
        passField = field("Password");
        passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        loadDefaults();

        ImageButton toggle = new ImageButton(this);
        toggle.setImageResource(android.R.drawable.ic_menu_view);
        toggle.setBackgroundColor(Color.TRANSPARENT);
        toggle.setOnClickListener(v -> togglePass());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(passField, new LinearLayout.LayoutParams(0,-2,1));
        row.addView(toggle);

        Button connect = new Button(this);
        connect.setText("CONNECT");
        connect.setOnClickListener(v -> connect());

        status = new TextView(this);
        status.setTextColor(0xFFBBBBBB);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0,20,0,0);

        card.addView(title);
        card.addView(userField);
        card.addView(row);
        card.addView(connect);
        card.addView(status);

        root.addView(card);
        setContentView(root);

        autoConnect();
    }

    private EditText field(String h) {
        EditText e = new EditText(this);
        e.setHint(h);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(0xFF777777);
        e.setSingleLine(true);
        e.setPadding(30,24,30,24);

        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF2A2A2A);
        g.setCornerRadius(28);
        e.setBackground(g);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,22);
        e.setLayoutParams(lp);
        return e;
    }

    private void togglePass() {
        passwordVisible = !passwordVisible;
        passField.setInputType(passwordVisible
                ? InputType.TYPE_CLASS_TEXT
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passField.setSelection(passField.getText().length());
    }

    private void loadDefaults() {
        SharedPreferences sp = getSharedPreferences("creds", MODE_PRIVATE);

        if (!sp.contains("080bel042")) {
            sp.edit()
              .putString("080bel042","mechanical")
              .putString("rita","rita")
              .putString("last","080bel042")
              .apply();
        }

        String last = sp.getString("last","080bel042");
        userField.setText(last);
        passField.setText(sp.getString(last,""));
    }

    private boolean onTargetWifi() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network net = cm.getActiveNetwork();
        if (net == null) return false;

        NetworkCapabilities cap = cm.getNetworkCapabilities(net);
        if (cap == null || !cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            return false;

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
        String ssid = info != null ? info.getSSID() : null;

        if (ssid == null) return false;
        ssid = ssid.replace("\"",""); // remove quotes Android adds
        return TARGET_SSID.equals(ssid);
    }

    private void autoConnect() {
        if (!onTargetWifi()) {
            status.setText("⚠ Not on " + TARGET_SSID);
            return;
        }
        connect();
    }

    private void connect() {
        status.setText("Connecting…");

        executor.execute(() -> {
            boolean ok = false;

            for (int i=0;i<MAX_RETRY && !ok;i++) {
                ok = tryLogin(userField.getText().toString(),
                              passField.getText().toString());
                sleep(1200);
            }

            saveCred();
            boolean r = ok;

            runOnUiThread(() ->
                status.setText(r ? "✅ Connected" : "❌ Failed")
            );
        });
    }

    private boolean tryLogin(String u,String p) {
        try {
            URL url = new URL("http://10.100.1.1:8090/login.xml");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();

            String data =
                "mode=191&username="+u+
                "&password="+p+
                "&a="+System.currentTimeMillis();

            c.setRequestMethod("POST");
            c.setDoOutput(true);

            OutputStream os = c.getOutputStream();
            os.write(data.getBytes());
            os.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String l;
            while ((l=br.readLine())!=null)
                if (l.contains("You are signed in as"))
                    return true;
        } catch (Exception ignored) {}
        return false;
    }

    private void saveCred() {
        SharedPreferences sp = getSharedPreferences("creds", MODE_PRIVATE);
        sp.edit()
          .putString(userField.getText().toString(), passField.getText().toString())
          .putString("last", userField.getText().toString())
          .apply();
    }

    private void sleep(long m) {
        try { Thread.sleep(m); } catch(Exception ignored){}
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}

