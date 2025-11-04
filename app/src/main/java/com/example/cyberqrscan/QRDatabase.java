package com.example.cyberqrscan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QRDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "cyberQRScan.db";
    private static final int DATABASE_VERSION = 1;

    public static final String scanTable = "ScanHistory";
    public static final String generateTable = "GenerateHistory";
    public static final String urlHashTable = "URL_Hash_Table";

    public static final String type = "type";
    public static final String data = "data";
    public static final String timestamp = "timestamp";

    private static final String createTableScan = "CREATE TABLE " + scanTable + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            type + " TEXT, " +
            data + " TEXT, " +
            timestamp + " LONG" +
            ");" ;

    private static final String createTableGenerate = "CREATE TABLE " + generateTable + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
    type + " TEXT, " +
    data + " TEXT, " +
    timestamp + " LONG" +
            ");";

    private static final String createTableURLHash = "CREATE TABLE " + urlHashTable +
            " ( id INTEGER PRIMARY KEY AUTOINCREMENT," +
            " hash_prefix TEXT NOT NULL," +
            " threat_type TEXT NOT NULL);";

    public QRDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(createTableScan);
        sqLiteDatabase.execSQL(createTableGenerate);
        sqLiteDatabase.execSQL(createTableURLHash);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + scanTable);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + generateTable);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + urlHashTable);
        onCreate(sqLiteDatabase);
    }

    public long insertData(String scanType, String scanData, long scanTimestamp, String tableName , boolean permission) {
        if (permission) {
            return -1; // clearly show skipped insertion
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(type, scanType);
        values.put(data, scanData);
        values.put(timestamp, scanTimestamp);
        return db.insert(tableName, null, values);
    }


    public List<String> getAllScan() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> results = new ArrayList<>();

        Cursor cursor = db.query(
                scanTable,
                new String[]{type, data, timestamp},
                null, null, null, null,
                timestamp + " DESC"   // latest first
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String entry = "Type: " + cursor.getString(0) +
                        "\nData: " + cursor.getString(1) +
                        "\nTime: " + cursor.getLong(2);
                results.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return results;
    }


    public List<String> getAllGenerate() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> results = new ArrayList<>();

        Cursor cursor = db.query(
                generateTable,
                new String[]{type, data, timestamp},
                null, null, null, null,
                timestamp + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String entry = "Type: " + cursor.getString(0) +
                        "\nData: " + cursor.getString(1) +
                        "\nTime: " + cursor.getLong(2);
                results.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return results;
    }


    public void deleteAll(String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(table, null, null);
    }

    void insertHashesToDatabase(String rawHashes, int prefixSize, String threatType) {
        try {
            byte[] decodedBytes = Base64.decode(rawHashes, Base64.DEFAULT);

            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                for (int i = 0; i < decodedBytes.length; i += prefixSize) {
                    byte[] prefix = Arrays.copyOfRange(decodedBytes, i, i + prefixSize);

                    String prefixBase64 = Base64.encodeToString(prefix, Base64.NO_WRAP);

                    ContentValues values = new ContentValues();
                    values.put("hash_prefix", prefixBase64);
                    values.put("threat_type", threatType);
                    System.out.println("hash_prefix: "+ prefixBase64 + ", threat_type: " + threatType);
                    db.insert(urlHashTable, null, values);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                db.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeHashesFromDatabase(JSONArray indices) {
        SQLiteDatabase db = this.getWritableDatabase();

        List<String> sortedHashes = new ArrayList<>();
        Cursor cursor = db.query(
                urlHashTable,
                new String[]{"hash_prefix"},
                null,
                null,
                null,
                null,
                "hash_prefix ASC"
        );

        while (cursor.moveToNext()) {
            sortedHashes.add(cursor.getString(0));
        }
        cursor.close();

        List<String> hashesToDelete = new ArrayList<>();
        for (int i = 0; i < indices.length(); i++) {
            int idx = indices.optInt(i, -1);
            if (idx >= 0 && idx < sortedHashes.size()) {
                hashesToDelete.add(sortedHashes.get(idx));
            }
        }

        // ✅ FIXED: Delete from urlHashTable instead of non-existing threat_hashes
        for (String hash : hashesToDelete) {
            db.delete(urlHashTable, "hash_prefix = ?", new String[]{hash});
        }
    }

    public String getUrlThreatType(String hashPrefix) {
        String threatType = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT threat_type FROM " + urlHashTable + " WHERE hash_prefix = ?",
                new String[]{hashPrefix}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                threatType = cursor.getString(cursor.getColumnIndexOrThrow("threat_type"));
            }
            cursor.close();
        }
        db.close();
        return threatType;
    }

    public boolean isHashPrefixInDatabase(String hashPrefix) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                urlHashTable,
                new String[]{"hash_prefix"},
                "hash_prefix = ?",
                new String[]{hashPrefix},
                null, null, null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public void showAllHashes() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + urlHashTable, null);
        while (cursor.moveToNext()){
            System.out.println("Hash: "+cursor.getString(1) + ", Threat type: " + cursor.getString(2));
        }
        cursor.close();
        db.close();
    }
}
