package com.wallet.crypto.alphawallet.ui.widget.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenCheckListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.MarketSaleHeaderSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.QuantitySelectorSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.RedeemHeaderSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.SortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketSaleSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.QuantitySelectorHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.RedeemTicketHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.SalesOrderHeaderHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TicketSaleHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TokenDescriptionHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by James on 12/02/2018.
 */

public class TicketSaleAdapter extends TicketAdapter {

    private OnTokenCheckListener onTokenCheckListener;
    private TicketRange selectedTicketRange;
    private QuantitySelectorHolder quantitySelector;

    public TicketSaleAdapter(OnTicketIdClickListener onTicketIdClickListener, Ticket t) {
        super(onTicketIdClickListener, t);
        onTokenCheckListener = this::onTokenCheck;
        selectedTicketRange = null;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketSaleHolder.VIEW_TYPE: {
                TicketSaleHolder tokenHolder = new TicketSaleHolder(R.layout.item_ticket, parent);
                tokenHolder.setOnTokenClickListener(onTicketIdClickListener);
                tokenHolder.setOnTokenCheckListener(onTokenCheckListener);
                holder = tokenHolder;
            } break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            } break;
            case TokenDescriptionHolder.VIEW_TYPE: {
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent);
            } break;
            case SalesOrderHeaderHolder.VIEW_TYPE: {
                holder = new SalesOrderHeaderHolder(R.layout.item_token_description, parent);
            } break;
            case RedeemTicketHolder.VIEW_TYPE: {
                holder = new RedeemTicketHolder(R.layout.item_token_description, parent);
            } break;
            case QuantitySelectorHolder.VIEW_TYPE: {
                quantitySelector = new QuantitySelectorHolder(R.layout.item_quantity_selector, parent);
                holder = quantitySelector;
            } break;
        }

        return holder;
    }

    public void setTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new MarketSaleHeaderSortedItem(t));

        TicketRange currentRange = null;
        int currentSeat = -1;
        char currentZone = '-';
        int i;
        //first sort the balance array
        List<Integer> sortedList = new ArrayList<>();
        sortedList.addAll(t.balanceArray);
        Collections.sort(sortedList);
        for (i = 0; i < sortedList.size(); i++)
        {
            int tokenId = sortedList.get(i);
            if (tokenId != 0)
            {
                char zone = TicketDecode.getZoneChar(tokenId);
                int seatNumber = TicketDecode.getSeatIdInt(tokenId);
                if (currentRange == null || seatNumber != currentSeat + 1 || zone != currentZone) //check consecutive seats and zone is still the same, and push final ticket
                {
                    currentRange = new TicketRange(tokenId, t.getAddress());
                    items.add(new TicketSaleSortedItem(currentRange, 10 + i));
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

    public void setRedeemTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new RedeemHeaderSortedItem(t));

        TicketRange currentRange = null;
        int currentSeat = -1;
        char currentZone = '-';
        int i;
        //first sort the balance array
        List<Integer> sortedList = new ArrayList<>();
        sortedList.addAll(t.balanceArray);
        Collections.sort(sortedList);
        for (i = 0; i < sortedList.size(); i++)
        {
            int tokenId = sortedList.get(i);
            if (tokenId != 0)
            {
                char zone = TicketDecode.getZoneChar(tokenId);
                int seatNumber = TicketDecode.getSeatIdInt(tokenId);
                if (currentRange == null || seatNumber != currentSeat + 1 || zone != currentZone) //check consecutive seats and zone is still the same, and push final ticket
                {
                    currentRange = new TicketRange(tokenId, t.getAddress());
                    items.add(new TicketSaleSortedItem(currentRange, 10 + i));
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

    public List<TicketRange> getCheckedItems()
    {
        List<TicketRange> checkedItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i) instanceof TicketSaleSortedItem)
            {
                TicketSaleSortedItem thisItem = (TicketSaleSortedItem) items.get(i);
                if (thisItem.value.isChecked)
                {
                    checkedItems.add(thisItem.value);
                }
            }
        }

        return checkedItems;
    }

    public TicketRange getCheckedItem()
    {
        return selectedTicketRange;
    }

    public void setRedeemTicketQuantity(TicketRange range, Ticket ticket)
    {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new QuantitySelectorSortedItem(ticket));

        selectedTicketRange = range;

        //now add the single range entry
        items.add(new TicketSaleSortedItem(range, 10));
        items.endBatchedUpdates();
    }

    public int getSelectedQuantity()
    {
        if (quantitySelector != null)
        {
            return quantitySelector.getCurrentQuantity();
        }
        else
        {
            return 0;
        }
    }

    private void onTokenCheck(TicketRange range) {
        if (selectedTicketRange != null)
            selectedTicketRange.isChecked = false;
        range.isChecked = true;
        selectedTicketRange = range;
        //user clicked a radio button, now invalidate all the other radio buttons
        notifyDataSetChanged();
    }
}
