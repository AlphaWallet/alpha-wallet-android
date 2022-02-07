package com.alphawallet.app.entity.walletconnect;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.walletconnect.walletconnectv2.client.WalletConnect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.RequiresApi;

public class WalletConnectV2SessionItem extends WalletConnectSessionItem implements Parcelable
{
    public final List<String> chains = new ArrayList<>();
    public final List<String> accounts = new ArrayList<>();
    public final List<String> methods = new ArrayList<>();
    public WalletConnectV2SessionItem(WalletConnect.Model.SettledSession s)
    {
        super();
        name = Objects.requireNonNull(s.getPeerAppMetaData()).getName();
        url = Objects.requireNonNull(s.getPeerAppMetaData()).getUrl();
        icon = s.getPeerAppMetaData().getIcons().isEmpty() ? null : s.getPeerAppMetaData().getIcons().get(0);
        sessionId = s.getTopic();
        localSessionId = s.getTopic();
        accounts.addAll(s.getAccounts());
        methods.addAll(s.getPermissions().getJsonRpc().getMethods());
    }

    public WalletConnectV2SessionItem(Parcel in)
    {
        name = in.readString();
        url = in.readString();
        icon = in.readString();
        sessionId = in.readString();
        localSessionId = in.readString();
        in.readStringList(chains);
        in.readStringList(accounts);
        in.readStringList(methods);
    }

    public WalletConnectV2SessionItem()
    {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static WalletConnectV2SessionItem from(WalletConnect.Model.SessionProposal sessionProposal)
    {
        WalletConnectV2SessionItem item = new WalletConnectV2SessionItem();
        item.name = sessionProposal.getName();
        item.url = sessionProposal.getUrl();
        item.icon = sessionProposal.getIcon();
        item.sessionId = sessionProposal.getTopic();
        item.chains.addAll(sessionProposal.getChains());
        item.methods.addAll(sessionProposal.getMethods());
        return item;
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
        dest.writeStringList(chains);
        dest.writeStringList(accounts);
        dest.writeStringList(methods);
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