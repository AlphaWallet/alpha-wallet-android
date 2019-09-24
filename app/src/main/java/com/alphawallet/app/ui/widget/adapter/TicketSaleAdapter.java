package com.alphawallet.app.ui.widget.adapter;

import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.tools.TokenDefinition;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ERC721Token;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.OnTokenCheckListener;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.entity.AssetSortedItem;
import com.alphawallet.app.ui.widget.entity.MarketSaleHeaderSortedItem;
import com.alphawallet.app.ui.widget.entity.QuantitySelectorSortedItem;
import com.alphawallet.app.ui.widget.entity.RedeemHeaderSortedItem;
import com.alphawallet.app.ui.widget.entity.TicketSaleSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenIdSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenscriptSortedItem;
import com.alphawallet.app.ui.widget.entity.TransferHeaderSortedItem;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.OpenseaHolder;
import com.alphawallet.app.ui.widget.holder.OpenseaSelectHolder;
import com.alphawallet.app.ui.widget.holder.QuantitySelectorHolder;
import com.alphawallet.app.ui.widget.holder.RedeemTicketHolder;
import com.alphawallet.app.ui.widget.holder.SalesOrderHeaderHolder;
import com.alphawallet.app.ui.widget.holder.TicketHolder;
import com.alphawallet.app.ui.widget.holder.TicketSaleHolder;
import com.alphawallet.app.ui.widget.holder.TokenDescriptionHolder;
import com.alphawallet.app.ui.widget.holder.TokenscriptViewHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.ui.widget.holder.TransferHeaderHolder;

/**
 * Created by James on 12/02/2018.
 */

public class TicketSaleAdapter extends NonFungibleTokenAdapter {

    private OnTokenCheckListener onTokenCheckListener;
    private TicketRange selectedTicketRange;
    private Asset selectedAsset;
    private QuantitySelectorHolder quantitySelector;

    /* Context ctx is used to initialise assetDefinition in the super class */
    public TicketSaleAdapter(OnTokenClickListener tokenClickListener, Token t, AssetDefinitionService assetService) {
        super(tokenClickListener, t, assetService, null);
        onTokenCheckListener = this::onTokenCheck;
        selectedTicketRange = null;
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
            case TicketSaleHolder.VIEW_TYPE: {
                TicketSaleHolder tokenHolder = new TicketSaleHolder(R.layout.item_ticket, parent, token, assetService);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                tokenHolder.setOnTokenCheckListener(onTokenCheckListener);
                holder = tokenHolder;
            } break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            } break;
            case TokenDescriptionHolder.VIEW_TYPE: {
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent, token, assetService, assetCount);
            } break;
            case SalesOrderHeaderHolder.VIEW_TYPE: {
                holder = new SalesOrderHeaderHolder(R.layout.item_redeem_ticket, parent, assetService);
            } break;
            case RedeemTicketHolder.VIEW_TYPE: {
                holder = new RedeemTicketHolder(R.layout.item_redeem_ticket, parent);
            } break;
            case QuantitySelectorHolder.VIEW_TYPE: {
                quantitySelector = new QuantitySelectorHolder(R.layout.item_quantity_selector, parent, getCheckedItem().tokenIds.size(), assetService);
                holder = quantitySelector;
            } break;
            case TransferHeaderHolder.VIEW_TYPE: {
                holder = new TransferHeaderHolder(R.layout.item_redeem_ticket, parent);
            } break;
            case TokenscriptViewHolder.VIEW_TYPE: {
                holder = new TokenscriptViewHolder(R.layout.item_tokenscript, parent, token, assetService, false);
            } break;
            case OpenseaHolder.VIEW_TYPE: {
                holder = new OpenseaSelectHolder(R.layout.item_opensea_token, parent, token);
                ((OpenseaSelectHolder)holder).setOnTokenCheckListener(this::onOpenseaCheck);
            } break;
        }

        return holder;
    }

    public void setTransferTicket(Token t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new TransferHeaderSortedItem(t));
        addTokens(t);

        items.endBatchedUpdates();
    }

    private void addTokens(Token t)
    {
        if (t instanceof ERC721Token)
        {
            setERC721Tokens(t, null);
        }
        else if (t instanceof Ticket)
        {
            addRanges(t);
        }
        else
        {
            System.out.println("*** UNKNOWN TOKEN IN LIST **");
        }
    }

    private void addRanges(Token t)
    {
        //first sort the balance array
        currentRange = null;
        List<TicketRangeElement> sortedList = generateSortedList(assetService, token, t.getArrayBalance());
        addSortedItems(sortedList, t, TicketSaleSortedItem.VIEW_TYPE);
    }

    @Override
    public void setToken(Token t) {
        items.beginBatchedUpdates();
        items.clear();
        items.add(new MarketSaleHeaderSortedItem(t));

        addRanges(t);
        items.endBatchedUpdates();
        notifyDataSetChanged();
    }

    public void setRedeemTicket(Token t) {
        items.beginBatchedUpdates();
        items.clear();

        TokenDefinition td = assetService.getAssetDefinition(t.tokenInfo.chainId, t.tokenInfo.address);
        if (td != null)
        {
            items.add(new TokenscriptSortedItem(t));
        }
        else
        {
            items.add(new RedeemHeaderSortedItem(t));
        }

        addRanges(t);
        items.endBatchedUpdates();
    }

    //TODO: Make this into a single templated fetch
    public List<String> getERC721Checked()
    {
        List<String> checkedItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i) instanceof AssetSortedItem)
            {
                AssetSortedItem thisItem = (AssetSortedItem) items.get(i);
                if (thisItem.value.isChecked)
                {
                    checkedItems.add(thisItem.value.getTokenId());
                }
            }
        }
        return checkedItems;
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

    public void setRedeemTicketQuantity(TicketRange range, Token ticket)
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

    private void onOpenseaCheck(Asset asset)
    {
        if (selectedAsset != null)
        {
            selectedAsset.isChecked = false;
        }
        asset.isChecked = true;
        selectedAsset = asset;
        notifyDataSetChanged();
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
