package com.example.cyberqrscan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class QRDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "cyberQRScan.db";
    private static final int DATABASE_VERSION = 1;
    public static final String scanTable = "ScanHistory" ;
    public static final String generateTable = "GenerateHistory" ;
    public static final String type = "type";
    public static final String data = "data";
    public static final String timestamp = "timestamp";
    public static List<List<String>> scans  = new ArrayList<>() ;
    public static List <List<String>> generates = new ArrayList<>() ;
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
    public QRDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(createTableScan);
        sqLiteDatabase.execSQL(createTableGenerate);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + scanTable);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + generateTable);
        onCreate(sqLiteDatabase);
    }
    public long insertData(String scanType, String scanData, long scanTimestamp , String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(type, scanType);
        values.put(data, scanData);
        values.put(timestamp, scanTimestamp);
        return db.insert(tableName, null, values);
    }
    public List getAllScan() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM " + QRDatabase.scanTable, null);
        List <String> row = new ArrayList<>() ;

        if (cursor.moveToFirst()) {
            do {
                row.add(cursor.getString(0));
                row.add(cursor.getString(1));
                scans.add(row) ;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return scans;}
    public List getAllGenerate() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM " + QRDatabase.generateTable, null);
        List <String> row = new ArrayList<>() ;

        if (cursor.moveToFirst()) {
            do {
                row.add(cursor.getString(0));
                row.add(cursor.getString(1));
                generates.add(row) ;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return generates;}
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(scanTable, null, null);
        db.delete(generateTable , null , null) ;
    }
}
