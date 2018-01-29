package com.wallet.crypto.trustapp.entity;


import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.repository.entity.RealmTokenInfo;
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
        super(ti.address, ti.name, ti.symbol, ti.decimals);
        this.venue = venue;
        this.date = date;
        this.price = price;
    }

    @Override
    public void setupContent(TokenHolder tokenHolder)
    {
        tokenHolder.symbol.setText(this.name);
        tokenHolder.icon.setImageResource(R.mipmap.ic_alpha);
        tokenHolder.balance.setVisibility(View.GONE);
        tokenHolder.arrayBalance.setVisibility(View.VISIBLE);

        String ids = populateIDs(((Ticket)(tokenHolder.token)).balanceArray, false);
        tokenHolder.arrayBalance.setText(ids);
    }

    @Override
    public String populateIDs(List<Uint16> idArray, boolean keepZeros)
    {
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Uint16 id : idArray)
        {
            if (!keepZeros && id.getValue().intValue() == 0) continue;
            if (!first)
            {
                sb.append(", ");
            }
            first = false;

            Integer value = id.getValue().intValue();
            sb.append(value.toString());
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

    @Override
    public void storeRealmData(RealmTokenInfo obj) {
        super.storeRealmData(obj);

        obj.setVenue(venue);
        obj.setDate(date);
        obj.setPrice(price);
    }

    public void clickReact(TokensViewModel viewModel, Context context, int balance)
    {
        viewModel.showUseToken(context, name, venue, date, address, price, balance);
    }
}
