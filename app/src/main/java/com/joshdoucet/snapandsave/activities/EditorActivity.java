package com.joshdoucet.snapandsave.activities;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.joshdoucet.snapandsave.R;
import com.joshdoucet.snapandsave.data.InventoryContract.ItemEntry;

import java.io.ByteArrayOutputStream;

import static android.view.View.GONE;
import static java.lang.Double.parseDouble;

/**
 *  - The EditorActivity has 2 mode. One is for "Adding a NEW item" to the database of inventory
 *          and, the other mode is for "Editing an Item". This activity utilizes many of the same
 *          database helper classes that the InventoryActivity uses.
 *          1. - Add an Item - displays a simple layout of editable fields
 *              that the user can interact with to populate a new entry for the database.
 *              The user may also add a photo of the item by taking a picture. This mode calls
 *              on ContentResolver to insert info to the DB
 *          2 - Edit an Item - This mode is activated when the user clicks on a specific
 *              item entry in the InventoryActivity. The editor than gets pre-populated with
 *              values from the SQLite database about that specific item.
 *              The user also has all the same functionality that are available in "Add an Item";
 *              however, when the data is saved the ContentResolver calls the update
 *              method instead of insert
 */

public class EditorActivity extends AppCompatActivity
                            implements LoaderManager.LoaderCallbacks<Cursor>{

    //Unique ID for the Item Loader that may be used to populate data fields
    private static final int ITEM_INFO_LOADER_ID = 5;

    //Code for receiving image from an intent
    static final int REQUEST_IMAGE_CAPTURE = 1;

    //Code for requesting CAMERA permission
    static final int PERMISSIONS_CODE_REQUEST_CAMERA = 3;

    //if device has camera
    private boolean mHasCamera;

    //Variable to store URI for a specific item passed by intent
    //Used if data for a specific item needs to be queried to populate the
    //editor activity
    private Uri mReceivedItemUri = null;

    //Variable used to determine if the editor has changed
    //if item has changed a dialog will prompt the user if they want
    //to exit and discard their changes
    private boolean mItemHasChanged = false;

    //Views that the visibility will be toggled
    private TextView mIdValueText;

    //Editable views that will be used to display and input
    // database data for a specific item
    private EditText mItemTitleEditText;
    private EditText mItemPriceEditText;
    private EditText mItemQuantityEditText;
    private TextView mItemQuantityTextView;
    private EditText mItemSupplierEditText;
    private ImageView mItemImageView;

    //Variables need to make proper adjustments to the inventory total value
    private double priceBeforeChange;
    private int quantityBeforeChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Collect information about the intent that opened this activity
        Intent receivedIntent = getIntent();
        mReceivedItemUri = receivedIntent.getData();

        mHasCamera = hasCameraPermissions(this);

        //Find all views that need to be toggled between activity modes
        // ("add item" or "edit item"). The visibility of these views will be toggled
        TextView mIdDescriptionText = (TextView) findViewById(R.id.id_desc_view);
        mIdValueText = (TextView) findViewById(R.id.id_value_view);

        // Find all relevant views that we will need to read user input from
        mItemTitleEditText = (EditText) findViewById(R.id.edit_title);
        mItemPriceEditText = (EditText) findViewById(R.id.edit_price);
        mItemSupplierEditText = (EditText) findViewById(R.id.edit_supplier);
        mItemQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mItemQuantityTextView = (TextView) findViewById(R.id.quantity_value);
        mItemImageView = (ImageView) findViewById(R.id.image_view);

        //Set touch listeners for the above editable views
        mItemTitleEditText.setOnTouchListener(mTouchListener);
        mItemPriceEditText.setOnTouchListener(mTouchListener);
        mItemQuantityEditText.setOnTouchListener(mTouchListener);
        mItemSupplierEditText.setOnTouchListener(mTouchListener);

        //Find all buttons
        Button mButtonSale = (Button) findViewById(R.id.button_sale);
        Button mButtonReceive = (Button) findViewById(R.id.button_receive);
        Button mButtonOrder = (Button) findViewById(R.id.button_order);

        //Set action bar title
        //Remove unnecessary views when applicable
        if(mReceivedItemUri == null){
            //No item URI was sent to EditorActivity. The editor will be used
            //to enter info for a NEW item
            setTitle(getString(R.string.title_add_item));

            //No ID views are needed because the new item doesn't have one yet
            mIdDescriptionText.setVisibility(GONE);
            mIdValueText.setVisibility(GONE);

            //Unneeded views for this mode
            mItemQuantityTextView.setVisibility(GONE);
            mItemImageView.setVisibility(GONE);
            mButtonOrder.setVisibility(GONE);
            mButtonReceive.setVisibility(GONE);
            mButtonSale.setVisibility(GONE);

        }else{
            //Otherwise, a URI was sent to the activity and will be used
            //to EDIT an existing item
            setTitle(getString(R.string.tittle_edit_item));

            //View was replaced with another one for this mode
            mItemQuantityEditText.setVisibility(GONE);

            //initialize loader that will be used to populate edit fields
            getLoaderManager().initLoader(ITEM_INFO_LOADER_ID, null, this);
        }
    }

    /**
     * On touch listener that will be used to tell if the editor fields have
     * changed since the activity started. This is useful for when the up
     * or back button is hit so the app can prompt the user if they would like to discard changes
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    /**
     * Creates a dialog message that prompts the user if they would
     * like to continue editing their changes to the Pet entry or
     * discard changes and return to the CatalogActivity
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create AlertDialog.Builder set the message, and click listeners
        // for the + and - buttons on the message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the item entry.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Create dialog when back button is pressed
     */
    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        //InventoryActivity.totalInventoryValue = 0;
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     *The following 3 methods are used for the actionBar options menu functionality
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mReceivedItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.menu_delete_row);
            menuItem.setVisible(false);
        } else{
            // if this is an existing item, hide the "Insert Sample Data" menu item
            MenuItem menuItem = menu.findItem(R.id.menu_insert_sample);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_delete_row:
                //Confirm that the user wants to delete item entry
                //if positive, delete item and finish activity
                //Otherwise, dismiss dialog and resume activity
                showDeleteConfirmationDialog();
                break;
            case R.id.menu_insert_sample:
                insertSampleData();
                finish();
                break;
            case R.id.menu_save_row:
                // Save item to database if successful insertItem() returns true
                if(saveItem()){
                    // Exit activity if item inserted
                    finish();
                }
                break;
            case android.R.id.home:
                // Respond to a click on the "Up" arrow button in the app bar
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the CatalogActivity
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return true;
    }

    /**
     * Insert Sample Data to the snapandsave.db file
     * Used for quick debugging purposes
     */
    public void insertSampleData(){
        int quantity = 2;
        double price = 17.99;

        Bitmap boots = BitmapFactory.decodeResource(getResources(), R.drawable.cast);
        byte[] blob = bitToByteArray(boots);

        ContentValues sampleVals = new ContentValues();
        sampleVals.put(ItemEntry.COLUMN_NAME, "Chromecast - Red");
        sampleVals.put(ItemEntry.COLUMN_SUPPLIER, "Google");
        sampleVals.put(ItemEntry.COLUMN_QUANTITY, quantity);
        sampleVals.put(ItemEntry.COLUMN_PRICE, price);
        sampleVals.put(ItemEntry.COLUMN_IMAGE, blob);

        Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, sampleVals);

        // Show a toast message depending on whether or not the insertion was successful
        if (newUri == null) {
            // If the row ID is -1, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.item_error), Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the insertion was successful and we can display a toast with the row ID.
            Toast.makeText(this, getString(R.string.item_saves),
                    Toast.LENGTH_SHORT).show();

            if(price != ItemEntry.NOT_FOR_SALE && price != ItemEntry.FREE) {
                InventoryActivity.totalInventoryValue += (price * quantity);
            }
        }
    }

    /**
     * Get user input from editor and save new item into database.
     * return true if item successfully entered
     */
    public boolean saveItem(){
        //Collect user input from editable views
        String itemTitle = mItemTitleEditText.getText().toString();
        String itemSupplier = mItemSupplierEditText.getText().toString();
        String itemQuantity = mItemQuantityEditText.getText().toString();
        String itemPrice = mItemPriceEditText.getText().toString();

        double price;
        int quantity;
        try {
            price = Double.parseDouble(itemPrice);
            quantity = Integer.parseInt(itemQuantity);
        }catch(NumberFormatException e){
            price = 0;
            quantity = 0;
        }

        //Values pairs that will be put into the database
        ContentValues itemVals = new ContentValues();
        itemVals.put(ItemEntry.COLUMN_NAME, itemTitle);
        itemVals.put(ItemEntry.COLUMN_SUPPLIER, itemSupplier);
        itemVals.put(ItemEntry.COLUMN_QUANTITY, itemQuantity);
        itemVals.put(ItemEntry.COLUMN_PRICE, itemPrice);

        //Convert image to type compatible with database
        mItemImageView.buildDrawingCache();
        Bitmap itemImage = mItemImageView.getDrawingCache();
        byte[] imageByte;

        //if image is not null add to ContentVals
        if(itemImage != null) {
            imageByte = bitToByteArray(itemImage);
        }else{
            imageByte = null;
        }

        itemVals.put(ItemEntry.COLUMN_IMAGE, imageByte);

        //If a new row is being added to the database table...
        if(mReceivedItemUri == null){
            Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, itemVals);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the row ID is -1, then there was an error with insertion.
                return false;
            } else {
                // Otherwise, the insertion was successful and we can display a toast with the row ID.
                Toast.makeText(this, getString(R.string.item_saves) + ContentUris.parseId(newUri),
                        Toast.LENGTH_SHORT).show();
                //Update inventory total
                if(price != ItemEntry.NOT_FOR_SALE && price != ItemEntry.FREE) {
                    InventoryActivity.totalInventoryValue += (price * quantity);
                }
                return true;
            }
        }else{
            //Otherwise an existing item entry is being updated
            //Calls ContentResolver to grab InentoryProvider which will update an existing
            //row in the inventory database
            int rowsUpdated = getContentResolver().update(mReceivedItemUri, itemVals, null, null);

            // Show a toast message depending on whether or not the update was successful
            if(rowsUpdated < 1){
                //No rows were updated, error updated item
                return false;
            }else{
                Toast.makeText(this, getString(R.string.item_saves), Toast.LENGTH_SHORT).show();

                //update inventory totals, subtract initial value and then add new item value total
                if(price != ItemEntry.NOT_FOR_SALE && price != ItemEntry.FREE) {
                    InventoryActivity.totalInventoryValue -= (priceBeforeChange * quantityBeforeChange);
                    InventoryActivity.totalInventoryValue += (price * quantity);
                }
                return true;
            }
        }
    }

    /**
     * Delete an item from the Items Inventory table
     */
    public void deleteItem(){
        if(mReceivedItemUri != null) {
            //Delete selected row in the Items table
            int rowsDeleted = getContentResolver().delete(mReceivedItemUri,
                    null, null);

            if (rowsDeleted > 0) {
                //Prompt User of Successful Deletion
                Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                        Toast.LENGTH_SHORT).show();
                int quantity = Integer.parseInt(mItemQuantityEditText.getText().toString());
                double price = Double.parseDouble(mItemPriceEditText.getText().toString());
                //update total inventory value
                if(price != ItemEntry.NOT_FOR_SALE && price != ItemEntry.FREE) {
                    InventoryActivity.totalInventoryValue -= (price * quantity);
                }
            } else {
                //Prompt User of failed deletion
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Prompt user for an Integer that will be used to update (Track a Sale) and subtract from the current quantity.
     * Update quantity if needed and dismiss dialog Method called via onClick XML attribute
     * @param view that was clicked on.
     */
    public void trackSale(View view){
        AlertDialog.Builder alertBuild = new AlertDialog.Builder(this);
        alertBuild.setMessage(R.string.message_sale);

        //Logic to display EditText
        LayoutInflater inflater = EditorActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.alert_edit_layout, null);
        alertBuild.setView(inputView);

        final EditText dialogInput = (EditText) inputView.findViewById(R.id.dialog_input);

        alertBuild.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                //Price info used to update inventory totals
                String priceText = mItemPriceEditText.getText().toString();
                double price = parseDouble(priceText);
                //Collect current quantity and user input quantity
                String quantityText = mItemQuantityTextView.getText().toString();
                String userUpdateInput = dialogInput.getText().toString();
                int currentQuantity = Integer.parseInt(quantityText);
                int quantityInput;
                try {
                    quantityInput = Integer.parseInt(userUpdateInput);
                }catch(NumberFormatException e){
                    quantityInput = 0;
                }
                //Since we cant have less than 0 of an item...
                if(currentQuantity - quantityInput < 0){
                    dialog.dismiss();
                    Toast.makeText(EditorActivity.this, getString(R.string.quantity_less_0),
                            Toast.LENGTH_LONG).show();
                }else if(quantityInput >= ItemEntry.MAX_QUANTITY) {
                    dialog.dismiss();
                    Toast.makeText(EditorActivity.this, getString(R.string.invalid_quantity),
                            Toast.LENGTH_LONG).show();
                }else {
                    //Update quantity in database to reflect sale
                    currentQuantity = currentQuantity - quantityInput;
                    ContentValues val = new ContentValues();
                    val.put(ItemEntry.COLUMN_QUANTITY, currentQuantity);
                    int rowsRpdated = getContentResolver().update(mReceivedItemUri, val,
                            null, null);
                    if(rowsRpdated > 0){
                        //Update inventory total values
                        if(price != ItemEntry.NOT_FOR_SALE && price != ItemEntry.FREE) {
                            InventoryActivity.totalInventoryValue -= (price * quantityInput);
                        }
                    }
                }
            }
        });
        alertBuild.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(dialog != null){
                    dialog.dismiss();
                }
            }
        });
        AlertDialog updateAlert = alertBuild.create();
        updateAlert.show();
    }

    /**
     * Prompt user for an Integer that will be used to update (Receive a Shipment) and add to the current quantity.
     * Update quantity if needed and dismiss dialog Method called via onClick XML attribute
     * @param view that was clicked on.
     */
    public void receiveOrder(View view){
        AlertDialog.Builder alertBuild = new AlertDialog.Builder(this);
        alertBuild.setMessage(R.string.message_receive);

        //Logic to display EditText
        LayoutInflater inflater = EditorActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.alert_edit_layout, null);
        alertBuild.setView(inputView);

        final EditText dialogInput = (EditText) inputView.findViewById(R.id.dialog_input);

        alertBuild.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                //Collect price for calculating total inventory value
                String itemPrice = mItemPriceEditText.getText().toString();
                double price = parseDouble(itemPrice);

                //Collect current quantity and user input quantity
                String quantityText = mItemQuantityTextView.getText().toString();
                String userUpdateInput = dialogInput.getText().toString();
                int currentQuantity = Integer.parseInt(quantityText);
                int quantityInput;
                try {
                    quantityInput = Integer.parseInt(userUpdateInput);
                }catch(NumberFormatException e){
                    quantityInput = 0;
                }

                if(currentQuantity + quantityInput >= ItemEntry.MAX_QUANTITY ||
                        quantityInput >= ItemEntry.MAX_QUANTITY){
                    dialog.dismiss();
                    Toast.makeText(EditorActivity.this, getString(R.string.invalid_quantity),
                            Toast.LENGTH_LONG).show();
                }


                //Update quantity in database to reflect received product
                currentQuantity = currentQuantity + quantityInput;
                ContentValues val = new ContentValues();
                val.put(ItemEntry.COLUMN_QUANTITY, currentQuantity);
                int rowsUpdated = getContentResolver().update(mReceivedItemUri, val,
                        null, null);
                if(rowsUpdated > 0){
                    //update inventory total vallues
                    if(price != ItemEntry.NOT_FOR_SALE && price != ItemEntry.FREE) {
                        InventoryActivity.totalInventoryValue += (price * quantityInput);
                    }
                }
            }
        });
        alertBuild.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(dialog != null){
                    dialog.dismiss();
                }
            }
        });
        AlertDialog updateAlert = alertBuild.create();
        updateAlert.show();
    }

    /**
     * Used to generate a pre populated email intent to order more product from the supplier
     * @param view, clicked view. Method called via onClick XML attribute
     */
    public void orderItem(View view){
        String itemTitle = mItemTitleEditText.getText().toString();
        String supplier = mItemSupplierEditText.getText().toString();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject) + itemTitle);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_header) + supplier
                                + getString(R.string.email_body));

        startActivity(Intent.createChooser(intent, getString(R.string.open_app_message)));
    }

    /**
     *Checks if device has a camera and if permissions are granted
     */
    public boolean hasCameraPermissions(Context context){
        PackageManager pm = context.getPackageManager();
        boolean hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        int hasPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA);
        if (hasCamera && hasPermission == PackageManager.PERMISSION_GRANTED) {
            //return true if permission is granted
            return true;
        }else{
            //Request permissions for camera
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_CODE_REQUEST_CAMERA);
            //if permission was granted
            return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Used to take a picture through a camera application and return for results
     * @param view button clicked on to execute function
     */
    public void captureImage(View view){
        //check permissions
        mHasCamera = hasCameraPermissions(this);
        if(mHasCamera) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }else{
            Toast.makeText(this, getString(R.string.camera_unavailable),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     *On camera intent result, save the bitmap image in an ImageView
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap mTempBitmap = (Bitmap) extras.get("data");
            mItemImageView.setImageBitmap(mTempBitmap);
            mItemImageView.setVisibility(View.VISIBLE);
        }
    }

    /**
     *Use the 2 functions bellow to convert between Bitmap and Byte[] image types
     */
    public static byte[] bitToByteArray(Bitmap bmp){
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 0 /* Ignored for PNGs */, blob);
        return blob.toByteArray();
    }

    public static Bitmap byteArrayToBitmap(byte[] image){
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    /**
     * Dialog to be shown when a user attempts to delete an item entry
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, remove item row from database.
                deleteItem();
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked "cancel" button, dismiss the dialog
                // and keep editing the item entry.
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
     * Loader used to fetch cursor data from items database to populate Views that are on screen
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the items table
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_QUANTITY,
                ItemEntry.COLUMN_PRICE,
                ItemEntry.COLUMN_SUPPLIER,
                ItemEntry.COLUMN_NAME,
                ItemEntry.COLUMN_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mReceivedItemUri,        // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    /**
     * Populate on screen views with data from the cursor that was queried in Loader on create
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)

        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int idColumnIndex = cursor.getColumnIndex(ItemEntry._ID);
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_QUANTITY);
            int supplierColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_IMAGE);

            // Extract out the value from the Cursor for the given column index
            int id = cursor.getInt(idColumnIndex);
            String name = cursor.getString(nameColumnIndex);
            priceBeforeChange = cursor.getDouble(priceColumnIndex);
            quantityBeforeChange = cursor.getInt(quantityColumnIndex);
            String supplier= cursor.getString(supplierColumnIndex);

            // Update the views on the screen with the values from the database
            mItemTitleEditText.setText(name);
            mItemSupplierEditText.setText(supplier);
            mItemQuantityTextView.setText(Integer.toString(quantityBeforeChange));
            mItemQuantityEditText.setText(Integer.toString(quantityBeforeChange));
            mItemPriceEditText.setText(Double.toString(priceBeforeChange));
            mIdValueText.setText(Integer.toString(id));

            //if an image exists in this row entry
            if(cursor.getBlob(imageColumnIndex) != null){
                byte[] image = cursor.getBlob(imageColumnIndex);
                Bitmap imageBit = byteArrayToBitmap(image);
                mItemImageView.setImageBitmap(imageBit);
            }else{
                mItemImageView.setVisibility(GONE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader.reset();
    }
}
