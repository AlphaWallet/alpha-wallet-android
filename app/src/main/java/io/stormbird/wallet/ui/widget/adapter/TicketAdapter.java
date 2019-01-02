package io.stormbird.wallet.ui.widget.adapter;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.TicketRangeElement;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.OpenseaService;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.entity.*;
import io.stormbird.wallet.ui.widget.holder.*;

/**
 * Created by James on 9/02/2018.
 */

public class TicketAdapter extends TokensAdapter {
    TicketRange currentRange = null;
    final Token token;
    protected OpenseaService openseaService;

    public TicketAdapter(OnTokenClickListener tokenClickListener, Token t, AssetDefinitionService service, OpenseaService opensea) {
        super(tokenClickListener, service);
        token = t;
        openseaService = opensea;
        if (t instanceof Ticket) setToken(t);
        if (t instanceof ERC721Token) setERC721Tokens(t, null);
    }

    public TicketAdapter(OnTokenClickListener tokenClickListener, Token token, String ticketIds, AssetDefinitionService service, OpenseaService opensea)
    {
        super(tokenClickListener, service);
        this.token = token;
        if (token instanceof Ticket) setTokenRange(token, ticketIds);
        openseaService = opensea;
        if (token instanceof ERC721Token) setERC721Tokens(token, ticketIds);
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketHolder.VIEW_TYPE: {
                TicketHolder tokenHolder = new TicketHolder(R.layout.item_ticket, parent, token, assetService);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenHolder;
            } break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            } break;
            case TokenDescriptionHolder.VIEW_TYPE: {
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent, token, assetService);
            } break;
            case IFrameHolder.VIEW_TYPE: {
                holder = new IFrameHolder(R.layout.item_iframe_token, parent, token, assetService);
            }
            break;
            case OpenseaHolder.VIEW_TYPE: {
                holder = new OpenseaHolder(R.layout.item_opensea_token, parent, token);
            } break;
        }

        return holder;
    }

    protected void setERC721Tokens(Token token, String ticketId)
    {
        if (!(token instanceof ERC721Token)) return;
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(token));
        int weight = 1; //use the same order we receive from OpenSea

        // populate the ERC721 items
        for (Asset asset : ((ERC721Token)token).tokenBalance)
        {
            if (ticketId == null || ticketId.equals(asset.getTokenId()))
            {
                items.add(new AssetSortedItem(asset, weight++));
            }
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

        List<BigInteger> idList = t.stringHexToBigIntegerList(ticketIds);
        List<TicketRangeElement> sortedList = generateSortedList(assetService, token, idList); //generate sorted list
        addSortedItems(sortedList, t, TokenIdSortedItem.VIEW_TYPE); //insert sorted items into view

        items.endBatchedUpdates();
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
        currentRange = null;
        List<TicketRangeElement> sortedList = generateSortedList(assetService, t, t.getArrayBalance());
        //determine what kind of holder we need:
        int holderType = TokenIdSortedItem.VIEW_TYPE;
        if (assetService.hasIFrame(t.getAddress()))
        {
            if (sortedList.size() == 0)
            {
                //display iframe information
                IFrameSortedItem item = new IFrameSortedItem(new TicketRange(BigInteger.ZERO, token.getAddress()), 2);
                items.add(item);
            }
            else
            {
                IFrameSortedItem item = new IFrameSortedItem(new TicketRange(sortedList.get(0).id, token.getAddress()), 2);
                items.add(item);
            }
        }

        addSortedItems(sortedList, t, holderType);
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

    @SuppressWarnings("unchecked")
    protected <T> T generateType(TicketRange range, int weight, int id)
    {
        T item;
        switch (id)
        {
            case IFrameHolder.VIEW_TYPE:
                item = (T) new IFrameSortedItem(range, weight);
                break;
            case TicketSaleHolder.VIEW_TYPE:
                item = (T) new TicketSaleSortedItem(range, weight);
                break;
            case TicketHolder.VIEW_TYPE:
            default:
                item = (T) new TokenIdSortedItem(range, weight);
                break;
        }

        return item;
    }

    protected <T> SortedList<T> addSortedItems(List<TicketRangeElement> sortedList, Token t, int id)
    {
        int currentNumber = -1;
        int currentCat = 0;
        long currentTime = 0;

        for (int i = 0; i < sortedList.size(); i++)
        {
            TicketRangeElement e = sortedList.get(i);
            if (currentRange != null && e.id.equals(currentRange.tokenIds.get(0)))
            {
                currentRange.tokenIds.add(e.id);
            }
            else if (currentRange == null || (e.ticketNumber != currentNumber + 1 && e.ticketNumber != currentNumber) || e.category != currentCat || e.time != currentTime) //check consecutive seats and zone is still the same, and push final ticket
            {
                currentRange = new TicketRange(e.id, t.getAddress());
                final T item = generateType(currentRange, 10 + i, id);
                items.add((SortedItem)item);
                currentCat = e.category;
                currentTime = e.time;
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

    private Single<Boolean> clearCache(Context ctx)
    {
        return Single.fromCallable(() -> {
            Glide.get(ctx).clearDiskCache();
            return true;
        });
    }

    //TODO: Find out how to calculate the storage hash for each image and reproduce that, deleting only the right image.
    public void reloadAssets(Context ctx)
    {
        token.setRequireAuxRefresh();

        if (token instanceof ERC721Token)
        {
            Disposable d = clearCache(ctx)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::cleared, error -> System.out.println("Cache clean: " + error.getMessage()));
        }
    }

    private void cleared(Boolean aBoolean)
    {
        this.notifyDataSetChanged();
    }
}
