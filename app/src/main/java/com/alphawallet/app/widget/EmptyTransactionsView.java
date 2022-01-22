package com.alphawallet.app.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;

public class EmptyTransactionsView extends FrameLayout {

    public EmptyTransactionsView(@NonNull Context context, OnClickListener onClickListener) {
        super(context);

        LayoutInflater.from(getContext())
                .inflate(R.layout.layout_empty_transactions, this, true);

        /*findViewById(R.id.action_buy).setOnClickListener(onClickListener);

        ((TextView)findViewById(R.id.no_transactions_subtext)).setText(context.getString(R.string.no_recent_transactions_subtext,
                                                                                         CustomViewSettings.primaryNetworkName()));

        Button buyButton = findViewById(R.id.action_buy);
        if (CustomViewSettings.primaryNetworkName().equals(C.ETHEREUM_NETWORK_NAME))
        {
            buyButton.setVisibility(VISIBLE);
            buyButton.setOnClickListener(((HomeActivity) context));
            buyButton.setText(context.getString(R.string.action_buy, CustomViewSettings.primaryNetworkName()));
        }
        else
        {
            buyButton.setVisibility(GONE);
        }*/
    }
}
