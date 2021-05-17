package com.alphawallet.app.entity.tokens;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.service.AssetDefinitionService;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by JB on 12/07/2020.
 */
public class TokenCardMeta implements Comparable<TokenCardMeta>, Parcelable
{
    public final String tokenId;
    public long lastUpdate;
    public long lastTxUpdate;
    public final int nameWeight;
    public final ContractType type;
    public final String balance;

    /*
    Initial value is False as Token considered to be Hidden
     */
    public boolean isEnabled = false;

    public TokenCardMeta(int chainId, String tokenAddress, String balance, long timeStamp, AssetDefinitionService svs, String name, String symbol, ContractType type)
    {
        this.tokenId = TokensRealmSource.databaseKey(chainId, tokenAddress);
        this.lastUpdate = timeStamp;
        this.type = type;
        this.nameWeight = calculateTokenNameWeight(chainId, tokenAddress, svs, name, symbol, isEthereum());
        this.balance = balance;
    }

    public TokenCardMeta(int chainId, String tokenAddress, String balance, long timeStamp, long lastTxUpdate, ContractType type)
    {
        this.tokenId = TokensRealmSource.databaseKey(chainId, tokenAddress);
        this.lastUpdate = timeStamp;
        this.lastTxUpdate = lastTxUpdate;
        this.type = type;
        this.nameWeight = 1000;
        this.balance = balance;
    }

    public TokenCardMeta(Token token)
    {
        this.tokenId = TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.getAddress());
        this.lastUpdate = token.updateBlancaTime;
        this.lastTxUpdate = token.lastTxCheck;
        this.type = token.getInterfaceSpec();
        this.nameWeight = 1000;
        this.balance = token.balance.toString();
    }

    protected TokenCardMeta(Parcel in)
    {
        tokenId = in.readString();
        lastUpdate = in.readLong();
        lastTxUpdate = in.readLong();
        nameWeight = in.readInt();
        type = ContractType.values()[in.readInt()];
        balance = in.readString();
    }

    public static final Creator<TokenCardMeta> CREATOR = new Creator<TokenCardMeta>() {
        @Override
        public TokenCardMeta createFromParcel(Parcel in) {
            return new TokenCardMeta(in);
        }

        @Override
        public TokenCardMeta[] newArray(int size) {
            return new TokenCardMeta[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(tokenId);
        dest.writeLong(lastUpdate);
        dest.writeLong(lastTxUpdate);
        dest.writeInt(nameWeight);
        dest.writeInt(type.ordinal());
        dest.writeString(balance);
    }

    public String getAddress()
    {
        return tokenId.substring(0, tokenId.indexOf('-'));
    }

    public boolean hasPositiveBalance()
    {
        return !balance.equals("0");
    }

    public int getChain()
    {
        int chainPos = tokenId.lastIndexOf('-') + 1;
        if (chainPos < tokenId.length())
        {
            String chainStr = tokenId.substring(chainPos);
            return Integer.parseInt(chainStr);
        }
        else
        {
            return MAINNET_ID;
        }
    }

    public static int calculateTokenNameWeight(int chainId, String tokenAddress, AssetDefinitionService svs, String tokenName, String symbol, boolean isEth)
    {
        int weight = 1000; //ensure base eth types are always displayed first
        String name = svs != null ? svs.getTokenName(chainId, tokenAddress, 1) : null;
        if (name != null)
        {
            name += symbol;
        }
        else
        {
            name = tokenName + symbol;
        }

        if (chainId == EthereumNetworkRepository.getOverrideToken().chainId && tokenAddress.equalsIgnoreCase(EthereumNetworkRepository.getOverrideToken().address))
        {
            return 1;
        }
        else if (isEth)
        {
            return chainId + 1;
        }

        if (TextUtils.isEmpty(name)) return Integer.MAX_VALUE;

        int i = 4;
        int pos = 0;

        while (i >= 0 && pos < name.length())
        {
            char c = name.charAt(pos++);
            int w = tokeniseCharacter(c);
            if (w > 0)
            {
                int component = (int)Math.pow(26, i)*w;
                weight += component;
                i--;
            }
        }

        String address = com.alphawallet.token.tools.Numeric.cleanHexPrefix(tokenAddress);
        for (i = 0; i < address.length() && i < 2; i++)
        {
            char c = address.charAt(i);
            int w = c - '0';
            weight += w;
        }

        if (weight < 2) weight = 2;

        return weight;
    }

    private static int tokeniseCharacter(char c)
    {
        int w = Character.toLowerCase(c) - 'a' + 1;
        if (w > 'z')
        {
            //could be ideographic, in which case we may want to display this first
            //just use a modulus
            w = w % 10;
        }
        else if (w < 0)
        {
            //must be a number
            w = 1 + (c - '0');
        }
        else
        {
            w += 10;
        }

        return w;
    }

    @Override
    public int compareTo(@NonNull TokenCardMeta otherToken)
    {
        return nameWeight - otherToken.nameWeight;
    }

    public boolean isEthereum()
    {
        return type == ContractType.ETHEREUM;
    }

    public boolean isNFT()
    {
        switch (type)
        {
            case NOT_SET:
            case OTHER:
            case ETHEREUM:
            case CURRENCY:
            case CREATION:
            case DELETED_ACCOUNT:
            case ERC20:
            default:
                return false;
            case ERC721:
            case ERC875_LEGACY:
            case ERC875:
            case ERC721_LEGACY:
            case ERC721_TICKET:
            case ERC721_UNDETERMINED:
                return true;
        }
    }

    public float calculateBalanceUpdateWeight()
    {
        float updateWeight = 0;
        //calculate balance update time
        if (nameWeight < Integer.MAX_VALUE)
        {
            if (type == ContractType.ERC721 || type == ContractType.ERC721_LEGACY || type == ContractType.ERC721_TICKET)
            {
                //ERC721 types which usually get their balance from opensea. Still need to check the balance for stale tokens to spot a positive -> zero balance transition
                updateWeight = 0.3f; // 100 seconds
            }
            else if (EthereumNetworkRepository.hasRealValue(getChain()))
            {
                if (isEthereum() || hasPositiveBalance())
                {
                    updateWeight = 1.0f; //30 seconds
                }
                else
                {
                    updateWeight = 0.5f; //1 minute
                }
            }
            else
            {
                //testnet: TODO: check time since last transaction - if greater than 1 month slow update further
                if (isEthereum())
                {
                    updateWeight = 0.5f; //1 minute
                }
                else if (hasPositiveBalance())
                {
                    updateWeight = 0.3f; //100 seconds
                }
                else
                {
                    updateWeight = 0.1f; //5 minutes
                }
            }
        }
        return updateWeight;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public long getUID()
    {
        return tokenId.hashCode();
    }

    public boolean equals(TokenCardMeta other)
    {
        return (tokenId.equalsIgnoreCase(other.tokenId));
    }
}
