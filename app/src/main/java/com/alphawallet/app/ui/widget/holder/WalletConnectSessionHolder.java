package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WalletConnectSessionHolder extends BinderViewHolder<Integer>
{
    public static final int VIEW_TYPE = 2024;
    private final ImageView icon;
    private final View container;

    public WalletConnectSessionHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);
        container = findViewById(R.id.layout_item_wallet_connect);
        icon = findViewById(R.id.logo);
    }

    @Override
    public void bind(Integer data, @NonNull Bundle addition)
    {
        if (data == 0)
        {
            container.setVisibility(View.GONE);
            return;
        }
        Glide.with(getContext())
                .load(C.WALLET_CONNECT_LOGO_URI)
                .circleCrop()
                .into(icon);
    }

}
