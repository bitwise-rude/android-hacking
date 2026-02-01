package com.minimal.app;

import android.content.Context;
import android.net.*;

public final class NetworkUtils {

    private static ConnectivityManager.NetworkCallback callback;

    public static boolean isWifi(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network net = cm.getActiveNetwork();
        if (net == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    public static void registerNetworkCallback(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null || callback != null) return;

        NetworkRequest req = new NetworkRequest.Builder().build();

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // background-safe only
            }
        };

        cm.registerNetworkCallback(req, callback);
    }
}

