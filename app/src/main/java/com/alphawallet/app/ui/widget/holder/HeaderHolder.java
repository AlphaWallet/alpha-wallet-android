package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokendata.TokenGroup;

public class HeaderHolder extends BinderViewHolder<TokenGroup> {
    public static final int VIEW_TYPE = 2022;

    private final TextView title;

    @Override
    public void bind(@Nullable TokenGroup data, @NonNull Bundle addition) {
        title.setText(groupToHeader(data));
    }

    private String groupToHeader(TokenGroup data)
    {
        if (data == null) return getString(R.string.assets);
        switch (data)
        {
            case ASSET:
            default:
                return getString(R.string.assets);
            case DEFI:
                return getString(R.string.defi_header);
            case GOVERNANCE:
                return getString(R.string.governance_header);
            case NFT:
                return getString(R.string.collectibles_header);
        }
    }

    public HeaderHolder(int res_id, ViewGroup parent) {
        super(res_id, parent);
        title = findViewById(R.id.title);
    }
}
