package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.ViewGroup;
import com.bumptech.glide.Glide;
import com.alphawallet.app.ui.widget.entity.AssetInstanceSortedItem;
import com.alphawallet.app.ui.widget.entity.AssetSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TicketSaleSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenBalanceSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenFunctionSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenIdSortedItem;
import com.alphawallet.app.ui.widget.holder.AssetInstanceScriptHolder;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.OpenseaHolder;
import com.alphawallet.app.ui.widget.holder.TicketHolder;
import com.alphawallet.app.ui.widget.holder.TicketSaleHolder;
import com.alphawallet.app.ui.widget.holder.TokenDescriptionHolder;
import com.alphawallet.app.ui.widget.holder.TokenFunctionViewHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.web3.entity.FunctionCallback;
import com.alphawallet.app.web3.entity.ScriptFunction;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ERC721Token;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 9/02/2018.
 */

public class NonFungibleTokenAdapter extends TokensAdapter {
    TicketRange currentRange = null;
    final Token token;
    protected OpenseaService openseaService;
    private ScriptFunction tokenScriptHolderCallback;
    private boolean clickThrough = false;
    private FunctionCallback functionCallback;
    private boolean containsScripted = false;
    protected int assetCount;

    public NonFungibleTokenAdapter(OnTokenClickListener tokenClickListener, Token t, AssetDefinitionService service, OpenseaService opensea) {
        super(tokenClickListener, service);
        assetCount = 0;
        token = t;
        clickThrough = true;
        openseaService = opensea;
        if (t instanceof Ticket) setToken(t);
        if (t instanceof ERC721Token) setERC721Tokens(t, null);
    }

    public NonFungibleTokenAdapter(OnTokenClickListener tokenClickListener, Token token, String ticketIds, AssetDefinitionService service, OpenseaService opensea)
    {
        super(tokenClickListener, service);
        assetCount = 0;
        this.token = token;
        if (token.isERC875()) setTokenRange(token, ticketIds);
        openseaService = opensea;
        if (token instanceof ERC721Token) setERC721Tokens(token, ticketIds);
    }

    public NonFungibleTokenAdapter(Token token, String displayIds, AssetDefinitionService service)
    {
        super(null, service);
        this.token = token;
        clickThrough = false;
        setTokenRange(token, displayIds);
    }

    public NonFungibleTokenAdapter(Token token, String viewCode, FunctionCallback callback, AssetDefinitionService service)
    {
        super(null, service);
        functionCallback = callback;
        this.token = token;
        TokenFunctionSortedItem item = new TokenFunctionSortedItem(viewCode, 200);
        items.clear();
        items.add(item);
        notifyDataSetChanged();
        containsScripted = true;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketHolder.VIEW_TYPE:
                holder = new TicketHolder(R.layout.item_ticket, parent, token, assetService);
                holder.setOnTokenClickListener(onTokenClickListener);
                break;
            case TotalBalanceHolder.VIEW_TYPE:
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
                break;
            case TokenDescriptionHolder.VIEW_TYPE:
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent, token, assetService, assetCount);
                break;
            case OpenseaHolder.VIEW_TYPE:
                holder = new OpenseaHolder(R.layout.item_opensea_token, parent, token);
                holder.setOnTokenClickListener(onTokenClickListener);
                break;
            case AssetInstanceScriptHolder.VIEW_TYPE:
                holder = new AssetInstanceScriptHolder(R.layout.item_ticket, parent, token, assetService, clickThrough);
                holder.setOnTokenClickListener(onTokenClickListener);
                break;
            case TokenFunctionViewHolder.VIEW_TYPE:
                holder = new TokenFunctionViewHolder(R.layout.item_function_layout, parent, token, functionCallback, assetService);
                tokenScriptHolderCallback = (ScriptFunction)holder;
                break;
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
                assetCount++;
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
        int holderType = TokenIdSortedItem.VIEW_TYPE;

        if (assetService.hasTokenView(t.tokenInfo.chainId, t.getAddress()))
        {
            containsScripted = true;
            holderType = AssetInstanceSortedItem.VIEW_TYPE;
        }

        List<BigInteger> idList = t.stringHexToBigIntegerList(ticketIds);
        List<TicketRangeElement> sortedList = generateSortedList(assetService, token, idList); //generate sorted list
        addSortedItems(sortedList, t, holderType); //insert sorted items into view

        items.endBatchedUpdates();
    }

    public void setToken(Token t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(t));
        assetCount = t.getTicketCount();
        if (t instanceof Ticket) addRanges(t);
        items.endBatchedUpdates();
    }

    private void addRanges(Token t)
    {
        currentRange = null;
        List<TicketRangeElement> sortedList = generateSortedList(assetService, t, t.getArrayBalance());
        //determine what kind of holder we need:
        int holderType = AssetInstanceSortedItem.VIEW_TYPE;
        containsScripted = true;

        //        if (assetService.hasTokenView(t.tokenInfo.chainId, t.getAddress()))
        //        {
        //            containsScripted = true;
        //            holderType = AssetInstanceSortedItem.VIEW_TYPE;
        //        }

        addSortedItems(sortedList, t, holderType);
    }

    protected List<TicketRangeElement> generateSortedList(AssetDefinitionService assetService, Token token, List<BigInteger> idList)
    {
        List<TicketRangeElement> sortedList = new ArrayList<>();
        for (BigInteger v : idList)
        {
            if (v.compareTo(BigInteger.ZERO) == 0) continue;
            TicketRangeElement e = new TicketRangeElement(assetService, token, v);
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
            case AssetInstanceScriptHolder.VIEW_TYPE:
                containsScripted = true;
                item = (T) new AssetInstanceSortedItem(range, weight);
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
        long currentTime = 0;

        for (int i = 0; i < sortedList.size(); i++)
        {
            TicketRangeElement e = sortedList.get(i);
            if (currentRange != null && e.id.equals(currentRange.tokenIds.get(0)))
            {
                currentRange.tokenIds.add(e.id);
            }
            else if (currentRange == null || e.time != currentTime) //check consecutive seats and zone is still the same, and push final ticket
            {
                currentRange = new TicketRange(e.id, t.getAddress());
                final T item = generateType(currentRange, 10 + i, id);
                items.add((SortedItem)item);
                currentTime = e.time;
            }
            else
            {
                //update
                currentRange.tokenIds.add(e.id);
            }
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
        if (token instanceof ERC721Token)
        {
            clearCache(ctx)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::cleared, error -> System.out.println("Cache clean: " + error.getMessage()))
                    .isDisposed();
        }
    }

    private void cleared(Boolean aBoolean)
    {
        this.notifyDataSetChanged();
    }

    public void addFunctionView(Token token, String view)
    {
        TokenFunctionSortedItem item = new TokenFunctionSortedItem(view, 200);
        items.add(item);
        notifyDataSetChanged();
    }

    public boolean containsScripted()
    {
        return containsScripted;
    }

    public void passFunction(String function, String arg)
    {
        //pass into the view
        tokenScriptHolderCallback.callFunction(function, arg);
    }

    public void setRadioButtons(boolean expose)
    {
        boolean requiresFullRedraw = false;
        //uncheck all ranges, note that the selected range will be checked after the refresh
        for (int i = 0; i < items.size(); i++)
        {
            SortedItem si = items.get(i);
            if (si.isRadioExposed() != expose) requiresFullRedraw = true;
            if (si.view != null)
            {
                AppCompatRadioButton button = si.view.itemView.findViewById(R.id.radioBox);
                if (button != null && (button.isChecked() || si.isItemChecked())) button.setChecked(false);
            }
            si.setIsChecked(false);
            si.setExposeRadio(expose);
        }

        if (requiresFullRedraw)
        {
            notifyDataSetChanged();
        }
    }

    public List<BigInteger> getSelectedTokenIds(List<BigInteger> selection)
    {
        List<BigInteger> tokenIds = new ArrayList<>(selection);
        for (int i = 0; i < items.size(); i++)
        {
            SortedItem si = items.get(i);
            if (si.isItemChecked())
            {
                List<BigInteger> rangeIds = si.getTokenIds();
                for (BigInteger tokenId : rangeIds) if (!tokenIds.contains(tokenId)) tokenIds.add(tokenId);
            }
        }

        return tokenIds;
    }

    public int getSelectedGroups()
    {
        int selected = 0;
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).isItemChecked()) selected++;
        }

        return selected;
    }
}
