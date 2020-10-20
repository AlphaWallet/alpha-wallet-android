package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;

/**
 * Created by James on 13/02/2018.
 */

public class SalesOrderHeaderHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1068;

    private final TextView title;
    private final AssetDefinitionService assetService;

    public SalesOrderHeaderHolder(int resId, ViewGroup parent, AssetDefinitionService service) {
        super(resId, parent);
        title = findViewById(R.id.name);
        assetService = service;
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition)
    {
        String typeName = assetService.getTokenName(token.tokenInfo.chainId, token.tokenInfo.address, 2);
        title.setText(getString(R.string.select_tickets, typeName != null ? typeName : getString(R.string.tickets)));
    }
}
