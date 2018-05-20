package io.stormbird.wallet.ui.widget;

import android.view.View;

import io.stormbird.token.entity.MagicLinkData;

/**
 * Created by James on 21/02/2018.
 */

public interface OnSalesOrderClickListener {
    void onOrderClick (View view, MagicLinkData range);
}
