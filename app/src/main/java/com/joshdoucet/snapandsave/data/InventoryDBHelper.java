package com.joshdoucet.snapandsave.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.joshdoucet.snapandsave.R;
import com.joshdoucet.snapandsave.activities.EditorActivity;
import com.joshdoucet.snapandsave.data.InventoryContract.ItemEntry;

/**
 *  - The InventoryDBHelper class will create and access a SQLite database and provide
 *      helper objects that can be used as a readable or writable database for
 *      C.R.U.D. operations.
 */

public class InventoryDBHelper extends SQLiteOpenHelper{

    //file name that the database will be stored in
    public static final String DATABASE_NAME = "snapandsave.db";

    public static final int DATABASE_VERSION = 1;

    public Context mContext;

    public InventoryDBHelper(Context context){
        //null is passed instead of a CursorFactory
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    /**
     *  -All CREATE TABLE statements will be executed in on create
     * @param sqLiteDatabase - SQLite database to execute command in
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a String that contains the SQL statement to create the items tableledkeyoboard


        final String SQL_CREATE_ITEMS_TABLE = "CREATE TABLE " + ItemEntry.TABLE_NAME + " ("
                + ItemEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ItemEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + ItemEntry.COLUMN_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + ItemEntry.COLUMN_PRICE + " REAL NOT NULL DEFAULT " + ItemEntry.NOT_FOR_SALE + ", "
                + ItemEntry.COLUMN_SUPPLIER + " TEXT, "
                + ItemEntry.COLUMN_IMAGE + " BLOB );";

        //Execute the above string in the database to create the items table
        sqLiteDatabase.execSQL(SQL_CREATE_ITEMS_TABLE);

        //Insert 3 sample rows into the database
        ContentValues sampleVals = new ContentValues();

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.moon_boots);
        byte[] blob = EditorActivity.bitToByteArray(bitmap);

        sampleVals.put(ItemEntry.COLUMN_NAME, "Bouncy Moon Boots");
        sampleVals.put(ItemEntry.COLUMN_SUPPLIER, "N.A.S.A.");
        sampleVals.put(ItemEntry.COLUMN_QUANTITY, 3);
        sampleVals.put(ItemEntry.COLUMN_PRICE, ItemEntry.NOT_FOR_SALE);
        sampleVals.put(ItemEntry.COLUMN_IMAGE, blob);

        sqLiteDatabase.insert(ItemEntry.TABLE_NAME, null, sampleVals);
        sampleVals.clear();

        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.keyboard);
        blob = EditorActivity.bitToByteArray(bitmap);

        sampleVals.put(ItemEntry.COLUMN_NAME, "LED Keyboard USB 3.0");
        sampleVals.put(ItemEntry.COLUMN_SUPPLIER, "AULA");
        sampleVals.put(ItemEntry.COLUMN_QUANTITY, 12);
        sampleVals.put(ItemEntry.COLUMN_PRICE, 24.99);
        sampleVals.put(ItemEntry.COLUMN_IMAGE, blob);

        sqLiteDatabase.insert(ItemEntry.TABLE_NAME, null, sampleVals);
        sampleVals.clear();

        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.towel);
        blob = EditorActivity.bitToByteArray(bitmap);
        sampleVals.put(ItemEntry.COLUMN_NAME, "Dirty Towel");
        sampleVals.put(ItemEntry.COLUMN_SUPPLIER, "El Gato Largo Inc");
        sampleVals.put(ItemEntry.COLUMN_QUANTITY, 1);
        sampleVals.put(ItemEntry.COLUMN_PRICE, ItemEntry.FREE);
        sampleVals.put(ItemEntry.COLUMN_IMAGE, blob);

        sqLiteDatabase.insert(ItemEntry.TABLE_NAME, null, sampleVals);
    }

    /**
     * Used to upgrade database to new version
     * @param sqLiteDatabase db to update
     * @param oldVersion old version num
     * @param newVersion new version num
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        //Do nothing for now. Database is still in version 1
    }
}
