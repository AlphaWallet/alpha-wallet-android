package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.widget.TokenIcon;

public class TokenGridHolder extends BinderViewHolder<TokenCardMeta> {

    public static final int VIEW_TYPE = 2005;

    private final LinearLayout clickLayer;
    private final TextView name;
    private final TextView count;
    private final TokenIcon imageIcon;
    private final AssetDefinitionService assetDefinition;
    private final TokensService tokensService;

    private TokensAdapterCallback tokensAdapterCallback;

    public TokenGridHolder(int resId, ViewGroup parent, AssetDefinitionService assetService, TokensService tSvs) {
        super(resId, parent);

        RelativeLayout ll = findViewById(R.id.token_layout);
        clickLayer = findViewById(R.id.click_layer);
        imageIcon = findViewById(R.id.token_icon);
        ll.setClipToOutline(true);
        name = findViewById(R.id.token_name);
        count = findViewById(R.id.token_count);
        tokensService = tSvs;
        assetDefinition = assetService;
    }

    @Override
    public void bind(@Nullable TokenCardMeta tcm, @NonNull Bundle addition) {
        if (tcm != null) {
            Token token = tokensService.getToken(tcm.getChain(), tcm.getAddress());
            if (token == null) return; //TODO: Generate placeholder
            imageIcon.bindData(token, assetDefinition);
            name.setText(token.getName(assetDefinition, token.balance.intValue()));
            count.setText(getString(R.string.token_count, token.balance.intValue()));

            clickLayer.setOnClickListener(v -> {
                if (tokensAdapterCallback != null) {
                    tokensAdapterCallback.onTokenClick(v, token, null, true);
                }
            });
        }
    }

    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback) {
        this.tokensAdapterCallback = tokensAdapterCallback;
    }
}