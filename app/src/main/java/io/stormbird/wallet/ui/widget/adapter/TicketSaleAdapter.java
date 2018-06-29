package io.stormbird.wallet.ui.widget.adapter;

import android.content.Context;
import android.view.ViewGroup;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.TicketRangeElement;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.OnTicketIdClickListener;
import io.stormbird.wallet.ui.widget.OnTokenCheckListener;
import io.stormbird.wallet.ui.widget.entity.MarketSaleHeaderSortedItem;
import io.stormbird.wallet.ui.widget.entity.QuantitySelectorSortedItem;
import io.stormbird.wallet.ui.widget.entity.RedeemHeaderSortedItem;
import io.stormbird.wallet.ui.widget.entity.TicketSaleSortedItem;
import io.stormbird.wallet.ui.widget.entity.TokenIdSortedItem;
import io.stormbird.wallet.ui.widget.entity.TransferHeaderSortedItem;
import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;
import io.stormbird.wallet.ui.widget.holder.QuantitySelectorHolder;
import io.stormbird.wallet.ui.widget.holder.RedeemTicketHolder;
import io.stormbird.wallet.ui.widget.holder.SalesOrderHeaderHolder;
import io.stormbird.wallet.ui.widget.holder.TicketHolder;
import io.stormbird.wallet.ui.widget.holder.TicketSaleHolder;
import io.stormbird.wallet.ui.widget.holder.TokenDescriptionHolder;
import io.stormbird.wallet.ui.widget.holder.TotalBalanceHolder;
import io.stormbird.wallet.ui.widget.holder.TransferHeaderHolder;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;

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
    public TicketSaleAdapter(OnTicketIdClickListener onTicketIdClickListener, Ticket t, AssetDefinitionService assetService) {
        super(onTicketIdClickListener, t, assetService);
        onTokenCheckListener = this::onTokenCheck;
        selectedTicketRange = null;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketHolder.VIEW_TYPE: {
                TicketHolder tokenHolder = new TicketHolder(R.layout.item_ticket, parent, ticket, assetService);
                tokenHolder.setOnTokenClickListener(onTicketIdClickListener);
                holder = tokenHolder;
            } break;
            case TicketSaleHolder.VIEW_TYPE: {
                TicketSaleHolder tokenHolder = new TicketSaleHolder(R.layout.item_ticket, parent, ticket, assetService);
                tokenHolder.setOnTokenClickListener(onTicketIdClickListener);
                tokenHolder.setOnTokenCheckListener(onTokenCheckListener);
                holder = tokenHolder;
            } break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            } break;
            case TokenDescriptionHolder.VIEW_TYPE: {
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent, ticket, assetService);
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
            NonFungibleToken nft = assetService.getNonFungibleToken(v);
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
