package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.token.entity.ContractInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by James on 2/02/2019.
 * Stormbird in Singapore
 */
public class ContractLocator implements Parcelable
{
    public final String address;
    public final long chainId;
    private final ContractType type;

    public ContractLocator(String n, long chainId)
    {
        this.address = n;
        this.chainId = chainId;
        this.type = ContractType.NOT_SET;
    }

    public ContractLocator(String n, long chainId, ContractType t)
    {
        this.address = n;
        this.chainId = chainId;
        this.type = t;
    }

    protected ContractLocator(Parcel in)
    {
        this.address = in.readString();
        this.chainId = in.readLong();
        this.type = ContractType.values()[in.readInt()];
    }

    public boolean equals(Token token)
    {
        return (token != null && address != null && chainId == token.tokenInfo.chainId && address.equalsIgnoreCase(token.getAddress()) );
    }

    public boolean equals(TokenCardMeta token)
    {
        return TokensRealmSource.databaseKey(chainId, address).equalsIgnoreCase(token.tokenId);
    }

    public static ContractLocator[] fromAddresses(String[] addresses, long chainID)
    {
        return Arrays.stream(addresses).map(s -> new ContractLocator(s, chainID)).toArray(ContractLocator[]::new);
    }

    public static List<ContractLocator> fromContractInfo(ContractInfo cInfo)
    {
        // public Map<Integer, List<String>> addresses = new HashMap<>();
        List<ContractLocator> retVal = new ArrayList<>();
        cInfo.addresses.keySet().forEach(chainId -> {
            cInfo.addresses.get(chainId).stream().map(addr -> new ContractLocator(addr, chainId)).forEach(retVal::add);
        });

        return retVal;
    }

    public static final Creator<ContractLocator> CREATOR = new Creator<ContractLocator>() {
        @Override
        public ContractLocator createFromParcel(Parcel in) {
            return new ContractLocator(in);
        }

        @Override
        public ContractLocator[] newArray(int size) {
            return new ContractLocator[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(address);
        dest.writeLong(chainId);
        dest.writeInt(type.ordinal());
    }
}
