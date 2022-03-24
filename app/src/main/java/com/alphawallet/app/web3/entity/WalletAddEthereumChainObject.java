package com.alphawallet.app.web3.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.tools.Numeric;

import java.math.BigInteger;

import timber.log.Timber;

/**
 * Created by JB on 28/07/21
 */

public class WalletAddEthereumChainObject implements Parcelable
{
    public NativeCurrency nativeCurrency;
    public String[] blockExplorerUrls;
    public String chainName;
    public String chainType; //ignore this
    public String chainId; //this is a hex number with "0x" prefix. If it is without "0x", process it as dec
    public String[] rpcUrls;

    public WalletAddEthereumChainObject()
    {
    }

    public long getChainId()
    {
        try
        {
            if (Numeric.containsHexPrefix(chainId))
            {
                return Numeric.toBigInt(chainId).longValue();
            }
            else
            {
                return new BigInteger(chainId).longValue();
            }
        }
        catch (NumberFormatException e)
        {
            Timber.e(e);
            return (0);
        }
    }

    protected WalletAddEthereumChainObject(Parcel in)
    {
        nativeCurrency = NativeCurrency.CREATOR.createFromParcel(in);
        blockExplorerUrls = in.readInt() == 1 ? in.createStringArray() : null;
        chainName = in.readString();
        chainId = in.readString();
        rpcUrls = in.readInt() == 1 ? in.createStringArray() : null;
    }

    public static final Creator<WalletAddEthereumChainObject> CREATOR = new Creator<WalletAddEthereumChainObject>()
    {
        @Override
        public WalletAddEthereumChainObject createFromParcel(Parcel in)
        {
            return new WalletAddEthereumChainObject(in);
        }

        @Override
        public WalletAddEthereumChainObject[] newArray(int size)
        {
            return new WalletAddEthereumChainObject[size];
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
        nativeCurrency.writeToParcel(dest, PARCELABLE_WRITE_RETURN_VALUE);
        dest.writeInt(blockExplorerUrls == null ? 0 : 1);
        if (blockExplorerUrls != null)
        {
            dest.writeStringArray(blockExplorerUrls);
        }
        dest.writeString(chainName);
        dest.writeString(chainId);
        dest.writeInt(rpcUrls == null ? 0 : 1);
        if (rpcUrls != null)
        {
            dest.writeStringArray(rpcUrls);
        }
    }
}
