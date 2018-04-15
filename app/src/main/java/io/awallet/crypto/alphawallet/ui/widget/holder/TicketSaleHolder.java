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
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Created by James on 12/02/2018.
 */

public class TicketSaleHolder extends BaseTicketHolder
{
    public static final int VIEW_TYPE = 1071;

    private OnTokenCheckListener onTokenCheckListener;

    private final AppCompatRadioButton select;
    private final LinearLayout ticketLayout;

    public TicketSaleHolder(int resId, ViewGroup parent, AssetDefinition definition, Token token)
    {
        super(resId, parent, definition, token);
        ticketLayout = findViewById(R.id.layout_select);
        select = findViewById(R.id.radioBox);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        super.bind(data, addition);
        select.setVisibility(View.VISIBLE);

        select.setOnCheckedChangeListener(null); //have to invalidate listener first otherwise we trigger cached listener and create infinite loop
        select.setChecked(data.isChecked);

        select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                if (b)
                {
                    onTokenCheckListener.onTokenCheck(data);
                }
            }
        });

        ticketLayout.setOnClickListener(v -> {
            select.setChecked(true);
        });
    }

    public void setOnTokenCheckListener(OnTokenCheckListener onTokenCheckListener)
    {
        this.onTokenCheckListener = onTokenCheckListener;
    }
}
