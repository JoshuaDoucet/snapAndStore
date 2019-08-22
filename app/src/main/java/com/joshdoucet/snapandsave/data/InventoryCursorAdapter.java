package com.joshdoucet.snapandsave.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.joshdoucet.snapandsave.R;
import com.joshdoucet.snapandsave.activities.InventoryActivity;
import com.joshdoucet.snapandsave.data.InventoryContract.ItemEntry;

import java.util.Locale;

/** - InventoryCursorAdapter
 *      Populate a layout with values from the Items database while recycling the views for
 *      better memory management when creating many views.
 */

public class InventoryCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new InventoryCursorAdapter
     *
     * @param context The context
     * @param c The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c){
        super(context, c, 0 /*flags*/ );
    }

    //Inflate a new inventory_item_layout if there are none to recycle
    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.inventory_list_item, parent, false);
    }

    /**
     * Gather data from cursor and fill the inventory list item view with said data
     * @param view R.layout.inventory_list_item
     * @param context activity
     * @param c cursor filled with database rows from the items table
     */
    @Override
    public void bindView(View view, final Context context, final Cursor c) {

        //Views to populate with cursor data
        final TextView nameView = (TextView) view.findViewById(R.id.name_text_view);
        TextView quantityView = (TextView) view.findViewById(R.id.quantity_text_view);
        TextView priceView = (TextView) view.findViewById(R.id.price_text_view);
        TextView minusOneButton = (TextView) view.findViewById(R.id.minus_button);

        //Set name, quantity, and price views with data from cursor
        nameView.setText(c.getString(c.getColumnIndex(ItemEntry.COLUMN_NAME)));

        quantityView.setText(c.getString(c.getColumnIndex(ItemEntry.COLUMN_QUANTITY)));
        int quantity = c.getInt(c.getColumnIndex(ItemEntry.COLUMN_QUANTITY));

        final double price = c.getDouble(c.getColumnIndex(ItemEntry.COLUMN_PRICE));
        // .14619 is the constant value for, NOT FOR SALE
        if(price == ItemEntry.NOT_FOR_SALE){
            priceView.setText(context.getString(R.string.mot_for_sale));
        }else if(price == ItemEntry.FREE){
            priceView.setText(context.getString(R.string.free));
        } else{
            //Set price view round decimal 2 digits, add currency symbol
            priceView.setText(context.getString(R.string.currency_symbol) +
                    String.format(Locale.ENGLISH, "%.2f", price));
        }


        //When the "-1" button is clicked the user is prompted with a dialog if they
        //would like to reduce the quantity of the clicked item by one
        final int position = c.getPosition();
        minusOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                final String itemTittle = nameView.getText().toString();
                builder.setMessage(itemTittle + context.getString(R.string.sell_one));
                builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked the "Sell" button, reduce quantity by 1 update DB.
                        //Gather data needed to decrease quantity by 1
                        c.moveToPosition(position);
                        int itemIdColumnIndex = c.getColumnIndex(ItemEntry._ID);
                        int itemId = c.getInt(itemIdColumnIndex);
                        int itemQuantity = c.getInt(c.getColumnIndex(ItemEntry.COLUMN_QUANTITY));
                        double itemPrice = c.getDouble(c.getColumnIndex(ItemEntry.COLUMN_PRICE));
                        Uri itemUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, itemId);

                        //Subtract 1 from item quantity and update database row
                        ContentValues quantityVal = new ContentValues();
                        quantityVal.put(ItemEntry.COLUMN_QUANTITY, itemQuantity - 1);
                        int rowsUpdated = context.getContentResolver().update(itemUri, quantityVal,
                                null, null);

                        //Subtract value of one item from inventory total
                        if(rowsUpdated == 1 && price != ItemEntry.NOT_FOR_SALE && price != ItemEntry.FREE) {
                            InventoryActivity.totalInventoryValue -= price;
                        }
                        dialog.dismiss();
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

        });
    }
}
