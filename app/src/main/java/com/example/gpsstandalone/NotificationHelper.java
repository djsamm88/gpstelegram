package com.example.gpsstandalone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "gps_channel_id";
    private static final String CHANNEL_NAME = "GPS Tracking Service";

    public static void createForegroundNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for GPS tracking service");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static Notification getNotification(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("GPS Tracker Aktif")
                .setContentText("Mengirim lokasi secara otomatis...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
    }
}
