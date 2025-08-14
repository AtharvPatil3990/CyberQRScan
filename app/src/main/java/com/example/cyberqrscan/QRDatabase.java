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

//CREATE TABLE url_hash_prefixes (
//        id INTEGER PRIMARY KEY AUTOINCREMENT,
//
//);


public class QRDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "cyberQRScan.db";
    private static final int DATABASE_VERSION = 1;
    public static final String scanTable = "ScanHistory" ;
    public static final String generateTable = "GenerateHistory" ;
    public static final String urlHashTable = "URL_Hash_Table";
    public static final String type = "type";
    public static final String data = "data";
    public static final String timestamp = "timestamp";
    private static final String createTableScan = "CREATE TABLE " + scanTable + " (" +
            type + " TEXT, " +
            data + " TEXT, " +
            timestamp + " INTEGER" +
            ")";
    private static final String createTableGenerate = "CREATE TABLE " + generateTable + " (" +
            type + " TEXT, " +
            data + " TEXT, " +
            timestamp + " INTEGER" +
            ")";
    private static final String createTableURLHash = "CREATE TABLE " + urlHashTable +
            " ( id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "    hash_prefix BLOB NOT NULL,\n" +
            "    threat_type TEXT NOT NULL);";
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

    public void UpdateURLHash(String hash, String threat_type, long expirationTime){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("hash_prefix", hash);
        values.put("threat_type", threat_type);
        db.insert(urlHashTable, null, values);
    }

    public long insertData(String scanType, String scanData, long scanTimestamp , String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(type, scanType);
        values.put(data, scanData);
        values.put(timestamp, scanTimestamp);
        return db.insert(tableName, null, values);
    }
    public Cursor getAllScan() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(scanTable, null, null, null, null, null, timestamp + " DESC");
    }
    public Cursor getAllGenerate() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(generateTable, null, null, null, null, null, timestamp + " DESC");
    }
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(scanTable, null, null);
        db.delete(generateTable , null , null) ;
    }

    void saveHashesToDatabase(String rawHashes, int prefixSize, String threatType){
        try {
            // Step 1: Decode Base64 into raw bytes
            byte[] decodedBytes = Base64.decode(rawHashes, Base64.DEFAULT);

            // Step 2: Split into prefixes and insert into DB
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                for (int i = 0; i < decodedBytes.length; i += prefixSize) {
                    byte[] prefix = Arrays.copyOfRange(decodedBytes, i, i + prefixSize);

                    ContentValues values = new ContentValues();
                    values.put("hash_prefix", prefix);
                    values.put("threat_type",threatType);

                    db.insert(urlHashTable, null, values); // Adding threat to DB
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

        // Step 1: Get all hashes sorted
        List<String> sortedHashes = new ArrayList<>();
        Cursor cursor = db.query(
                urlHashTable,                // table name
                new String[]{"hash_prefix"},    // columns
                null,                           // where clause
                null,                           // where args
                null,                           // group by
                null,                           // having
                "hash_prefix ASC"               // order by
        );

        while (cursor.moveToNext()) {
            sortedHashes.add(cursor.getString(0));
        }
        cursor.close();

        // Step 2: Collect hashes to delete using indices
        List<String> hashesToDelete = new ArrayList<>();
        for (int i = 0; i < indices.length(); i++) {
            int idx = indices.optInt(i, -1);
            if (idx >= 0 && idx < sortedHashes.size()) {
                hashesToDelete.add(sortedHashes.get(idx));
            }
        }

        // Step 3: Delete matching hashes
        for (String hash : hashesToDelete) {
            db.delete("threat_hashes", "hash_prefix = ?", new String[]{hash});
        }
    }



    public boolean isHashPrefixInDatabase(String hashPrefix){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                urlHashTable,                      // Table
                new String[]{"hash_prefix"},           // Columns
                "hash_prefix = ?",                     // WHERE clause
                new String[]{hashPrefix},              // WHERE args
                null, null, null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }
}
