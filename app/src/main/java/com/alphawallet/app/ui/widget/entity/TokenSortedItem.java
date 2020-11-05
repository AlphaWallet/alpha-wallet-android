package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.tokens.TokenCardMeta;

public class TokenSortedItem extends SortedItem<TokenCardMeta> {

    private boolean debugging = false;

    public TokenSortedItem(int viewType, TokenCardMeta value, int weight) {
        super(viewType, value, weight);
    }

    public void debug()
    {
        debugging = true;
    }

    @Override
    public int compare(SortedItem other) {
        if (debugging) System.out.println("DEBUG: Compare: " + weight + " " + other.weight);
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        if (viewType == newItem.viewType)
        {
            TokenCardMeta newToken = (TokenCardMeta) newItem.value;
            //if (!oldToken.tokenId.equalsIgnoreCase(newToken.tokenId)) return false;
            if (debugging) System.out.println("DEBUG: Contents: " + weight + " " + newItem.weight + " Balance: " + value.balance + " " + newToken.balance);
            if (weight != newItem.weight) return false;
            else return value.balance.equals(newToken.balance) && value.type.ordinal() == newToken.type.ordinal();
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        if (viewType == other.viewType)
        {
            TokenCardMeta oldToken = value;
            TokenCardMeta newToken = (TokenCardMeta) other.value;

            if (debugging)
            {
                if (oldToken == null || newToken == null) System.out.println("DEBUG: Item: One is null");
                else System.out.println("DEBUG: Item: " + oldToken.tokenId + " " + newToken.tokenId);
            }

            if (oldToken == null || newToken == null) return false;
            else return oldToken.tokenId.equalsIgnoreCase(newToken.tokenId);
        }
        else
        {
            return false;
        }
    }
}
