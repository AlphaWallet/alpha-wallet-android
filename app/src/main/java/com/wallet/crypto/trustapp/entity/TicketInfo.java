package com.wallet.crypto.trustapp.entity;


import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;

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
        //For tickets use the venue title and switch to the ticket logo
        symbol.setText(this.name);
        icon.setImageResource(R.mipmap.ic_alpha);
    }
}
