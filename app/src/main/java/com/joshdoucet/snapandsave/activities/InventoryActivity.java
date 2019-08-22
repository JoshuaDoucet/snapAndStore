/*
TODO - Familiarize with the app
TODO - Add a +1 button
TODO - Change all references of "Sell" to "Remove"
TODO -add "not for sale" and "free" option to items
TODO - Change " Track a Sale" ti "Remove Inventory"
TODO - Change "Recieve Shipment" to "Add Inventory"
TODO - Alter sample item. i.e. Take unique photos, No trademarks
TODO - add ability to select photo from device
TODO - Export/Import database??
TODO - Shared preferences?
        TODO - Prefered curency + curency conversion
        TODO - add option to alter email message
TODO - Add a better caption for deleting an item
TODO - Alter default email message
TODO - Change "Take Picture" to "Edit Photo"
TODO - Enable landscape orientation
TODO - Alter home screen message when database is empty
 */
package com.joshdoucet.snapandsave.activities;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.joshdoucet.snapandsave.R;
import com.joshdoucet.snapandsave.data.InventoryContract.ItemEntry;
import com.joshdoucet.snapandsave.data.InventoryCursorAdapter;

import java.text.DecimalFormat;

/**
 *  OVERVIEW InventoryActivity
 *
 * - This activity will display a list of a stores inventory data enclosed in a ListView
 *
 * - The ListView will be populated via the InventoryCursorAdapter.
 *      and the CursorAdapter will gather all necessary info about the inventory
 *      from a cursor prov* - The InventoryContract provides constants related to the inventory database
 *      such as table names, column names, content uri etc.
 *      This class helps to reduce errors when performing operations on the DB.ided be the inventory database helper classes
 * - The InventoryDBHelper class will create and access a database and provide
 *      helper objects that can be used as a readable or writable database for
 *      C.R.U.D. operations
 * - The InventoryProvider class works as a ContentProvider that directly performs operations
 *      on the database. This is the only place the DB should be directly interacted with.
 *      The provider acts as an abstraction layer between the UI and the data. The UI code should
 *      interact with the ContentResolver which will then work with the InventoryProvider.
 *      All data integrity checks are done within this class to ensure bad data is not entered
 *      into the database
 */

public class InventoryActivity extends AppCompatActivity
                        implements LoaderManager.LoaderCallbacks<Cursor>{

    //Adapter used to populate the ListView in the activity
    InventoryCursorAdapter mInventoryCursorAdapter;

    //Unique ID for the Loader that fetches data from the snapandsave.db
    private static final int ITEM_LOADER_ID = 4;

    public TextView mTotalValueView;

    public static double totalInventoryValue;

    //if loader has started once
    boolean hasStartedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        //FAB that will open the Inventory "EditorActivity"
        FloatingActionButton addItemButton = (FloatingActionButton) findViewById(R.id.add_fab);

        //Set click listener on the "add_fab"
        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Create and execute intent to view the "EditorActivity"
                Intent openEditorIntent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(openEditorIntent);
            }
        });

        mTotalValueView = (TextView) findViewById(R.id.value_total);

        //Initialize CursorAdapter that will be populated with data
        mInventoryCursorAdapter = new InventoryCursorAdapter(this, null);

        //Initialize list view that will be populated with items in the store inventory
        ListView inventoryListView = (ListView) findViewById(R.id.inventory_list_view);
        //Set empty view for when the list view is empty
        inventoryListView.setEmptyView(findViewById(R.id.empty_view));
        //Set adapter for the list view
        inventoryListView.setAdapter(mInventoryCursorAdapter);

        //On item click...
        inventoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Create intent that will open the Editor Activity
                Intent openEditorIntent = new Intent(InventoryActivity.this, EditorActivity.class);
                //Append the clicked ID to the CONTENT_URI for the items table
                Uri uriWithId = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id);
                openEditorIntent.setData(uriWithId);
                //Start Activity with added uri data
                startActivity(openEditorIntent);
            }
        });

        //initialize LoaderCallBacks for a CursorLoader that will read info about items
        //from the Store Inventory database via the Inventory Content Provider
        getLoaderManager().initLoader(ITEM_LOADER_ID, null, this);

    }

    /**
     * Create a CursorLoader that will query item name price and quantity
     * @param id of the Inventory Loader constant
     * @param args null
     * @return CursorLoader
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_NAME,
                ItemEntry.COLUMN_PRICE,
                ItemEntry.COLUMN_QUANTITY};

        //Prevents SQL injection
        String selection = null;
        String[] selectionArgs = null;

        // Perform a query on the item table via the ContentResolver
        // and the InventoryProvider using a background CursorLoader
        return new CursorLoader(
                this,                   //Current Activity
                ItemEntry.CONTENT_URI,   //Content URI for items table
                projection,             //Columns to select
                selection,              //Selection, the WHERE SQL
                selectionArgs,           //SelectionArgs WHERE values
                null);                  //Sort Order
    }




    /**
     * Swap in new / altered cursor info to the cursor adapter
     * Calculate new total value of the store inventory
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //Update the cursor with new updated data
        mInventoryCursorAdapter.swapCursor(data);

        //If this is the first time the loader is running
        if(!hasStartedOnce) {
            hasStartedOnce = true;
            totalInventoryValue = 0;
            while (data.moveToNext()) {
                int quantity = data.getInt(data.getColumnIndex(ItemEntry.COLUMN_QUANTITY));
                double price = data.getDouble(data.getColumnIndex(ItemEntry.COLUMN_PRICE));
                if(price != ItemEntry.FREE && price != ItemEntry.NOT_FOR_SALE) {
                    InventoryActivity.totalInventoryValue += (quantity * price);
                }
            }
        }

        //Display formatted total inventory value
        formatTotal();
    }

    /**
     * Empty or reset cursor adapter
     * @param loader ItemCursorLoader
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //Called when data in the adapter needs to be deleted
        mInventoryCursorAdapter.swapCursor(null);
        //Display formatted total inventory value
        formatTotal();
    }

    /**
     * Inflate the options menu for the InventoryActivity
     * @param menu for the activity
     * @return true if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_delete_database:
                showDeleteConfirmationDialog();
                break;
            default:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Dialog to be shown when a user attempts to delete a items entry
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positvie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteAllItems();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Delete all items from the Items Inventory table
     */
    public void deleteAllItems(){
        //Delete all rows in the Items table
        int rowsDeleted = getContentResolver().delete(ItemEntry.CONTENT_URI,
                null, null);

        if(rowsDeleted > 0){
            //Prompt User of Successful Deletion
            Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                    Toast.LENGTH_SHORT).show();
            totalInventoryValue = 0;
            formatTotal();
        }else{
            //Prompt User of failed deletion
            Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        formatTotal();
    }

    private void formatTotal(){
        //Display formatted total inventory value
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        String totalValue = getString(R.string.currency_symbol) +
                formatter.format(totalInventoryValue);
        mTotalValueView.setText(totalValue);
    }
}
