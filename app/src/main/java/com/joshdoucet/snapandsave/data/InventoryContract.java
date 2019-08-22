package com.joshdoucet.snapandsave.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 *  - The InventoryContract provides constants related to the inventory database
 *      such as table names, column names, content uri etc.
 *      This class helps to reduce errors when performing operations on the DB.
 */

public final class InventoryContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private InventoryContract() {}

    //Name of the content authority used to create a URI
    public final static String CONTENT_AUTHORITY = "com.joshdoucet.snapandsave";

    //Base URI plus content authority
    public final static Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    //Path name for the items table
    public static final String PATH_ITEMS = "items";

    /**
     * ITEMS TABLE
     * Inner class that defines constant values for the items database table.
     * Each entry in the table represents a an item and its quantity.
     */
    public static final class ItemEntry implements BaseColumns{
        //TABLE NAME
        public static final String TABLE_NAME = "items";

        //Content URI for the items table
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ITEMS);

        //The MIME type of the CONTENT_URI for a list of items.
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        //The MIME type of the CONTENT_URI for a single ITEM.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        /**
         * Column names for the Items table
         * SQL types are commented above each variable
         */

        //Unique ID for the item entry
        //COLUMN Type - INTEGER PRIMARY KEY AUTOINCREMENT
        public final static String _ID = BaseColumns._ID;

        //Name of the item in inventory
        //COLUMN Type - TEXT MOT NULL
        public final static String COLUMN_NAME = "name";

        //Quantity of the item or, number of item on hand
        //COLUMN Type -  INTEGER NOT NULL DEFAULT 0
        public final static String COLUMN_QUANTITY = "quantity";

        //Supplier of the item - who provided the item
        //COLUMN Type - TEXT
        public final static String COLUMN_SUPPLIER = "supplier";

        //Price of the item - all pricing MUST be stored in United States Dollars for data integrity
        //COLUMN Type - REAL NOT NULL DEFAULT
        public final static String COLUMN_PRICE = "price_US_$";

        //Picture for an item
        //COLUMN Type - BLOB
        public final static String COLUMN_IMAGE = "image";

        /**
         * Other constant values
         */

        //Price that represents an item is not for sale
        public final static double NOT_FOR_SALE = .14619;
        public final static double FREE = 0.00;
        //max values
        public final static int MAX_QUANTITY = 9999999;
        public final static double MAX_PRICE = 9999999.99;
    }

}
