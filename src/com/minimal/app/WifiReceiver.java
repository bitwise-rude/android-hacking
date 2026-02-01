package com.minimal.app;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        // This receiver is kept as a fallback for older Android versions.
        // On API 28+, WifiService registers its own NetworkCallback which is more reliable.

        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        Network net = cm.getActiveNetwork();
        if (net == null) return;

        NetworkCapabilities cap = cm.getNetworkCapabilities(net);
        if (cap != null && cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Intent i = new Intent(ctx, WifiService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i);
            } else {
                ctx.startService(i);
            }
        }
    }
}