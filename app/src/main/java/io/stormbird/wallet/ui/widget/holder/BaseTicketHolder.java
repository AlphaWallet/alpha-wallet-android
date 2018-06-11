package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.math.BigInteger;
import java.util.Locale;

import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;

import io.stormbird.wallet.ui.widget.OnTicketIdClickListener;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;

public class BaseTicketHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener
{
    private TicketRange thisData;
    private Ticket ticket;
    private OnTicketIdClickListener onTicketClickListener;
    private final TokenDefinition assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor

    private final TextView name;
    private final TextView count; // word choice: "amount" would imply the amount of money it costs
    private final TextView ticketDate;
    private final TextView ticketTime;
    private final TextView venue;
    private final TextView ticketText;
    private final TextView ticketCat;
    protected final TextView ticketRedeemed;
    private final TextView ticketDetails;
    protected final LinearLayout ticketDetailsLayout;
    protected final LinearLayout ticketLayout;

    public BaseTicketHolder(int resId, ViewGroup parent, TokenDefinition definition, Token ticket) {
        super(resId, parent);
        name = findViewById(R.id.name);
        count = findViewById(R.id.amount);
        venue = findViewById(R.id.venue);
        ticketDate = findViewById(R.id.date);
        ticketTime = findViewById(R.id.time);
        ticketText = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        ticketDetails = findViewById(R.id.ticket_details);
        itemView.setOnClickListener(this);
        ticketRedeemed = findViewById(R.id.redeemed);
        ticketDetailsLayout = findViewById(R.id.layout_ticket_details);
        ticketLayout = findViewById(R.id.layout_select);
        assetDefinition = definition;
        this.ticket = (Ticket)ticket;
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        DateFormat date = android.text.format.DateFormat.getLongDateFormat(getContext());
        date.setTimeZone(TimeZone.getTimeZone("Europe/Moscow")); // TODO: use the timezone defined in XML
        DateFormat time = android.text.format.DateFormat.getTimeFormat(getContext());
        time.setTimeZone(TimeZone.getTimeZone("Europe/Moscow")); // TODO: use the timezone defined in XML
        this.thisData = data;
        name.setText(ticket.tokenInfo.name);

        if (data.tokenIds.size() > 0) {
            BigInteger firstTokenId = data.tokenIds.get(0);
            String seatCount = String.format(Locale.getDefault(), "x%d", data.tokenIds.size());
            count.setText(seatCount);
            try {
                NonFungibleToken nonFungibleToken = new NonFungibleToken(firstTokenId, assetDefinition);
                String nameStr = assetDefinition.getTokenName();
                nameStr = nonFungibleToken.getAttribute("category").text;
                String venueStr = nonFungibleToken.getAttribute("venue").text;
                Date startTime = new Date(nonFungibleToken.getAttribute("time").value.longValue()*1000L);
                int cat = nonFungibleToken.getAttribute("category").value.intValue();

                name.setText(nameStr);
                count.setText(seatCount);
                venue.setText(venueStr);
                ticketDate.setText(date.format(startTime));
                ticketTime.setText(time.format(startTime));
                ticketText.setText(
                        nonFungibleToken.getAttribute("countryA").text + "-" +
                                nonFungibleToken.getAttribute("countryB").text
                );
                ticketCat.setText("M" + nonFungibleToken.getAttribute("match").text);
                ticketDetails.setText(
                        nonFungibleToken.getAttribute("locality").name + ": " +
                                nonFungibleToken.getAttribute("locality").text
                );
            } catch (NullPointerException e) {
                /* likely our XML token definition is outdated, just
                 * show raw data here. TODO: only the fields not
                 * understood are displayed as raw
                 */
                name.setText(firstTokenId.toString(16));
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (onTicketClickListener != null) {
            onTicketClickListener.onTicketIdClick(v, thisData);
        }
    }

    public void setOnTokenClickListener(OnTicketIdClickListener onTokenClickListener) {
        this.onTicketClickListener = onTokenClickListener;
    }
}
