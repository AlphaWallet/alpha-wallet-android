package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.TokenManagementActivity;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;

import static com.alphawallet.app.C.EXTRA_ADDRESS;

public class HeaderHolder extends BinderViewHolder<String> {
    public static final int VIEW_TYPE = 2022;

    TextView title;

    @Override
    public void bind(@Nullable String data, @NonNull Bundle addition) {
        title.setText(data);
    }

    public HeaderHolder(int res_id, ViewGroup parent) {
        super(res_id, parent);
        title = findViewById(R.id.title);
    }
}
