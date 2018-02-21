package com.wallet.crypto.alphawallet.ui.widget.adapter;

import android.media.session.MediaSession;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenClickListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.SortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.ui.widget.entity.TokenBalanceSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TokenIdSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TokenSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TotalBalanceSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TicketHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TokenDescriptionHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Created by James on 9/02/2018.
 */

public class TicketAdapter extends TokensAdapter {

    protected OnTicketIdClickListener onTicketIdClickListener;

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
            } break;
            case TokenDescriptionHolder.VIEW_TYPE: {
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent);
            } break;
        }

        return holder;
    }

    public void setTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(t));

        TicketRange currentRange = null;
        int currentSeat = -1;
        char currentZone = '-';
        int i;
        //first sort the balance array
        List<Integer> sortedList = t.balanceArray.subList(0, t.balanceArray.size());
        Collections.sort(sortedList);
        for (i = 0; i < sortedList.size(); i++)
        {
            int tokenId = sortedList.get(i);
            if (tokenId != 0)
            {
                char zone = TicketDecode.getZoneChar(tokenId);
                int seatNumber = TicketDecode.getSeatIdInt(tokenId);
                if (seatNumber != currentSeat + 1 || zone != currentZone
                        || i == (sortedList.size() - 1)) //check consecutive seats and zone is still the same, and push final ticket
                {
                    if (currentRange != null) items.add(new TokenIdSortedItem(currentRange, 10 + i));
                    int seatStart = TicketDecode.getSeatIdInt(tokenId);
                    currentRange = new TicketRange(tokenId);
                    currentZone = zone;
                }
                else
                {
                    //update
                    currentRange.tokenIds.add(tokenId);
                }

                currentSeat = seatNumber;
            }
        }
        items.endBatchedUpdates();
    }
}

