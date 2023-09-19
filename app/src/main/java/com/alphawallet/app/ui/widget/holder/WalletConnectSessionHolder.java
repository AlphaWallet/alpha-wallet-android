package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.ui.WalletConnectNotificationActivity;

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
        container.setOnClickListener(view -> onClick());
    }

    private void onClick()
    {
        getContext().startActivity(new Intent(getContext(), WalletConnectNotificationActivity.class));
    }
}
