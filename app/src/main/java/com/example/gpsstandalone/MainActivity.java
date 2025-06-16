package com.example.gpsstandalone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    EditText tokenInput, chatIdInput, uniqueIdInput, intervalInput;
    Button saveButton, btnTestVoice;
    Button recordButton;
    SharedPreferences preferences;

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Audio (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST_CODE);
            }
        }

        // Inisialisasi UI
        tokenInput = findViewById(R.id.tokenInput);
        chatIdInput = findViewById(R.id.chatIdInput);
        uniqueIdInput = findViewById(R.id.uniqueIdInput);
        intervalInput = findViewById(R.id.intervalInput);
        saveButton = findViewById(R.id.saveButton);
        btnTestVoice = findViewById(R.id.btnTestVoice);
        recordButton = findViewById(R.id.recordButton);

        preferences = getSharedPreferences("config", MODE_PRIVATE);

        // Load data
        tokenInput.setText(preferences.getString("bot_token", ""));
        chatIdInput.setText(preferences.getString("chat_id", ""));
        uniqueIdInput.setText(preferences.getString("unique_id", ""));
        intervalInput.setText(preferences.getString("interval", "60"));

        // Tombol simpan konfigurasi
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
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
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

        // Tombol uji rekam suara
        btnTestVoice.setOnClickListener(v -> {
            if (hasAllAudioPermissions()) {
                String token = tokenInput.getText().toString().trim();
                String chatId = chatIdInput.getText().toString().trim();

                if (token.isEmpty() || chatId.isEmpty()) {
                    Toast.makeText(this, "Token dan Chat ID wajib diisi untuk test!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this, "Merekam suara 10 detik...", Toast.LENGTH_SHORT).show();
                VoiceRecorder.getInstance().startRecording(MainActivity.this, 10, token, chatId);
            } else {
                requestAudioPermission();
            }
        });

        // Tombol manual rekam suara
        recordButton.setOnClickListener(v -> {
            String token = tokenInput.getText().toString().trim();
            String chatId = chatIdInput.getText().toString().trim();

            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "Isi token dan chat ID dulu", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1002);
                return;
            }

            Toast.makeText(this, "Merekam suara selama 5 detik...", Toast.LENGTH_SHORT).show();
            VoiceRecorder.getInstance().startRecording(this, 5, token, chatId);
        });

        // Minta permission awal jika belum diberikan
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
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
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAllAudioPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION
                },
                PERMISSION_REQUEST_CODE);
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_PERMISSION_REQUEST_CODE);
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
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

        } else if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin microphone diberikan.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Izin microphone ditolak!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
