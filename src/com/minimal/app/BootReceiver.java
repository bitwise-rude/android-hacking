package com.minimal.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (action.equals("android.intent.action.BOOT_COMPLETED")
         || action.equals("android.intent.action.QUICKBOOT_REBOOT")) {
            startAlarm(ctx);
        }
    }

    /**
     * Sets a repeating alarm that fires WifiCheckReceiver every 30 seconds.
     * Safe to call multiple times — setRepeating with the same PendingIntent
     * just replaces the previous one.
     */
    public static void startAlarm(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent checkIntent = new Intent(ctx, WifiCheckReceiver.class);
        checkIntent.setAction("com.minimal.app.WIFI_CHECK");

        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Start immediately, then repeat every 30 seconds
        // 3 = AlarmManager.ELAPSED_REAL_TIME raw value
        am.setRepeating(
                3,
                System.currentTimeMillis(),
                10 * 1000,
                pi);
    }
}