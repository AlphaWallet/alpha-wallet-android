package com.alphawallet.app.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.ui.AddEditAddressActivity;

public class AddEditAddressRouter {

    public void open(Context context, int requestCode) {
        ((Activity) context).startActivityForResult(new Intent(context, AddEditAddressActivity.class), requestCode);
    }

    /**
     * Use to auto fill wallet address when called from Transaction Detail screen.
     * @param context
     * @param walletAddress Wallet Address to auto fill
     */
    public void open(Context context, int requestCode, String walletAddress) {
        Intent intent = new Intent(context, AddEditAddressActivity.class);
        intent.putExtra(C.EXTRA_CONTACT_WALLET_ADDRESS, walletAddress);
        ((Activity) context).startActivityForResult(intent, requestCode);
    }

    /**
     * Use to pass a contact for editing.
     * @param context
     * @param requestCode
     * @param contact
     */
    public void open(Context context, int requestCode, AddressBookContact contact) {
        Intent intent = new Intent(context, AddEditAddressActivity.class);
        intent.putExtra(C.EXTRA_CONTACT, contact);
        ((Activity) context).startActivityForResult(intent, requestCode);
    }
}
