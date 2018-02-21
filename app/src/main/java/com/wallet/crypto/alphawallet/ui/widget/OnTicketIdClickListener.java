package com.wallet.crypto.alphawallet.ui.widget;

import android.view.View;

import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;

/**
 * Created by James on 10/02/2018.
 */

public interface OnTicketIdClickListener {
    void onTicketIdClick (View view, TicketRange range);
}
