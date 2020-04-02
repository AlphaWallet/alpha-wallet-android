package com.alphawallet.app.ui.widget.holder;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.Utils;

public class EventHolder extends BinderViewHolder<EventMeta> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1016;
    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";

    private final TextView date;
    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final TextView chainName;
    private final ImageView typeIcon;
    private final TextView supplimental;
    private final TokensService tokensService;
    private final ProgressBar pendingSpinner;
    private final RelativeLayout transactionBackground;
    private final FetchTransactionsInteract transactionsInteract;

    private String defaultAddress;

    public EventHolder(int resId, ViewGroup parent, TokensService service, FetchTransactionsInteract interact) {
        super(resId, parent);

        if (resId == R.layout.item_recent_transaction) {
            date = findViewById(R.id.transaction_date);
        } else {
            date = null;
        }
        typeIcon = findViewById(R.id.type_icon);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);
        chainName = findViewById(R.id.text_chain_name);
        supplimental = findViewById(R.id.supplimental);
        pendingSpinner = findViewById(R.id.pending_spinner);
        transactionBackground = findViewById(R.id.layout_background);
        tokensService = service;
        transactionsInteract = interact;

        typeIcon.setColorFilter(
                ContextCompat.getColor(getContext(), R.color.item_icon_tint),
                PorterDuff.Mode.SRC_ATOP);

        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable EventMeta data, @NonNull Bundle addition)
    {
        defaultAddress = addition.getString(DEFAULT_ADDRESS_ADDITIONAL);
        supplimental.setText("");

        //fetch data from database
        String hash = data.hash;
        //transaction = transactionsInteract.fetchCached(defaultAddress, hash);

        if (chainName != null)
        {
            Utils.setChainColour(chainName, data.chainId);
            chainName.setText(tokensService.getShortNetworkName(data.chainId));
            chainName.setVisibility(View.VISIBLE);
        }

        //now display eventtext
        typeIcon.setImageResource(R.drawable.ic_ethereum);
        typeIcon.setVisibility(View.VISIBLE);
        //show event text and time
        type.setText("Event");
        address.setText(data.eventDisplay);
        value.setText("");
        supplimental.setText("");
        pendingSpinner.setVisibility(View.GONE);
        if (transactionBackground != null) transactionBackground.setBackgroundResource(R.color.white);
    }

    @Override
    public void onClick(View v)
    {
        //So far nothing for event click
    }
}
