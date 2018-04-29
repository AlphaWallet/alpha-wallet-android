package io.awallet.crypto.alphawallet.ui.widget.adapter;

import android.content.Context;
import android.view.ViewGroup;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.TicketRangeElement;
import io.awallet.crypto.alphawallet.repository.AssetDefinition;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;
import io.awallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.ui.widget.entity.TokenBalanceSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TokenIdSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TicketHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenDescriptionHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;

import org.web3j.utils.Numeric;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 9/02/2018.
 */

public class TicketAdapter extends TokensAdapter {
    TicketRange currentRange = null;
    final Ticket ticket;
    protected AssetDefinition assetDefinition;

    protected OnTicketIdClickListener onTicketIdClickListener;

    public TicketAdapter(Context ctx, OnTicketIdClickListener onTicketIdClickListener, Ticket t) {
        super();
        assetDefinition = initAssetDefinition(ctx);
        this.onTicketIdClickListener = onTicketIdClickListener;
        ticket = t;
        setTicket(t);
    }

    public TicketAdapter(Context ctx, OnTicketIdClickListener onTicketIdClick, Ticket ticket, String ticketIds)
    {
        super();
        this.onTicketIdClickListener = onTicketIdClick;
        assetDefinition = initAssetDefinition(ctx);
        this.ticket = ticket;
        //setTicket(ticket);
        setTicketRange(ticket, ticketIds);
    }

    private AssetDefinition initAssetDefinition(Context ctx)
    {
        AssetDefinition definition = null;
        try
        {
            definition = new AssetDefinition("TicketingContract.xml", ctx.getResources());
        }
        catch (IOException |SAXException e)
        {
            // TODO: how to gracefully bail out from creating a new activity?
            // Answer - TODO: fall back to static class which provides only the ticket name
            e.printStackTrace();
            //throw new RuntimeException("Error in parsing XML asset Definition");
        }

        return definition;
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

    private void setTicketRange(Ticket t, String ticketIds)
    {
        items.beginBatchedUpdates();
        items.clear();

	/* as why there are 2 for loops immediately following: the
	 * sort that's required to get groupings. Splitting it in two
	 * makes the algorithm n*2 complexity (plus a log n for sort),
	 * rather than a n^2 complexity which you'd need to do it in
	 * one go. The code produced is simple enough for anyone
	 * looking at it in future. - James Brown
         */
        List<BigInteger> idList = t.stringHexToBigIntegerList(ticketIds);
        List<TicketRangeElement> sortedList = new ArrayList<>();
        for (BigInteger v : idList)
        {
            if (v.compareTo(BigInteger.ZERO) == 0) continue;
            TicketRangeElement e = new TicketRangeElement();
            e.id = v;
            NonFungibleToken nft = new NonFungibleToken(v, assetDefinition);
            e.ticketNumber = nft.getAttribute("number").value.intValue();
            e.category = (short)nft.getAttribute("category").value.intValue();
            e.match = (short)nft.getAttribute("match").value.intValue();
            e.venue = (short)nft.getAttribute("venue").value.intValue();
            sortedList.add(e);
        }
        TicketRangeElement.sortElements(sortedList);

        int currentCat = 0;
        int currentNumber = -1;

        for (int i = 0; i < sortedList.size(); i++)
        {
            TicketRangeElement e = sortedList.get(i);
            if (currentRange == null || e.ticketNumber != currentNumber + 1 || e.category != currentCat) //check consecutive seats and zone is still the same, and push final ticket
            {
                currentRange = new TicketRange(e.id, t.getAddress());
                items.add(new TokenIdSortedItem(currentRange, 10 + i));
                currentCat = e.category;
            }
            else
            {
                //update
                currentRange.tokenIds.add(e.id);
            }
            currentNumber = e.ticketNumber;
        }

        items.endBatchedUpdates();
    }

    public void setTicket(Ticket t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(t));

        addRanges(t);
        items.endBatchedUpdates();
    }

    /* This one look similar to the one in TicketAdapter, it needs a
     * bit more abstraction to merge - the types produced are
     * different.*/
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
            e.ticketNumber = nft.getAttribute("number").value.intValue();
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
                items.add(new TokenIdSortedItem(currentRange, 10 + i));
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
}

