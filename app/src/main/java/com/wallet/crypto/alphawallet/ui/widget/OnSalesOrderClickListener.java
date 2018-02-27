package com.wallet.crypto.alphawallet.ui.widget;

import android.view.View;

import com.wallet.crypto.alphawallet.entity.SalesOrder;

/**
 * Created by James on 21/02/2018.
 */

public interface OnSalesOrderClickListener {
    void onOrderClick (View view, SalesOrder range);
}
