package com.wallet.crypto.alphawallet.ui.widget.adapter;

import android.media.session.MediaSession;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenClickListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.SortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TokenIdSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TokenSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TotalBalanceSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TicketHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;

import java.math.BigDecimal;

/**
 * Created by James on 9/02/2018.
 */

public class TicketAdapter extends TokensAdapter {

    private OnTicketIdClickListener onTicketIdClickListener;
    public TicketAdapter(OnTicketIdClickListener onTicketIdClickListener, Ticket t) {
        super();
        this.onTicketIdClickListener = onTicketIdClickListener;
        setTicket(t);
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketHolder.VIEW_TYPE: {
                TicketHolder tokenHolder = new TicketHolder(R.layout.item_ticket, parent);
                tokenHolder.setOnTokenClickListener(onTicketIdClickListener);
                holder = tokenHolder;
            } break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            }
        }

        return holder;
    }

    public void setTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        if (total != null)
        {
            items.add(total);
        }
        for (int i = 0; i < t.balanceArray.size(); i++) {
            //initially don't sort
            int tokenId = t.balanceArray.get(i);
            if (tokenId != 0)
            {
                items.add(new TokenIdSortedItem(tokenId, 10 + i));
            }
        }
        items.endBatchedUpdates();
    }
}

