package com.example.gpsstandalone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {




        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);

            String token = prefs.getString("bot_token", "");
            String chatId = prefs.getString("chat_id", "");
            String interval = prefs.getString("interval", "");
            String uid = prefs.getString("unique_id", "");



            boolean isValid = !token.isEmpty() && !chatId.isEmpty() && !interval.isEmpty() && !uid.isEmpty();

            if (isValid) {
                Log.d("BootReceiver", "Boot completed, config valid. Starting LocationService...");

                // Kirim notifikasi ke Telegram
                String message = "✅ Perangkat menyala ulang\nID: " + uid + "\nService akan dimulai.";
                TelegramHelper telegram = new TelegramHelper(context);
                telegram.sendMessage(token, chatId, message);
                // Mulai LocationService
                Intent serviceIntent = new Intent(context, LocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else {
                Log.d("BootReceiver", "Boot completed, config tidak lengkap. Service tidak dijalankan.");
            }
        }
    }
}
