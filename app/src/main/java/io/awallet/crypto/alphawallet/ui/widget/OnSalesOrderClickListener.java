package io.awallet.crypto.alphawallet.ui.widget;

import android.view.View;

import io.awallet.crypto.alphawallet.entity.SalesOrder;

/**
 * Created by James on 21/02/2018.
 */

public interface OnSalesOrderClickListener {
    void onOrderClick (View view, SalesOrder range);
}
