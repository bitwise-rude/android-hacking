package com.minimal.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        // Fire on both BOOT_COMPLETED and QUICKBOOT_REBOOT
        if (action.equals("android.intent.action.BOOT_COMPLETED")
         || action.equals("android.intent.action.QUICKBOOT_REBOOT")) {

            Intent svc = new Intent(ctx, WifiService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(svc);
            } else {
                ctx.startService(svc);
            }
        }
    }
}