package com.minimal.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;

public class WifiService extends Service {

    static final String TARGET_SSID  = "CITPC-WIFI";
    static final String CHANNEL_ID   = "citpc_auto";
    static final String CHANNEL_NAME = "CITPC Auto-Login";

    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {

        // ─── Create notification channel (required API 26+) ───
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Monitors WiFi for auto-login");
            ((NotificationManager) getSystemService("notification"))
                    .createNotificationChannel(ch);
        }

        // ─── Start foreground with proper notification ───
        Notification n = new Notification.Builder(this,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? CHANNEL_ID : "")
                .setContentTitle("CITPC Connector")
                .setContentText("Watching for " + TARGET_SSID + "…")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        startForeground(1, n);

        // ─── Register NetworkCallback (modern, non-deprecated) ───
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                super.onCapabilitiesChanged(network, caps);
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    checkAndLaunch();
                }
            }
        };

        cm.registerDefaultNetworkCallback(networkCallback);

        // Also do an immediate check in case we're already on the target WiFi
        checkAndLaunch();

        return START_STICKY;   // Keep alive so callback stays registered
    }

    private void checkAndLaunch() {
        String ssid = getCurrentSsid();
        if (TARGET_SSID.equals(ssid)) {
            // Launch MainActivity with auto_login flag
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            open.putExtra("auto_login", true);
            startActivity(open);
        }
    }

    /**
     * Gets current SSID without using the deprecated getConnectionInfo().
     * Works on API 29+ via NetworkCapabilities; falls back for older versions.
     */
    private String getCurrentSsid() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network net = cm.getActiveNetwork();
        if (net == null) return null;

        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        if (caps == null) return null;

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            // On API 29+ we can get transport info directly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Object transportInfo = caps.getTransportInfo();
                if (transportInfo instanceof android.net.wifi.WifiInfo) {
                    android.net.wifi.WifiInfo wifiInfo = (android.net.wifi.WifiInfo) transportInfo;
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null) {
                        ssid = ssid.replace("\"", "");
                    }
                    return ssid;
                }
            }

            // Fallback for API < 29 — getConnectionInfo() is deprecated but still works
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            if (wm != null) {
                String ssid = wm.getConnectionInfo().getSSID();
                if (ssid != null) {
                    ssid = ssid.replace("\"", "");
                }
                return ssid;
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister callback to avoid leaks
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null && networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}