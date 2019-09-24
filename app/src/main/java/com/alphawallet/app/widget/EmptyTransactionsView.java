package com.alphawallet.app.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;

public class EmptyTransactionsView extends FrameLayout {

    public EmptyTransactionsView(@NonNull Context context, OnClickListener onClickListener) {
        super(context);

        LayoutInflater.from(getContext())
                .inflate(R.layout.layout_empty_transactions, this, true);

        findViewById(R.id.action_buy).setOnClickListener(onClickListener);
    }

    public void setNetworkInfo(NetworkInfo networkInfo) {
        if (networkInfo.isMainNetwork) {
            findViewById(R.id.action_buy).setVisibility(VISIBLE);
        } else {
            findViewById(R.id.action_buy).setVisibility(GONE);
        }
    }
}
