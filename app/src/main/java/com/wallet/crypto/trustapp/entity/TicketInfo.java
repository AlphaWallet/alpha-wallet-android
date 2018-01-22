package com.wallet.crypto.trustapp.entity;


import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.repository.entity.RealmTokenInfo;
import com.wallet.crypto.trustapp.ui.AddTokenActivity;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModel;

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
    public void setupContent(ImageView icon, TextView symbol)
    {
        symbol.setText(this.name);
        icon.setImageResource(R.mipmap.ic_alpha);
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
        viewModel.showUseToken(context, name, venue, date, price, balance);
    }
}
