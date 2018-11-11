package io.stormbird.wallet.ui.widget.adapter;

import android.support.v7.util.SortedList;
import android.view.ViewGroup;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.TicketRangeElement;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.OpenseaService;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.ui.widget.OnTicketIdClickListener;
import io.stormbird.wallet.ui.widget.entity.AssetSortedItem;
import io.stormbird.wallet.ui.widget.entity.SortedItem;
import io.stormbird.wallet.ui.widget.entity.TicketSaleSortedItem;
import io.stormbird.wallet.ui.widget.entity.TokenBalanceSortedItem;
import io.stormbird.wallet.ui.widget.entity.TokenIdSortedItem;
import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;
import io.stormbird.wallet.ui.widget.holder.OpenseaHolder;
import io.stormbird.wallet.ui.widget.holder.TicketHolder;
import io.stormbird.wallet.ui.widget.holder.TokenDescriptionHolder;
import io.stormbird.wallet.ui.widget.holder.TotalBalanceHolder;

/**
 * Created by James on 9/02/2018.
 */

public class TicketAdapter extends TokensAdapter {
    TicketRange currentRange = null;
    final Token token;
    protected AssetDefinitionService assetService;
    protected OpenseaService openseaService;

    protected OnTicketIdClickListener onTicketIdClickListener;

    public TicketAdapter(OnTicketIdClickListener onTicketIdClickListener, Token t, AssetDefinitionService service, OpenseaService opensea) {
        super();
        assetService = service;
        this.onTicketIdClickListener = onTicketIdClickListener;
        token = t;
        openseaService = opensea;
        if (t instanceof Ticket) setToken(t);
    }

    public TicketAdapter(OnTicketIdClickListener onTicketIdClick, Token token, String ticketIds, AssetDefinitionService service, OpenseaService opensea)
    {
        super();
        this.onTicketIdClickListener = onTicketIdClick;
        assetService = service;
        this.token = token;
        //setTicket(ticket);
        if (token instanceof Ticket) setTokenRange(token, ticketIds);
        openseaService = opensea;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketHolder.VIEW_TYPE: {
                TicketHolder tokenHolder = new TicketHolder(R.layout.item_ticket, parent, token, assetService);
                tokenHolder.setOnTokenClickListener(onTicketIdClickListener);
                holder = tokenHolder;
            } break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            } break;
            case TokenDescriptionHolder.VIEW_TYPE: {
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent, token, assetService);
            } break;
            case OpenseaHolder.VIEW_TYPE: {
                holder = new OpenseaHolder(R.layout.item_opensea_token, parent, token);
            } break;
        }

        return holder;
    }

    public void setERC721Contract(Token token)
    {
        if (!(token instanceof ERC721Token)) return;
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(token));
        int weight = 1; //use the same order we receive from OpenSea

        // populate the ERC721 items
        for (Asset asset : ((ERC721Token)token).tokenBalance)
        {
            items.add(new AssetSortedItem(asset, weight++));
        }
        items.endBatchedUpdates();
    }

    public int getTicketRangeCount() {
        int count = 0;
        if (currentRange != null) {
            count = currentRange.tokenIds.size();
        }
        return count;
    }

    private void setTokenRange(Token t, String ticketIds)
    {
        items.beginBatchedUpdates();
        items.clear();

        List<BigInteger> idList = ((Ticket)t).stringHexToBigIntegerList(ticketIds);
        List<TicketRangeElement> sortedList = generateSortedList(assetService, token, idList); //generate sorted list
        addSortedItems(sortedList, t, TokenIdSortedItem.class); //insert sorted items into view

        items.endBatchedUpdates();
    }

    protected List<TicketRangeElement> generateSortedList(AssetDefinitionService assetService, Token token, List<BigInteger> idList)
    {
        List<TicketRangeElement> sortedList = new ArrayList<>();
        for (BigInteger v : idList)
        {
            if (v.compareTo(BigInteger.ZERO) == 0) continue;
            TicketRangeElement e = new TicketRangeElement(assetService, token, v);
            e.id = v;
            sortedList.add(e);
        }
        TicketRangeElement.sortElements(sortedList);
        return sortedList;
    }

    protected <T> T generateType(TicketRange range, int weight, T type)
    {
        T item;
        if (type.getClass().isInstance(TokenIdSortedItem.class))
        {
            item = (T) new TokenIdSortedItem(range, weight);
        }
        else
        {
            item = (T) new TicketSaleSortedItem(range, weight);
        }

        return item;
    }

    protected <T> SortedList<T> addSortedItems(List<TicketRangeElement> sortedList, Token t, T element)
    {
        TicketRange currentRange = null;
        int currentNumber = -1;
        int currentCat = 0;

        for (int i = 0; i < sortedList.size(); i++)
        {
            TicketRangeElement e = sortedList.get(i);
            if (currentRange != null && e.id.equals(currentRange.tokenIds.get(0)))
            {
                currentRange.tokenIds.add(e.id);
            }
            else if (currentRange == null || e.ticketNumber != currentNumber + 1 || e.category != currentCat) //check consecutive seats and zone is still the same, and push final ticket
            {
                currentRange = new TicketRange(e.id, t.getAddress());
                final T item = generateType(currentRange, 10 + i, element);
                items.add((SortedItem)item);
                currentCat = e.category;
            }
            else
            {
                //update
                currentRange.tokenIds.add(e.id);
            }
            currentNumber = e.ticketNumber;
        }

        return null;
    }

    public void setToken(Token t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(t));

        if (t instanceof Ticket) addRanges(t);
        items.endBatchedUpdates();
    }

    private void addRanges(Token t)
    {
        TicketRange currentRange = null;
        int currentNumber = -1;

        //first sort the balance array
        List<TicketRangeElement> sortedList = generateSortedList(assetService, t, ((Ticket)t).balanceArray);
        addSortedItems(sortedList, t, TokenIdSortedItem.class);

//        List<TicketRangeElement> sortedList = new ArrayList<>();
//        for (BigInteger v : ((Ticket)t).balanceArray)
//        {
//            if (v.compareTo(BigInteger.ZERO) == 0) continue;
//            TicketRangeElement e = new TicketRangeElement(assetService, token, v);
//            e.id = v;
//            sortedList.add(e);
//        }
//        TicketRangeElement.sortElements(sortedList);
        //addSortedItems(sortedList, t); //insert sorted items into view

//        int currentCat = 0;
//
//        for (int i = 0; i < sortedList.size(); i++)
//        {
//            TicketRangeElement e = sortedList.get(i);
//            if (currentRange != null && e.id.equals(currentRange.tokenIds.get(0)))
//            {
//                currentRange.tokenIds.add(e.id);
//            }
//            else if (currentRange == null || e.ticketNumber != currentNumber + 1 || e.category != currentCat) //check consecutive seats and zone is still the same, and push final ticket
//            {
//                currentRange = new TicketRange(e.id, t.getAddress());
//                items.add(new TokenIdSortedItem(currentRange, 10 + i));
//                currentCat = e.category;
//            }
//            else
//            {
//                //update
//                currentRange.tokenIds.add(e.id);
//            }
//            currentNumber = e.ticketNumber;
//        }
    }
}

