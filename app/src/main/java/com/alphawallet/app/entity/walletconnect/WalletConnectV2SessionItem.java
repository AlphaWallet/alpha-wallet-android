package com.alphawallet.app.entity.walletconnect;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.walletconnect.walletconnectv2.client.WalletConnect;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class WalletConnectV2SessionItem extends WalletConnectSessionItem implements Parcelable
{
    public WalletConnectV2SessionItem(WalletConnect.Model.SettledSession s)
    {
        super();
        name = Objects.requireNonNull(s.getPeerAppMetaData()).getName();
        url = Objects.requireNonNull(s.getPeerAppMetaData()).getUrl();
        icon = s.getPeerAppMetaData().getIcons().isEmpty() ? null : s.getPeerAppMetaData().getIcons().get(0);
        sessionId = s.getTopic();
        localSessionId = s.getTopic();
        chainId = getFirstChainId(s.getAccounts());
    }

    public WalletConnectV2SessionItem(Parcel in)
    {
        name = in.readString();
        url = in.readString();
        icon = in.readString();
        sessionId = in.readString();
        localSessionId = in.readString();
        chainId = in.readLong();
    }

    private long getFirstChainId(List<String> accounts)
    {
        Log.d("seaborn", accounts.get(0));
        return Long.parseLong(accounts.get(0).split(":")[1]);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(icon);
        dest.writeString(sessionId);
        dest.writeString(localSessionId);
        dest.writeLong(chainId);
    }

    public static final Parcelable.Creator<WalletConnectV2SessionItem> CREATOR
            = new Parcelable.Creator<WalletConnectV2SessionItem>() {
        public WalletConnectV2SessionItem createFromParcel(Parcel in) {
            return new WalletConnectV2SessionItem(in);
        }

        @Override
        public WalletConnectV2SessionItem[] newArray(int size)
        {
            return new WalletConnectV2SessionItem[0];
        }
    };
}