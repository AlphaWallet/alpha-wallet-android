package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.TokenManagementActivity;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;
import com.alphawallet.app.ui.widget.entity.WarningData;

import static com.alphawallet.app.C.EXTRA_ADDRESS;

public class ManageTokensHolder extends BinderViewHolder<ManageTokensData> {
    public static final int VIEW_TYPE = 2015;

    LinearLayout layout;

    @Override
    public void bind(@Nullable ManageTokensData data, @NonNull Bundle addition) {
        layout.setOnClickListener(v -> {
            if (data.walletAddress != null) {
                Intent intent = new Intent(getContext(), TokenManagementActivity.class);
                intent.putExtra(EXTRA_ADDRESS, data.walletAddress);
                getContext().startActivity(intent);
            }
        });
    }

    public ManageTokensHolder(int res_id, ViewGroup parent) {
        super(res_id, parent);
        layout = findViewById(R.id.layout_manage_tokens);
    }
}
