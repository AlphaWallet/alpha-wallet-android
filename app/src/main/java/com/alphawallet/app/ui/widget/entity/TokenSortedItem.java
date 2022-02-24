package com.alphawallet.app.ui.widget.entity;

import static com.alphawallet.app.repository.EthereumNetworkBase.getChainOrdinal;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;

import timber.log.Timber;

public class TokenSortedItem extends SortedItem<TokenCardMeta> {

    private boolean debugging = false;
    private double fiatValue = 0;
    public final int chainOrdinal;

    public TokenSortedItem(int viewType, TokenCardMeta value, long weight) {
        super(viewType, value, new TokenPosition(value.group, value.getChain(), weight));
        chainOrdinal = getChainOrdinal(value.getChain());
    }

    public void setFiatValue(double v)
    {
        fiatValue = v;
    }

    public void debug()
    {
        debugging = true;
    }

    @Override
    public int compare(SortedItem other)
    {
        if (other instanceof TokenSortedItem)
        {
            if (((TokenCardMeta)other.value).getUID() == value.getUID()) return 0; // must be equal
            else return weight.compare(other.weight, compareTokens(other));
        }
        else
        {
            return super.compare(other);
        }
    }

    private int compareTokens(SortedItem other)
    {
        TokenCardMeta otherTcm = ((TokenSortedItem)other).value;
        TokenSortedItem otherTsi = (TokenSortedItem) other;
        //compare tokens with old model
        //check value comparison
        if (value.getAddress().hashCode() == otherTcm.getAddress().hashCode())
        {
            return 0;
        }
        else if (value.isEthereum())
        {
            if (otherTcm.isEthereum()) return 0;
            else return -1; // always going to be before
        }
        else if (otherTcm.isEthereum())
        {
            return 1; //other will be first
        }
        else if (fiatValue > 0 || otherTsi.fiatValue > 0)
        {
            return Double.compare(otherTsi.fiatValue, fiatValue);
        }
        else if (weight.weighting != otherTsi.weight.weighting)
        {
            return Long.compare(weight.weighting, otherTsi.weight.weighting);
        }
        else //if weighting is the same, compare addresses if it's a Token item, otherwise it should be a match
        {
            //finally compare identical token names against address
            return Integer.compare(value.getAddress().hashCode(), otherTcm.getAddress().hashCode());
        }
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        if (viewType == newItem.viewType)
        {
            TokenCardMeta newToken = (TokenCardMeta) newItem.value;
            //if (!oldToken.tokenId.equalsIgnoreCase(newToken.tokenId)) return false;
            if (debugging) Timber.d("DEBUG: Contents: " + weight + " " + newItem.weight + " Balance: " + value.balance + " " + newToken.balance);
            if (weight != newItem.weight) return false;
            else return value.balance.equals(newToken.balance) && value.type.ordinal() == newToken.type.ordinal()
                    && value.lastUpdate == newToken.lastUpdate;
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
                if (oldToken == null || newToken == null) Timber.d("DEBUG: Item: One is null");
                else Timber.d("DEBUG: Item: " + oldToken.tokenId + " " + newToken.tokenId);
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
