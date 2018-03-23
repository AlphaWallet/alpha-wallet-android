package io.awallet.crypto.alphawallet.ui.widget;

import android.view.View;

import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

/**
 * Created by James on 10/02/2018.
 */

public interface OnTicketIdClickListener {
    void onTicketIdClick (View view, TicketRange range);
}
