package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.holder.TokenHolder;

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
            Token oldToken = (Token) value;
            Token newToken = (Token) newItem.value;

            if (!oldToken.getAddress().equals(newToken.getAddress())) return false;
            else if (!oldToken.getFullBalance().equals(newToken.getFullBalance())) return false;
            else if (!oldToken.getFullName().equals(newToken.getFullName())) return false;
            else if (oldToken.ticker == null && newToken.ticker != null) return false;

            //Had a redeem
            if (oldToken instanceof Ticket)
            {
                Ticket oTick = (Ticket) oldToken;
                Ticket nTick = (Ticket) newToken;
                if (!oTick.getBurnList().equals(nTick.getBurnList())) return false;
                else return oTick.isMatchedInXML() == nTick.isMatchedInXML();
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
            Token oldToken = (Token) value;
            Token newToken = (Token) other.value;

            if (!oldToken.getAddress().equals(newToken.getAddress())) return false;
            else return true;
        }
        else
        {
            return false;
        }

//        return other.viewType == TokenHolder.VIEW_TYPE
//                && ((TokenSortedItem) other).value.tokenInfo.address.equalsIgnoreCase(value.tokenInfo.address);
    }
}
