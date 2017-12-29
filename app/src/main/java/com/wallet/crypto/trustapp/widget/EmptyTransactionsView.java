package com.wallet.crypto.trustapp.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.wallet.crypto.trustapp.R;

public class EmptyTransactionsView extends FrameLayout {
    public EmptyTransactionsView(@NonNull Context context, OnClickListener onClickListener) {
        super(context);

        LayoutInflater.from(getContext())
                .inflate(R.layout.layout_empty_transactions, this, true);

        findViewById(R.id.action_buy).setOnClickListener(onClickListener);
    }
}
