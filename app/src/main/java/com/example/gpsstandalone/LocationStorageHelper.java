package com.example.gpsstandalone;

import android.content.Context;
import android.database.sqlite.*;
import android.content.ContentValues;
import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationStorageHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "gps.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "offline_locations";

    public LocationStorageHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, lat REAL, lon REAL, caption TEXT,timestamp TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void save(double lat, double lon, String caption) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put("timestamp", time);
        values.put("lat", lat);
        values.put("lon", lon);
        values.put("caption", caption);
        db.insert(TABLE_NAME, null, values);
    }

    public List<LocationData> getAll() {
        List<LocationData> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        while (cursor.moveToNext()) {
            LocationData data = new LocationData();
            data.id = cursor.getInt(0);
            data.lat = cursor.getDouble(1);
            data.lon = cursor.getDouble(2);
            data.caption = cursor.getString(3);
            data.timestamp = cursor.getString(4);

            list.add(data);
        }
        cursor.close();
        return list;
    }

    public void delete(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});
    }

    public static class LocationData {
        public int id;
        public double lat;
        public double lon;
        public String caption;

        public String timestamp;

    }
}
