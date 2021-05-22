package com.alphawallet.app.ui.widget.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.SortedList;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.entity.AssetInstanceSortedItem;
import com.alphawallet.app.ui.widget.entity.AssetSortedItem;
import com.alphawallet.app.ui.widget.entity.QuantitySelectorSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TokenBalanceSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenIdSortedItem;
import com.alphawallet.app.ui.widget.holder.AssetInstanceScriptHolder;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.OpenseaHolder;
import com.alphawallet.app.ui.widget.holder.QuantitySelectorHolder;
import com.alphawallet.app.ui.widget.holder.TicketHolder;
import com.alphawallet.app.ui.widget.holder.TokenDescriptionHolder;
import com.alphawallet.app.ui.widget.holder.TokenFunctionViewHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.web3.entity.FunctionCallback;
import com.alphawallet.token.entity.TicketRange;
import com.bumptech.glide.Glide;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.service.AssetDefinitionService.ASSET_SUMMARY_VIEW_NAME;

/**
 * Created by James on 9/02/2018.
 */

public class NonFungibleTokenAdapter extends TokensAdapter {
    TicketRange currentRange = null;
    final Token token;
    protected final OpenseaService openseaService;
    private final boolean clickThrough;
    protected int assetCount;
    private FunctionCallback functionCallback;
    private final Activity activity;

    public NonFungibleTokenAdapter(OnTokenClickListener tokenClickListener, Token t, AssetDefinitionService service,
                                   OpenseaService opensea, Activity activity) {
        super(tokenClickListener, service);
        assetCount = 0;
        token = t;
        clickThrough = true;
        openseaService = opensea;
        setToken(t);
        this.activity = activity;
    }

    public NonFungibleTokenAdapter(OnTokenClickListener tokenClickListener, Token t, List<BigInteger> tokenSelection,
                                   AssetDefinitionService service)
    {
        super(tokenClickListener, service);
        assetCount = 0;
        token = t;
        clickThrough = false;
        openseaService = null;
        setTokenRange(token, tokenSelection);
        this.activity = null;
    }
    
    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TicketHolder.VIEW_TYPE: //Ticket holder now deprecated //TODO: remove
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
                holder = new OpenseaHolder(R.layout.item_opensea_token, parent, token, activity, clickThrough);
                holder.setOnTokenClickListener(onTokenClickListener);
                break;
            case AssetInstanceScriptHolder.VIEW_TYPE:
                holder = new AssetInstanceScriptHolder(R.layout.item_ticket, parent, token, assetService, clickThrough);
                holder.setOnTokenClickListener(onTokenClickListener);
                break;
            case TokenFunctionViewHolder.VIEW_TYPE:
                holder = new TokenFunctionViewHolder(R.layout.item_function_layout, parent, token, functionCallback, assetService);
                break;
            case QuantitySelectorHolder.VIEW_TYPE:
                holder = new QuantitySelectorHolder(R.layout.item_quantity_selector, parent, assetCount, assetService);
                break;
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

    public void addQuantitySelector()
    {
        items.add(new QuantitySelectorSortedItem(token));
    }

    private void setTokenRange(Token t, List<BigInteger> tokenIds)
    {
        items.beginBatchedUpdates();
        items.clear();
        assetCount = tokenIds.size();
        int holderType = t.isERC721() ? OpenseaHolder.VIEW_TYPE : AssetInstanceScriptHolder.VIEW_TYPE;

        //TokenScript view for ERC721 overrides OpenSea display
        if (assetService.hasTokenView(t.tokenInfo.chainId, t.getAddress(), ASSET_SUMMARY_VIEW_NAME)) holderType = AssetInstanceScriptHolder.VIEW_TYPE;

        List<TicketRangeElement> sortedList = generateSortedList(assetService, token, tokenIds); //generate sorted list
        addSortedItems(sortedList, t, holderType); //insert sorted items into view

        items.endBatchedUpdates();
    }

    public void setToken(Token t)
    {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TokenBalanceSortedItem(t));
        assetCount = t.getTicketCount();
        int holderType = t.isERC721() ? OpenseaHolder.VIEW_TYPE : AssetInstanceScriptHolder.VIEW_TYPE;

        //TokenScript view for ERC721 overrides OpenSea display
        if (assetService.hasTokenView(t.tokenInfo.chainId, t.getAddress(), ASSET_SUMMARY_VIEW_NAME)) holderType = AssetInstanceScriptHolder.VIEW_TYPE;

        addRanges(t, holderType);
        items.endBatchedUpdates();
    }

    private void addRanges(Token t, int holderType)
    {
        currentRange = null;
        List<TicketRangeElement> sortedList = generateSortedList(assetService, t, t.getArrayBalance());
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
                item = (T) new AssetInstanceSortedItem(range, weight);
                break;
            case OpenseaHolder.VIEW_TYPE:
                item = (T) new AssetSortedItem(range, weight);
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
            if (currentRange != null && t.groupWithToken(currentRange, e, currentTime))
            {
                currentRange.tokenIds.add(e.id);
            }
            else
            {
                currentRange = new TicketRange(e.id, t.getAddress());
                final T item = generateType(currentRange, 10 + i, id);
                items.add((SortedItem)item);
                currentTime = e.time;
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
    //TODO: Possibly the best way is not to use glide, revert back to caching images as in the original implementation.
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

    public int getSelectedQuantity()
    {
        for (int i = 0; i < items.size(); i++)
        {
            SortedItem si = items.get(i);
            if (si.view.getItemViewType() == QuantitySelectorHolder.VIEW_TYPE)
            {
                return ((QuantitySelectorHolder) si.view).getCurrentQuantity();
            }
        }
        return 0;
    }

    public TicketRange getSelectedRange(List<BigInteger> selection)
    {
        int quantity = getSelectedQuantity();
        if (quantity > selection.size()) quantity = selection.size();
        List<BigInteger> subSelection = new ArrayList<>();

        for (int i = 0; i < quantity; i++)
        {
            subSelection.add(selection.get(i));
        }

        return new TicketRange(subSelection, token.getAddress(), false);
    }
}
