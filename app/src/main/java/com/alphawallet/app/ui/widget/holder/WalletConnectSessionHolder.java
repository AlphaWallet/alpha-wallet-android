package com.alphawallet.app.ui.widget.holder;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.ui.WalletConnectSessionActivity;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WalletConnectSessionHolder extends BinderViewHolder<List<WalletConnectSessionItem>>
{
    public static final int VIEW_TYPE = 2024;
    private final View container;

    public WalletConnectSessionHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);
        container = findViewById(R.id.layout_item_wallet_connect);
    }

    public void bind(@Nullable List<WalletConnectSessionItem> sessionItemList, @NonNull Bundle addition)
    {
        container.setOnClickListener(view -> onClick(sessionItemList));
    }

    private void onClick(List<WalletConnectSessionItem> sessions)
    {
        Context context = getContext();
        context.startActivity(WalletConnectSessionActivity.getIntent(sessions, context));
    }
}
