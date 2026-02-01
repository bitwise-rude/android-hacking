package com.minimal.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;

public class WifiCheckReceiver extends BroadcastReceiver {

    static final String TARGET_SSID         = "kushal50_wnepal";
    static final String CHANNEL_ALERT       = "citpc_alert";
    static final String CHANNEL_ALERT_NAME  = "CITPC WiFi Detected";
    // SharedPreferences key to track whether we already notified for this connection
    static final String PREF_NOTIFIED       = "wifi_notified";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        // Create notification channel (idempotent, safe every time)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService("notification");
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ALERT, CHANNEL_ALERT_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Notifies when CITPC WiFi is detected");
            nm.createNotificationChannel(ch);
        }

        String ssid = getCurrentSsid(ctx);

        SharedPreferences sp = ctx.getSharedPreferences("wifi_state", Context.MODE_PRIVATE);

        if (TARGET_SSID.equals(ssid)) {
            // Only notify once per connection — don't spam every 30s
            if (sp.getBoolean(PREF_NOTIFIED, false)) return;
            sp.edit().putBoolean(PREF_NOTIFIED, true).apply();

            postNotification(ctx);
        } else {
            // Not on CITPC anymore — reset so we notify again next time
            sp.edit().putBoolean(PREF_NOTIFIED, false).apply();
        }
    }

    // ──────────────────────────────────────────────

    private void postNotification(Context ctx) {
        Intent open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("auto_login", true);

        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(ctx,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? CHANNEL_ALERT : "")
                .setContentTitle("CITPC WiFi Detected")
                .setContentText("Tap to connect automatically")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) ctx.getSystemService("notification");
        nm.notify(2, n);
    }

    private String getCurrentSsid(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;

        Network net = cm.getActiveNetwork();
        if (net == null) return null;

        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null;

        // API 29+ — proper non-deprecated way
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Object ti = caps.getTransportInfo();
            if (ti instanceof android.net.wifi.WifiInfo) {
                String ssid = ((android.net.wifi.WifiInfo) ti).getSSID();
                return ssid != null ? ssid.replace("\"", "") : null;
            }
        }

        // Fallback for older API
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            String ssid = wm.getConnectionInfo().getSSID();
            return ssid != null ? ssid.replace("\"", "") : null;
        }
        return null;
    }
}