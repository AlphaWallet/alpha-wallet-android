package com.wallet.crypto.trustapp.entity;


import android.content.Context;
import android.os.Parcel;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.ui.AddTokenActivity;
import com.wallet.crypto.trustapp.ui.widget.holder.TokenHolder;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModel;

import org.web3j.abi.datatypes.generated.Uint16;

import java.util.List;

/**
 * Created by James on 20/01/2018.
 */

public class TicketInfo extends TokenInfo implements TokenInterface
{
    public final String venue;
    public final String date;
    public final double price;

    public TicketInfo(TokenInfo ti, String venue, String date, double price)
    {
        super(ti.address, ti.name, ti.symbol, ti.decimals, ti.isEnabled);
        this.venue = venue;
        this.date = date;
        this.price = price;
    }

    private TicketInfo(Parcel in)
    {
        super(in);
        this.venue = in.readString();
        this.date = in.readString();
        this.price = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeInt(decimals);
        dest.writeInt(isEnabled ? 1 : 0);
        dest.writeString(venue);
        dest.writeString(date);
        dest.writeDouble(price);
    }

    public static final Creator<TicketInfo> CREATOR = new Creator<TicketInfo>() {
        @Override
        public TicketInfo createFromParcel(Parcel in) {
            return new TicketInfo(in);
        }

        @Override
        public TicketInfo[] newArray(int size) {
            return new TicketInfo[size];
        }
    };

    @Override
    public void setupContent(TokenHolder tokenHolder)
    {
        tokenHolder.symbol.setText(this.name);
        tokenHolder.icon.setImageResource(R.mipmap.ic_alpha);
        tokenHolder.balanceEth.setVisibility(View.GONE);
        tokenHolder.arrayBalance.setVisibility(View.VISIBLE);

        String ids = populateIDs(((Ticket)(tokenHolder.token)).balanceArray, false);
        tokenHolder.arrayBalance.setText(ids);
    }

    @Override
    public String populateIDs(List<Integer> idArray, boolean keepZeros)
    {
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Integer id : idArray)
        {
            if (!keepZeros && id == 0) continue;
            if (!first)
            {
                sb.append(", ");
            }
            first = false;

            sb.append(id.toString());
            displayIDs = sb.toString();
        }

        return displayIDs;
    }

    @Override
    public void addTokenSetupPage(AddTokenActivity layout) {
        super.addTokenSetupPage(layout);
        layout.ticketLayout.setVisibility(View.VISIBLE);
        layout.venue.setText(venue);
        layout.date.setText(date);
        layout.price.setText(String.valueOf(price));
    }

//    @Override
//    public void storeRealmData(RealmTokenInfo obj) {
//        super.storeRealmData(obj);
//
//        obj.setVenue(venue);
//        obj.setDate(date);
//        obj.setPrice(price);
//    }

    public void clickReact(TokensViewModel viewModel, Context context, int balance, Token token)
    {
        viewModel.showUseToken(context, name, venue, date, address, price, balance, token);
    }
}
