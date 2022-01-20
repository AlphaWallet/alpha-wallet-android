package com.alphawallet.app.ui.widget.entity;

import static com.alphawallet.app.repository.EthereumNetworkBase.getChainOrdinal;

import com.alphawallet.app.entity.tokendata.TokenGroup;

/**
 * Created by JB on 10/01/2022.
 */
public class TokenPosition
{
    //position consists of group, chain and weighting
    public final TokenGroup group;
    public final int chainOrdinal;
    public final long weighting;
    public final boolean isGroupHeader;
    public final boolean singleton;

    public TokenPosition(TokenGroup group, long chainId, long weighting, boolean isGroupHeader)
    {
        this.group = group;
        this.chainOrdinal = getChainOrdinal(chainId);
        this.weighting = weighting;
        this.isGroupHeader = isGroupHeader;
        this.singleton = false;
    }

    public TokenPosition(TokenGroup group, long chainId, long weighting)
    {
        this.group = group;
        this.chainOrdinal = getChainOrdinal(chainId);
        this.weighting = weighting;
        this.isGroupHeader = false;
        this.singleton = false;
    }

    public TokenPosition(long weighting)
    {
        this.group = TokenGroup.ASSET;
        this.chainOrdinal = 1;
        this.weighting = weighting;
        this.isGroupHeader = false;
        this.singleton = true;
    }

    public int compare(TokenPosition other)
    {
        return compare(other, Long.compare(weighting, other.weighting));
    }

    private int compareGroupHeader(TokenPosition other)
    {
        if (other.isGroupHeader)
        {
            return group.compareTo(other.group);
        }
        else
        {
            if (group != other.group)
            {
                return group.compareTo(other.group);
            }
            else
            {
                return -1;
            }
        }
    }

    public int compare(TokenPosition other, int valueCompare)
    {
        if (weighting == 0 || singleton || other.singleton) //zero weighting always at top; only compare weighting if one of the items is a singleton
        {
            return Long.compare(weighting, other.weighting);
        }
        else if (isGroupHeader)
        {
            return compareGroupHeader(other);
        }
        else //normal compare, use weighting
        {
            //first compare group headers
            if (group != other.group)
            {
                return Integer.compare(group.ordinal(), other.group.ordinal());
            }
            //next compare chain ordinals
            else if (chainOrdinal != other.chainOrdinal)
            {
                return Integer.compare(chainOrdinal, other.chainOrdinal);
            }
            else
            {
                return valueCompare;
            }
        }
    }
}
