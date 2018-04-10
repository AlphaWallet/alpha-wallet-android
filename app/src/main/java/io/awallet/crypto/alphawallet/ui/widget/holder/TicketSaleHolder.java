package io.awallet.crypto.alphawallet.ui.widget.holder;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.TicketDecode;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenTicker;
import io.awallet.crypto.alphawallet.repository.AssetDefinition;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;
import io.awallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import io.awallet.crypto.alphawallet.ui.widget.OnTokenCheckListener;
import io.awallet.crypto.alphawallet.ui.widget.OnTokenClickListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Created by James on 12/02/2018.
 */

public class TicketSaleHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener
{
    public static final int VIEW_TYPE = 1071;

    private TicketRange thisData;
    private OnTicketIdClickListener onTicketClickListener;
    private OnTokenCheckListener onTokenCheckListener;

    public final AppCompatRadioButton select;
    public final LinearLayout ticketLayout;
    public final TextView name;
    public final TextView amount;
    public final TextView venue;
    public final TextView date;
    public final TextView ticketIds;
    public final TextView ticketCat;
    public final String tokenName;
    private final AssetDefinition assetDefinition;

    public TicketSaleHolder(int resId, ViewGroup parent, AssetDefinition definition, String contractName)
    {
        super(resId, parent);
        ticketLayout = findViewById(R.id.layout_select);
        name = findViewById(R.id.name);
        amount = findViewById(R.id.amount);
        venue = findViewById(R.id.venue);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        select = findViewById(R.id.radioBox);
        tokenName = contractName;
        assetDefinition = definition;
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        this.thisData = data;
        try
        {
            if (data.tokenIds.size() > 0)
            {
                Bytes32 firstTokenId = data.tokenIds.get(0);
                name.setText(tokenName);
                String seatCount = String.format(Locale.getDefault(), "x%d", data.tokenIds.size());
                NonFungibleToken nonFungibleToken = new NonFungibleToken(Numeric.toBigInt(firstTokenId.getValue()), assetDefinition);
                amount.setText(seatCount);
                venue.setText(nonFungibleToken.getAttribute("venue").text);
                date.setText(nonFungibleToken.getDate("dd - MMM"));
                ticketIds.setText(nonFungibleToken.getRangeStr(data));
                ticketCat.setText(nonFungibleToken.getAttribute("category").text);

//                int firstTokenId = data.tokenIds.get(0);
//                int seatStart = TicketDecode.getSeatIdInt(firstTokenId);
//                String seatRange = String.valueOf(seatStart);
//                if (data.tokenIds.size() > 1)
//                    seatRange = seatStart + "-" + (seatStart + (data.tokenIds.size() - 1));
//                String seatCount = String.format(Locale.getDefault(), "x%d", data.tokenIds.size());
//                name.setText(TicketDecode.getName());
//                amount.setText(seatCount);
//                venue.setText(TicketDecode.getVenue(firstTokenId));
//                date.setText(TicketDecode.getDate(firstTokenId));
//                ticketIds.setText(seatRange);
//                ticketCat.setText(TicketDecode.getZone(firstTokenId));
                select.setVisibility(View.VISIBLE);

                select.setOnCheckedChangeListener(null); //have to invalidate listener first otherwise we trigger cached listener and create infinite loop
                select.setChecked(thisData.isChecked);

                select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b)
                    {
                        if (b) {
                            onTokenCheckListener.onTokenCheck(thisData);
                        }
                    }
                });

                ticketLayout.setOnClickListener(v -> {
                    select.setChecked(true);
                });
            }
            else
            {
                fillEmpty();
            }
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    protected void fillEmpty()
    {
        name.setText(R.string.NA);
        venue.setText(R.string.NA);
    }

    @Override
    public void onClick(View v)
    {
        if (onTicketClickListener != null)
        {
            onTicketClickListener.onTicketIdClick(v, thisData);
        }
    }

    public void setOnTokenClickListener(OnTicketIdClickListener onTokenClickListener)
    {
        this.onTicketClickListener = onTokenClickListener;
    }

    public void setOnTokenCheckListener(OnTokenCheckListener onTokenCheckListener)
    {
        this.onTokenCheckListener = onTokenCheckListener;
    }
}
