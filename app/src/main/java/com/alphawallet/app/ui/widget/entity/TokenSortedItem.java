package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.Token;

public class TokenSortedItem extends SortedItem<Token> {

    public TokenSortedItem(Token value, int weight) {
        super(TokenHolder.VIEW_TYPE, value, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        if (viewType == newItem.viewType)
        {
            Token oldToken = value;
            Token newToken = (Token) newItem.value;

            if (!oldToken.getAddress().equals(newToken.getAddress())) return false;
            else if (weight != newItem.weight) return false;
            else if (!oldToken.getFullBalance().equals(newToken.getFullBalance())) return false;
            else if (!oldToken.pendingBalance.equals(newToken.pendingBalance)) return false;
            else if (!oldToken.getFullName().equals(newToken.getFullName())) return false;
            else if (oldToken.ticker == null && newToken.ticker != null) return false;

            //Had a redeem
            if (oldToken instanceof Ticket && newToken instanceof Ticket)
            {
                Ticket oTick = (Ticket) oldToken;
                Ticket nTick = (Ticket) newToken;

                return oTick.isMatchedInXML() == nTick.isMatchedInXML();
            }

            //TODO: balance value gone stale

            return true;
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
            Token oldToken = value;
            Token newToken = (Token) other.value;

            return oldToken.getAddress().equals(newToken.getAddress()) && other.weight == weight;
        }
        else
        {
            return false;
        }

//        return other.viewType == TokenHolder.VIEW_TYPE
//                && ((TokenSortedItem) other).value.tokenInfo.address.equalsIgnoreCase(value.tokenInfo.address);
    }
}
