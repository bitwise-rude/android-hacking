package com.minimal.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;

public class WifiService extends Service {

    static final String TARGET_SSID   = "kushal_wnepal";
    static final String CHANNEL_ID    = "citpc_auto";
    static final String CHANNEL_NAME  = "CITPC Auto-Login";
    static final String CHANNEL_ALERT = "citpc_alert";
    static final String CHANNEL_ALERT_NAME = "CITPC WiFi Detected";

    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean alreadyNotified = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {

        // ─── Create notification channels ───
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService("notification");

            NotificationChannel chFg = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            chFg.setDescription("Keeps the monitor running in background");
            nm.createNotificationChannel(chFg);

            NotificationChannel chAlert = new NotificationChannel(
                    CHANNEL_ALERT, CHANNEL_ALERT_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            chAlert.setDescription("Notifies you when CITPC WiFi is detected");
            nm.createNotificationChannel(chAlert);
        }

        // ─── Start foreground (silent persistent notification) ───
        Notification fg = new Notification.Builder(this,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? CHANNEL_ID : "")
                .setContentTitle("CITPC Connector")
                .setContentText("Watching for " + TARGET_SSID + "…")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();

        startForeground(1, fg);

        // ─── Register network callback ───
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                super.onCapabilitiesChanged(network, caps);
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    checkAndNotify();
                }
            }
            @Override
            public void onLost(Network network) {
                super.onLost(network);
                alreadyNotified = false;
            }
        };

        cm.registerDefaultNetworkCallback(networkCallback);

        // Immediate check in case already connected
        checkAndNotify();

        return START_STICKY;
    }

    private void checkAndNotify() {
        if (alreadyNotified) return;

        String ssid = getCurrentSsid();
        if (!TARGET_SSID.equals(ssid)) return;

        alreadyNotified = true;

        // PendingIntent opens MainActivity with auto_login flag
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("auto_login", true);

        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification alert = new Notification.Builder(this,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? CHANNEL_ALERT : "")
                .setContentTitle("CITPC WiFi Detected")
                .setContentText("Tap to connect automatically")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService("notification");
        nm.notify(2, alert);
    }

    private String getCurrentSsid() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network net = cm.getActiveNetwork();
        if (net == null) return null;

        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Object ti = caps.getTransportInfo();
            if (ti instanceof android.net.wifi.WifiInfo) {
                String ssid = ((android.net.wifi.WifiInfo) ti).getSSID();
                return ssid != null ? ssid.replace("\"", "") : null;
            }
        }

        // Fallback for older API
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        if (wm != null) {
            String ssid = wm.getConnectionInfo().getSSID();
            return ssid != null ? ssid.replace("\"", "") : null;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null && networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}