package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.WalletConnectSessionActivity;

import androidx.annotation.NonNull;

public class WalletConnectSessionHolder extends BinderViewHolder<Integer>
{
    public static final int VIEW_TYPE = 2024;
    private final View container;

    public WalletConnectSessionHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);
        container = findViewById(R.id.layout_item_wallet_connect);
    }

    @Override
    public void bind(Integer data, @NonNull Bundle addition)
    {
        container.setOnClickListener(this::onClick);
    }

    private void onClick(View view)
    {
        Intent intent = new Intent(getContext(), WalletConnectSessionActivity.class);
        getContext().startActivity(intent);
    }
}
