package com.alphawallet.app.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.ui.HomeActivity;

public class EmptyTransactionsView extends FrameLayout {

    public EmptyTransactionsView(@NonNull Context context, OnClickListener onClickListener) {
        super(context);

        LayoutInflater.from(getContext())
                .inflate(R.layout.layout_empty_transactions, this, true);

        findViewById(R.id.action_buy).setOnClickListener(onClickListener);

        ((TextView)findViewById(R.id.no_transactions_subtext)).setText(context.getString(R.string.no_recent_transactions_subtext,
                                                                                         VisibilityFilter.primaryNetworkName()));

        Button buyButton = findViewById(R.id.action_buy);
        if (VisibilityFilter.primaryNetworkName().equals(C.ETHEREUM_NETWORK_NAME))
        {
            buyButton.setVisibility(VISIBLE);
            buyButton.setOnClickListener(((HomeActivity) context));
            buyButton.setText(context.getString(R.string.action_buy, VisibilityFilter.primaryNetworkName()));
        }
        else
        {
            buyButton.setVisibility(GONE);
        }
    }
}
