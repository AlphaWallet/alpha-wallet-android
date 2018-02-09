package com.wallet.crypto.alphawallet.entity;


import android.content.Context;
import android.os.Parcel;
import android.view.View;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.ui.AddTokenActivity;
import com.wallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import com.wallet.crypto.alphawallet.viewmodel.TokensViewModel;

import java.util.List;

/**
 * Created by James on 20/01/2018.
 */

/**
 * Notes on what we need to represent in a ticket
 *
 * 1. Ticket info. Current contract is an int16, we can use this to start with.
 * 2. Venue stored in info, also gives date
 * 3. Date code in info together with venue
 * 4. Seat code band in info - gives price lookup
 *
 * //test spec for int16:
 * //16 venues/dates, first 4 bits
 * //left with 12 bits, can store 4096 seat/price zones
 */




public class TicketInfo extends TokenInfo implements TokenInterface
{
    public TicketInfo(TokenInfo ti)
    {
        super(ti.address, ti.name, ti.symbol, ti.decimals, ti.isEnabled);
    }

    private TicketInfo(Parcel in)
    {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeInt(decimals);
        dest.writeInt(isEnabled ? 1 : 0);
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
        //tokenHolder.symbol.setText(this.name);
        tokenHolder.fillIcon(null, R.mipmap.ic_alpha);
        tokenHolder.balanceEth.setVisibility(View.GONE);
        tokenHolder.balanceCurrency.setVisibility(View.GONE);
        tokenHolder.arrayBalance.setVisibility(View.VISIBLE);

        //String ids = populateIDs(((Ticket)(tokenHolder.token)).balanceArray, false);
        tokenHolder.arrayBalance.setText(String.valueOf(((Ticket)(tokenHolder.token)).balanceArray.size()) + " Tickets");
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
        layout.ticketLayout.setVisibility(View.GONE);
//        layout.venue.setText(TicketDecode.getVenue(tokenId));
//        layout.date.setText(TicketDecode.getDate(tokenId));
//        layout.price.setText(TicketDecode.getPriceString(tokenId));
        layout.isStormbird = true;
//        layout.tokenId = tokenId;
    }

    @Override
    public void clickReact(TokensViewModel viewModel, Context context, int balance, Token token)
    {
        viewModel.showUseToken(context, token);
    }
}
