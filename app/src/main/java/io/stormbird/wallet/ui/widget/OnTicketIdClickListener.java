package io.stormbird.wallet.ui.widget;

import android.view.View;

import io.stormbird.token.entity.TicketRange;

/**
 * Created by James on 10/02/2018.
 */

public interface OnTicketIdClickListener {
    void onTicketIdClick (View view, TicketRange range);
}
