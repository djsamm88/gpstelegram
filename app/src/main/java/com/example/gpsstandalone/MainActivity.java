package com.example.gpsstandalone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    EditText tokenInput, chatIdInput, uniqueIdInput, intervalInput;
    Button saveButton, btnTestVoice, recordButton, btnRequestAllPermissions;
    SharedPreferences preferences;

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("config", MODE_PRIVATE);

        // Inisialisasi UI
        tokenInput = findViewById(R.id.tokenInput);
        chatIdInput = findViewById(R.id.chatIdInput);
        uniqueIdInput = findViewById(R.id.uniqueIdInput);
        intervalInput = findViewById(R.id.intervalInput);
        saveButton = findViewById(R.id.saveButton);
        btnTestVoice = findViewById(R.id.btnTestVoice);
        recordButton = findViewById(R.id.recordButton);
        btnRequestAllPermissions = findViewById(R.id.btnRequestAllPermissions);

        // Load data
        tokenInput.setText(preferences.getString("bot_token", ""));
        chatIdInput.setText(preferences.getString("chat_id", ""));
        uniqueIdInput.setText(preferences.getString("unique_id", ""));
        intervalInput.setText(preferences.getString("interval", "60"));

        // Tombol simpan & mulai
        saveButton.setOnClickListener(v -> {
            String token = tokenInput.getText().toString().trim();
            String chatId = chatIdInput.getText().toString().trim();
            String uniqueId = uniqueIdInput.getText().toString().trim();
            String interval = intervalInput.getText().toString().trim();

            if (token.isEmpty() || chatId.isEmpty() || uniqueId.isEmpty() || interval.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!hasAllPermissions()) {
                Toast.makeText(this, "Beberapa izin wajib belum diberikan!", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isLocationServiceEnabled()) {
                Toast.makeText(this, "GPS belum aktif. Aktifkan dulu!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }

            preferences.edit()
                    .putString("bot_token", token)
                    .putString("chat_id", chatId)
                    .putString("unique_id", uniqueId)
                    .putString("interval", interval)
                    .apply();

            Toast.makeText(this, "Disimpan! Service akan dimulai.", Toast.LENGTH_SHORT).show();
            startLocationService();
        });

        // Tombol test kirim suara
        btnTestVoice.setOnClickListener(v -> {
            if (hasAudioPermission()) {
                VoiceRecorder.getInstance().startRecording(
                        MainActivity.this,
                        10,
                        tokenInput.getText().toString().trim(),
                        chatIdInput.getText().toString().trim()
                );
                Toast.makeText(this, "Merekam 10 detik...", Toast.LENGTH_SHORT).show();
            } else {
                requestAudioPermission();
            }
        });

        // Tombol manual rekam
        recordButton.setOnClickListener(v -> {
            if (hasAudioPermission()) {
                VoiceRecorder.getInstance().startRecording(
                        this,
                        5,
                        tokenInput.getText().toString().trim(),
                        chatIdInput.getText().toString().trim()
                );
                Toast.makeText(this, "Merekam 5 detik...", Toast.LENGTH_SHORT).show();
            } else {
                requestAudioPermission();
            }
        });

        // Tombol periksa & minta semua izin
        btnRequestAllPermissions.setOnClickListener(v -> {
            requestAllPermissions();
            openBatteryOptimizationSettings();
            openAutoStartSettings();
        });

        // Minta permission awal
        if (!hasAllPermissions()) {
            requestAllPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String token = preferences.getString("bot_token", "");
        String chatId = preferences.getString("chat_id", "");
        String uniqueId = preferences.getString("unique_id", "");
        String interval = preferences.getString("interval", "");

        if (!token.isEmpty() && !chatId.isEmpty() && !uniqueId.isEmpty() && !interval.isEmpty()) {
            if (hasAllPermissions() && isLocationServiceEnabled()) {
                startLocationService();
            }
        }
    }

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                (!isBatteryOptimizationEnabled());
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO
                },
                PERMISSION_REQUEST_CODE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 3003);
        }
    }

    private boolean isLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_PERMISSION_REQUEST_CODE);
    }

    private void startLocationService() {
        Intent intent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    // Battery Optimization
    private boolean isBatteryOptimizationEnabled() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        }
    }

    // Auto-start (khusus vendor seperti Xiaomi, Oppo, dll)
    private void openAutoStartSettings() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Tidak bisa buka pengaturan auto-start", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Callback hasil permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int result : results) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            Toast.makeText(this, granted ? "Semua izin diberikan." : "Beberapa izin ditolak.", Toast.LENGTH_SHORT).show();
        } else if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this,
                    (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
                            ? "Izin mic diberikan."
                            : "Izin mic ditolak!", Toast.LENGTH_SHORT).show();
        }
    }
}
