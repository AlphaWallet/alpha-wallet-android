package io.awallet.crypto.alphawallet.ui.widget.adapter;

import android.media.session.MediaSession;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.TicketDecode;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import io.awallet.crypto.alphawallet.ui.widget.OnTokenClickListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.SortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.ui.widget.entity.TokenBalanceSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TokenIdSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TokenSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TotalBalanceSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TicketHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenDescriptionHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;

import org.web3j.abi.datatypes.Int;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by James on 9/02/2018.
 */

public class TicketAdapter extends TokensAdapter {
    TicketRange currentRange = null;

    protected OnTicketIdClickListener onTicketIdClickListener;

    public TicketAdapter(OnTicketIdClickListener onTicketIdClickListener, Ticket t) {
        super();
        this.onTicketIdClickListener = onTicketIdClickListener;
        setTicket(t);
    }

    public TicketAdapter(OnTicketIdClickListener onTicketIdClick, Ticket ticket, String ticketIds)
    {
        super();
        this.onTicketIdClickListener = onTicketIdClick;
        //setTicket(ticket);
        setTicketRange(ticket, ticketIds);
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

    public int getTicketRangeCount() {
        int count = 0;
        if (currentRange != null) {
            count = currentRange.tokenIds.size();
        }
        return count;
    }

    private void setTicketRange(Ticket ticket, String ticketIds)
    {
        items.beginBatchedUpdates();
        items.clear();
        //items.add(new TokenBalanceSortedItem(ticket));

        //first convert to integer array
        List<Integer> sortedList = ticket.parseIDListInteger(ticketIds);
        Collections.sort(sortedList);

        int currentSeat = -1;
        char currentZone = '-';
//        TicketRange currentRange = null;
        //now generate the ticket display
        for (int i = 0; i < sortedList.size(); i++)
        {
            int tokenId = sortedList.get(i);
            if (tokenId != 0)
            {
                char zone = TicketDecode.getZoneChar(tokenId);
                int seatNumber = TicketDecode.getSeatIdInt(tokenId);
                if (seatNumber != currentSeat + 1 || zone != currentZone) //check consecutive seats and zone is still the same, and push final ticket
                {
                    currentRange = new TicketRange(tokenId, ticket.getAddress());
                    items.add(new TokenIdSortedItem(currentRange, 10 + i));
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

    public void setTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(t));

        TicketRange currentRange = null;
        int currentSeat = -1;
        char currentZone = '-';
        boolean currentBurn = false;
        //first sort the balance array
        List<Integer> sortedList = new ArrayList<>();
        sortedList.addAll(t.balanceArray);
        sortedList.addAll(t.getBurnList());
        Collections.sort(sortedList);
        for (int i = 0; i < sortedList.size(); i++)
        {
            int tokenId = sortedList.get(i);
            if (tokenId != 0)
            {
                char zone = TicketDecode.getZoneChar(tokenId);
                int seatNumber = TicketDecode.getSeatIdInt(tokenId);
                boolean isBurned = false;
                if (t.getBurnList().contains((Integer)tokenId)) {
                    isBurned = true;
                }
                if (seatNumber != currentSeat + 1 || zone != currentZone || isBurned != currentBurn) //check consecutive seats and zone is still the same, and push final ticket
                {
                    currentRange = new TicketRange(tokenId, t.getAddress(), isBurned);
                    items.add(new TokenIdSortedItem(currentRange, 10 + i));
                    currentZone = zone;
                    currentBurn = isBurned;
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

