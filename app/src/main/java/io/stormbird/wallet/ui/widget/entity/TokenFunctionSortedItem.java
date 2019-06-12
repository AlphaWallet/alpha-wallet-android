package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.ui.widget.holder.AssetInstanceScriptHolder;
import io.stormbird.wallet.ui.widget.holder.TokenFunctionViewHolder;

/**
 * Created by James on 3/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionSortedItem extends SortedItem<String>
{
    public static final int VIEW_TYPE = TokenFunctionViewHolder.VIEW_TYPE;

    public TokenFunctionSortedItem(String data, int weight) {
        super(VIEW_TYPE, data, weight);
    }

    @Override
    public int compare(SortedItem other)
    {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return false;
    }
}
