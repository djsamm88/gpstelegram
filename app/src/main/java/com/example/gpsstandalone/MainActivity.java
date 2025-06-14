package com.example.gpsstandalone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.location.LocationManager;
import android.provider.Settings;

public class MainActivity extends AppCompatActivity {

    EditText tokenInput, chatIdInput, uniqueIdInput, intervalInput;
    Button saveButton;
    SharedPreferences preferences;

    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1001);
                return;
            }
        }


        tokenInput = findViewById(R.id.tokenInput);
        chatIdInput = findViewById(R.id.chatIdInput);
        uniqueIdInput = findViewById(R.id.uniqueIdInput);
        intervalInput = findViewById(R.id.intervalInput);
        saveButton = findViewById(R.id.saveButton);

        preferences = getSharedPreferences("config", MODE_PRIVATE);

        // Load data yang sudah disimpan
        tokenInput.setText(preferences.getString("bot_token", ""));
        chatIdInput.setText(preferences.getString("chat_id", ""));
        uniqueIdInput.setText(preferences.getString("unique_id", ""));
        intervalInput.setText(preferences.getString("interval", "60"));



        saveButton.setOnClickListener(v -> {
            String token = tokenInput.getText().toString().trim();
            String chatId = chatIdInput.getText().toString().trim();
            String uniqueId = uniqueIdInput.getText().toString().trim();
            String interval = intervalInput.getText().toString().trim();

            if (token.isEmpty() || chatId.isEmpty() || uniqueId.isEmpty() || interval.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!hasRequiredPermissions()) {
                Toast.makeText(this, "Izin lokasi belum diberikan!", Toast.LENGTH_SHORT).show();
                requestRequiredPermissions();
                return;
            }

            if (!isLocationServiceEnabled()) {
                Toast.makeText(this, "Layanan lokasi (GPS) belum aktif!", Toast.LENGTH_LONG).show();
                // Buka pengaturan lokasi
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                return;
            }

            // Simpan ke SharedPreferences
            preferences.edit()
                    .putString("bot_token", token)
                    .putString("chat_id", chatId)
                    .putString("unique_id", uniqueId)
                    .putString("interval", interval)
                    .apply();

            Toast.makeText(this, "Disimpan! Service akan dimulai.", Toast.LENGTH_SHORT).show();

            startLocationService();
        });


        // Minta permission jika belum dapat
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
        }
    }





    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences("config", MODE_PRIVATE);

        String token = preferences.getString("bot_token", "");
        String chatId = preferences.getString("chat_id", "");
        String uniqueId = preferences.getString("unique_id", "");
        String interval = preferences.getString("interval", "");

        if (!token.isEmpty() && !chatId.isEmpty() && !uniqueId.isEmpty() && !interval.isEmpty()) {
            if (hasRequiredPermissions()) {
                startLocationService();
            }
        }
    }





    private boolean isLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.FOREGROUND_SERVICE
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                startLocationService();
            } else {
                Toast.makeText(this, "Permission ditolak. Tidak bisa mulai service.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
}
