package io.stormbird.wallet.ui.widget.adapter;

import android.support.v7.widget.AppCompatRadioButton;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.TicketRangeElement;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.OnTokenCheckListener;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.entity.*;
import io.stormbird.wallet.ui.widget.holder.*;

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
