package com.alphawallet.app.router;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.ui.AddressBookActivity;
import com.alphawallet.app.ui.BaseActivity;

public class AddressBookRouter {

    public void open(Context context) {
        context.startActivity(new Intent(context, AddressBookActivity.class));
    }

    public void openForContactSelection(Context context, int requestCode) {
        Intent i = new Intent(context, AddressBookActivity.class);
        ((BaseActivity) context).startActivityForResult(i, requestCode);
    }
}
