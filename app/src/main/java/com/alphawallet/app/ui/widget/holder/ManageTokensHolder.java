package com.alphawallet.app.ui.widget.holder;

import static com.alphawallet.app.C.EXTRA_ADDRESS;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.TokenManagementActivity;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;

public class ManageTokensHolder extends BinderViewHolder<ManageTokensData> {
    public static final int VIEW_TYPE = 2015;

    LinearLayout layout;

    @Override
    public void bind(@Nullable ManageTokensData data, @NonNull Bundle addition) {
    }

    public ManageTokensHolder(int res_id, ViewGroup parent) {
        super(res_id, parent);
        layout = findViewById(R.id.layout_manage_tokens);
    }
}
