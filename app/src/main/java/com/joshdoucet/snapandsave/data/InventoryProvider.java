package com.joshdoucet.snapandsave.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.joshdoucet.snapandsave.R;
import com.joshdoucet.snapandsave.data.InventoryContract.ItemEntry;

/**
 *   - The InventoryProvider class works as a ContentProvider that directly performs operations
 *      on the database. This is the only place the DB should be directly interacted with.
 *      The provider acts as an abstraction layer between the UI and the data. The UI code should
 *      interact with the ContentResolver which will then work with the InventoryProvider.
 *      All data integrity checks are done within this class to ensure bad data is not entered
 *      into the database
 */

public class InventoryProvider extends ContentProvider{
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    //Matches a provided URI with a URI type so proper operations can be performed on
    //that specific uri.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /**
     * Integer codes that identify different types of URI's within the InventoryProvider
     */

    //Code for URIs that identify the entire items table
    private static final int ITEMS_TABLE_CODE = 100;
    //Code for URIs that identify a specific item by _ID
    private static final int SINGLE_ITEM_CODE = 101;

    // Static initializer. This is run the first time anything is called from this class.
    static{
        //Add URI definitions (CONTENT_AUTHORITY, PAT_NAME, MATCHER_CODE)
        //definition for referencing the whole items table URI
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY,
                InventoryContract.PATH_ITEMS, ITEMS_TABLE_CODE);
        //definition for referencing a single item in the items table by _ID
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY,
                InventoryContract.PATH_ITEMS + "/#", SINGLE_ITEM_CODE);
    }

    //Helper object that can interact with the snapandsave.db
    public InventoryDBHelper mInventoryDBHelper;

    @Override
    public boolean onCreate() {
        //Initialize DB helper
        mInventoryDBHelper = new InventoryDBHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query( Uri uri, String[] projection, String selection,
                         String[] selectionArgs,  String sortOrder) {
        //Get a readable version of our inventory database
        SQLiteDatabase readDB = mInventoryDBHelper.getReadableDatabase();
        //Data that will be returned
        Cursor cursor;

        //Matched code returned from the URI parameter
        int matchCode = sUriMatcher.match(uri);
        switch (matchCode){
            case ITEMS_TABLE_CODE:
                //Query the database using provided parameters. The cursor may
                //contain multiple rows
                cursor = readDB.query(ItemEntry.TABLE_NAME,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);
                //groupBy and having parameters not needed, null inserted
                break;
            case SINGLE_ITEM_CODE:
                //Select one row equal to the uri's +_ID
                //Using selection and selectionArgs together helps prevent unwanted data being
                //injected into our SQLite database
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                //Query the database for a single row based on the uri _ID
                //Only one row will be returned in the cursor
                cursor = readDB.query(ItemEntry.TABLE_NAME,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException(getContext().
                        getString(R.string.unknown_uri_query));
        }

        //Set notification URI on the cursor, so we know what
        //content URI the cursor was made for
        //if data at the URI changes then we know the cursor must be updated
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType( Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS_TABLE_CODE:
                return ItemEntry.CONTENT_LIST_TYPE;
            case SINGLE_ITEM_CODE:
                return ItemEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert( Uri uri,  ContentValues contentValues) {
        int matchCode = sUriMatcher.match(uri);
        switch (matchCode){
            //only URIs for an entire items table are supported for insertion
            case ITEMS_TABLE_CODE:
                //Insert item into SQLite database and return the URI for the new row
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException(getContext()
                        .getString(R.string.cannot_insert_uri) + uri);
        }
    }

    public Uri insertItem(Uri uri, ContentValues values){
        //Check if values has valid data that can be inserted into the database
        boolean hasDataIntegrity = hasDataIntegrity(values);

        if(!hasDataIntegrity){
            //Show toast message about bad data, return null early
            Context c = getContext();
            Toast.makeText(c, c.getString(R.string.invalid_data),
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        //Insert valid ContentValue pairs into the database in a new row then,
        //return thr new row ID into a variable of type long
        SQLiteDatabase writeDB = mInventoryDBHelper.getWritableDatabase();
        long newRowId = writeDB.insert(ItemEntry.TABLE_NAME, null, values);

        //if no new row was inserted..
        if(newRowId == -1){
            //Log event and return null early
            Log.e(LOG_TAG, getContext().getString(R.string.cannot_insert_uri) + uri);
            return null;
        }

        //Notify listeners that data has changed for the current URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the URI with the new ID appended to the end of it
        return ContentUris.withAppendedId(uri, newRowId);
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete( Uri uri,  String selection,
                       String[] selectionArgs) {
        SQLiteDatabase writeDB = mInventoryDBHelper.getWritableDatabase();

        //return value, num of rows deleted from database
        int rowsDeleted;

        //Get a matcher code for the parameter uri
        final int matchCode = sUriMatcher.match(uri);

        switch(matchCode){
            case ITEMS_TABLE_CODE:
                //Delete ALL rows in database
                rowsDeleted = writeDB.delete(ItemEntry.TABLE_NAME, null, null);
                break;
            case SINGLE_ITEM_CODE:
                //Delete a SINGLE row from the database
                selection = ItemEntry._ID + "=?";
                //extract _ID from Uri
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = writeDB.delete(ItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(getContext()
                        .getString(R.string.invalid_delete_uri));
        }

        if(rowsDeleted != 0){
            //Notify content listeners that data has changed in the database
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update( Uri uri,  ContentValues contentValues,
                       String selection,  String[] selectionArgs) {
        //Check if Content values is empty, if so return early nothing will be updated
        if(contentValues.size() == 0) {
            return 0;
        }

        //Check content values for data integrity
        boolean hasDataIntegrity = hasDataIntegrity(contentValues);
        if(!hasDataIntegrity){
            //Show toast message about bad data, return null early
            Context c = getContext();
            Toast.makeText(c, c.getString(R.string.invalid_data),
                    Toast.LENGTH_SHORT).show();
            return 0;
        }

        //Writable database object
        SQLiteDatabase writeDB = mInventoryDBHelper.getWritableDatabase();
        //Return value num of rows in DB updated
        int rowsUpdated;
        //Find a uri matcher code
        final int matchCode = sUriMatcher.match(uri);

        switch(matchCode){
            case ITEMS_TABLE_CODE:
                //Update ALL rows in database
                rowsUpdated = writeDB.update(ItemEntry.TABLE_NAME, contentValues,
                        selection, selectionArgs);
                break;
            case SINGLE_ITEM_CODE:
                //Update a SINGLE row from the database
                selection = ItemEntry._ID + "=?";
                //extract _ID from Uri
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsUpdated = writeDB.update(ItemEntry.TABLE_NAME, contentValues,
                        selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(getContext()
                        .getString(R.string.invalid_update_uri));
        }

        if(rowsUpdated > 0){
            //Notify content listeners that data has changed in the database
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    /**
     * Check ContentValues for valid database entries.
     * If invalid throw an IllegalArgumentException return BAD_DATA or false
     * @param values ContentValue pairs to enter into the database
     */
    public boolean hasDataIntegrity(ContentValues values){
        //return value constants
        final boolean GOOD_DATA = true;
        final boolean BAD_DATA = false;

        Context context = getContext();

        //Throw IllegalArgumentException if "Bad Data" is found
        try{
            //Valid name values cannot be empty and must be shorter than 35 chars
            if(values.containsKey(ItemEntry.COLUMN_NAME)){
                String name = values.getAsString(ItemEntry.COLUMN_NAME);
                if(TextUtils.isEmpty(name) || name.length() > 35){
                    Toast.makeText(context, context.getString(R.string.invalid_name_toast),
                            Toast.LENGTH_SHORT).show();
                    throw new IllegalArgumentException
                            (context.getString(R.string.invalid_name));
                }
            }

            //Valid quantity values cannot be less than zero
            if(values.containsKey(ItemEntry.COLUMN_QUANTITY)){
                //Quantity cannot be null. This field is required
                if(values.getAsInteger(ItemEntry.COLUMN_QUANTITY) == null){
                    Toast.makeText(context, context.getString(R.string.quantity_empty),
                            Toast.LENGTH_SHORT).show();
                    throw new NullPointerException(LOG_TAG);
                }
                int quantity = values.getAsInteger(ItemEntry.COLUMN_QUANTITY);
                if(quantity < 0 || quantity >= ItemEntry.MAX_QUANTITY){
                    Toast.makeText(context, context.getString(R.string.invalid_quantity_toast),
                            Toast.LENGTH_SHORT).show();
                    throw new IllegalArgumentException
                            (context.getString(R.string.invalid_quantity));
                }
            }

            //Valid supplier values CAN be null, must not exceed 25 chars
            if(values.containsKey(ItemEntry.COLUMN_SUPPLIER)){
                String supplier = values.getAsString(ItemEntry.COLUMN_SUPPLIER);
                if(supplier.length() > 25){
                    Toast.makeText(context, context.getString(R.string.invalid_supplier_toast),
                            Toast.LENGTH_SHORT).show();
                    throw new IllegalArgumentException
                            (context.getString(R.string.invalid_supplier));
                }
            }

            //Valid price values must be greater than 0 or equal to
            // -0.001 (number that represents not for sale)
            if(values.containsKey(ItemEntry.COLUMN_PRICE)){
                //Price cannot be null. This field is required
                if(values.getAsDouble(ItemEntry.COLUMN_PRICE) == null){
                    Toast.makeText(context, context.getString(R.string.price_empty),
                            Toast.LENGTH_SHORT).show();
                    throw new NullPointerException(LOG_TAG);
                }
                double price = values.getAsDouble(ItemEntry.COLUMN_PRICE);
                if(price < 0 && price != ItemEntry.NOT_FOR_SALE ||
                        price >= ItemEntry.MAX_PRICE){
                    Toast.makeText(context, context.getString(R.string.invalid_price_toast),
                            Toast.LENGTH_SHORT).show();
                    throw new IllegalArgumentException
                            (context.getString(R.string.invalid_price));
                }
            }

            if(values.containsKey(ItemEntry.COLUMN_IMAGE)) {
                if (values.getAsByteArray(ItemEntry.COLUMN_IMAGE) == null) {
                    Toast.makeText(context, context.getString(R.string.no_image),
                            Toast.LENGTH_SHORT).show();
                    throw new NullPointerException(LOG_TAG);
                }
            }

            //All data quality checks passed without throwing an exception
            return GOOD_DATA;

        }catch(IllegalArgumentException e){
            //if an I.A.E is thrown then the ContentValue pairs contain
            //data not suitable for the snapandsave.db
            return BAD_DATA;
        }catch(NullPointerException f){
            return BAD_DATA;
        }
    }
}