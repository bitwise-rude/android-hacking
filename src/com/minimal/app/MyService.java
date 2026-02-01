package com.minimal.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MyService extends Service {

    private static final String TAG = "CITPC_SERVICE";
    private static final String CHANNEL_ID = "citpc_wifi_channel";
    private static final String CHANNEL_UPDATES_ID = "citpc_updates_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;
    
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private NotificationManager notificationManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler;
    private AtomicInteger notificationIdCounter = new AtomicInteger(2000);
    
    private boolean wasConnected = false;
    private boolean isLoggingIn = false;
    private static final String PREF_NAME = "creds";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate()");
        
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
        
        // Start as foreground service with persistent notification
        startForeground(FOREGROUND_NOTIFICATION_ID, createPersistentNotification());
        
        // Setup network monitoring
        setupNetworkMonitoring();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        showToast("🔵 CITPC Monitor Started");
        showUpdateNotification("🔵 Monitor Started", "Watching for WiFi changes...");
        return START_STICKY;
    }

    private void setupNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d(TAG, "🟢 WiFi CONNECTED!");
                
                if (!wasConnected) {
                    wasConnected = true;
                    showToast("🟢 WiFi Connected!");
                    showUpdateNotification("🟢 WiFi Connected", "Preparing auto-login in 2 seconds...");
                    
                    // Wait 2 seconds then auto-login
                    executor.execute(() -> {
                        try {
                            Thread.sleep(2000);
                            autoLogin();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d(TAG, "🔴 WiFi DISCONNECTED!");
                wasConnected = false;
                isLoggingIn = false;
                showToast("🔴 WiFi Disconnected");
                showUpdateNotification("🔴 WiFi Disconnected", "Waiting for reconnection...");
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                super.onCapabilitiesChanged(network, capabilities);
                
                boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                
                Log.d(TAG, "Network - Internet: " + hasInternet + ", Validated: " + isValidated);
                
                // If WiFi connected but no internet, try to login
                if (hasInternet && !isValidated && !isLoggingIn) {
                    Log.d(TAG, "🔶 WiFi has no internet - attempting login");
                    showToast("🔶 Portal Detected!");
                    executor.execute(this::checkAndLogin);
                }
            }
            
            private void checkAndLogin() {
                try {
                    Thread.sleep(1000);
                    autoLogin();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            Log.d(TAG, "✅ Network monitoring started");
        }
    }

    private void autoLogin() {
        if (isLoggingIn) {
            Log.d(TAG, "Already logging in, skipping...");
            return;
        }
        
        isLoggingIn = true;
        
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastUser = sp.getString("last_user", null);
        
        if (lastUser == null || lastUser.isEmpty()) {
            Log.d(TAG, "❌ No saved credentials");
            showToast("❌ No saved account - open app");
            showUpdateNotification("⚠️ No Account Saved", "Open app to save login credentials");
            isLoggingIn = false;
            return;
        }
        
        String password = sp.getString("pass_" + lastUser, "");
        
        Log.d(TAG, "🔑 Auto-login attempt for: " + lastUser);
        showToast("🔑 Logging in as " + lastUser + "...");
        
        boolean success = false;
        int attempts = 0;
        
        for (int i = 0; i < 1; i++) {
            attempts = i + 1;
            Log.d(TAG, "📡 Login attempt " + attempts + "/3");
            
           
            success = tryLogin(lastUser, password);
            if (success) break;
            
            if (i < 2) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        if (success) {
            Log.d(TAG, "✅ AUTO-LOGIN SUCCESS!");
            showToast("✅ Logged in as " + lastUser);
        } else {
            Log.d(TAG, "❌ AUTO-LOGIN FAILED after " + attempts + " attempts");
            showToast("❌ Login failed - check credentials or maybe wrong Wifi");
        }
        
        isLoggingIn = false;
    }

    private boolean tryLogin(String username, String password) {
        HttpURLConnection connection = null;
        try {
            Log.d(TAG, "🌐 Connecting to CITPC portal...");
            
            URL url = new URL("http://10.100.1.1:8090/login.xml");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            String data = "mode=191&username=" + username + "&password=" + password + "&a=" + System.currentTimeMillis();
            OutputStream os = connection.getOutputStream();
            os.write(data.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            StringBuilder response = new StringBuilder();
            
            while ((line = br.readLine()) != null) {
                response.append(line);
                if (line.contains("signed in")) {
                    br.close();
                    Log.d(TAG, "✅ Portal responded: Login successful!");
                    return true;
                }
            }
            br.close();
            
            Log.d(TAG, "Response: " + response.toString());
            
        } catch (java.net.ConnectException e) {
            Log.e(TAG, "❌ Connection failed: " + e.getMessage());
            Log.e(TAG, "Possible reasons: Not on CITPC WiFi, Already logged in, or Portal IP changed");
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "❌ Connection timeout");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error: " + e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
        return false;
    }

    private void showToast(final String message) {
        mainHandler.post(() -> Toast.makeText(MyService.this, message, Toast.LENGTH_LONG).show());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for persistent foreground notification
            NotificationChannel persistentChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "CITPC Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            persistentChannel.setDescription("Keeps the monitoring service running");
            persistentChannel.setShowBadge(false);
            
            // Channel for status updates - HIGH IMPORTANCE so they show up!
            NotificationChannel updatesChannel = new NotificationChannel(
                    CHANNEL_UPDATES_ID,
                    "CITPC Status Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            updatesChannel.setDescription("Login status and WiFi notifications");
            updatesChannel.enableVibration(true);
            updatesChannel.enableLights(true);
            updatesChannel.setLightColor(Color.BLUE);
            updatesChannel.setShowBadge(true);
            
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(persistentChannel);
                notificationManager.createNotificationChannel(updatesChannel);
                Log.d(TAG, "✅ Notification channels created");
            }
        }
    }

    private Notification createPersistentNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("CITPC Auto-Login")
                .setContentText("Monitoring WiFi in background...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void showUpdateNotification(String title, String message) {
        if (notificationManager == null) return;
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_UPDATES_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .build();

        // Use different ID for each notification so they all show up
        int notificationId = notificationIdCounter.incrementAndGet();
        notificationManager.notify(notificationId, notification);
        
        Log.d(TAG, "📱 Notification sent: " + title + " - " + message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        // Unregister network callback
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "Network callback unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering callback: " + e.getMessage());
            }
        }
        
        // Shutdown executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        
        showToast("🔴 CITPC Monitor Stopped");
        showUpdateNotification("🔴 Monitor Stopped", "Auto-login service has been stopped");
    }
}