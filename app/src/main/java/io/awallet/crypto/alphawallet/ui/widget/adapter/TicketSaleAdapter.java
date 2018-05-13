package io.awallet.crypto.alphawallet.ui.widget.adapter;

import android.content.Context;
import android.view.ViewGroup;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.TicketRangeElement;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;
import io.awallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import io.awallet.crypto.alphawallet.ui.widget.OnTokenCheckListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.MarketSaleHeaderSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.QuantitySelectorSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.RedeemHeaderSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketSaleSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TokenIdSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TransferHeaderSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.QuantitySelectorHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.RedeemTicketHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.SalesOrderHeaderHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TicketHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TicketSaleHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenDescriptionHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TransferHeaderHolder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 12/02/2018.
 */

public class TicketSaleAdapter extends TicketAdapter {

    private OnTokenCheckListener onTokenCheckListener;
    private TicketRange selectedTicketRange;
    private QuantitySelectorHolder quantitySelector;

    /* Context ctx is used to initialise assetDefinition in the super class */
    public TicketSaleAdapter(Context ctx, OnTicketIdClickListener onTicketIdClickListener, Ticket t) {
        super(ctx, onTicketIdClickListener, t);
        onTokenCheckListener = this::onTokenCheck;
        selectedTicketRange = null;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketHolder.VIEW_TYPE: {
                TicketHolder tokenHolder = new TicketHolder(R.layout.item_ticket, parent, assetDefinition, ticket);
                tokenHolder.setOnTokenClickListener(onTicketIdClickListener);
                holder = tokenHolder;
            } break;
            case TicketSaleHolder.VIEW_TYPE: {
                TicketSaleHolder tokenHolder = new TicketSaleHolder(R.layout.item_ticket, parent, assetDefinition, ticket);
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
                holder = new SalesOrderHeaderHolder(R.layout.item_redeem_ticket, parent);
            } break;
            case RedeemTicketHolder.VIEW_TYPE: {
                holder = new RedeemTicketHolder(R.layout.item_redeem_ticket, parent);
            } break;
            case QuantitySelectorHolder.VIEW_TYPE: {
                quantitySelector = new QuantitySelectorHolder(R.layout.item_quantity_selector, parent, getCheckedItem().tokenIds.size());
                holder = quantitySelector;
            } break;
            case TransferHeaderHolder.VIEW_TYPE: {
                holder = new TransferHeaderHolder(R.layout.item_redeem_ticket, parent);
            } break;
        }

        return holder;
    }

    public void setTransferTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TransferHeaderSortedItem(t));

        addRanges(t);

        items.endBatchedUpdates();
    }

    private void addRanges(Ticket t)
    {
        TicketRange currentRange = null;
        int currentNumber = -1;

        //first sort the balance array
        List<TicketRangeElement> sortedList = new ArrayList<>();
        for (BigInteger v : t.balanceArray)
        {
            if (v.compareTo(BigInteger.ZERO) == 0) continue;
            TicketRangeElement e = new TicketRangeElement();
            e.id = v;
            NonFungibleToken nft = new NonFungibleToken(v, assetDefinition);
            e.ticketNumber = nft.getAttribute("numero").value.intValue();
            e.category = (short)nft.getAttribute("category").value.intValue();
            e.match = (short)nft.getAttribute("match").value.intValue();
            e.venue = (short)nft.getAttribute("venue").value.intValue();
            sortedList.add(e);
        }
        TicketRangeElement.sortElements(sortedList);

        int currentCat = 0;

        for (int i = 0; i < sortedList.size(); i++)
        {
            TicketRangeElement e = sortedList.get(i);
            if (currentRange == null || e.ticketNumber != currentNumber + 1 || e.category != currentCat) //check consecutive seats and zone is still the same, and push final ticket
            {
                currentRange = new TicketRange(e.id, t.getAddress());
                items.add(new TicketSaleSortedItem(currentRange, 10 + i));
                currentCat = e.category;
            }
            else
            {
                //update
                currentRange.tokenIds.add(e.id);
            }
            currentNumber = e.ticketNumber;
        }
    }

    public void setTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new MarketSaleHeaderSortedItem(t));

        addRanges(t);
        items.endBatchedUpdates();
    }

    public void setRedeemTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new RedeemHeaderSortedItem(t));

        addRanges(t);
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
        items.add(new TokenIdSortedItem(range, 10));
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
