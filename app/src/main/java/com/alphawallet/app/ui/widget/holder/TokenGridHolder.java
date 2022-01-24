package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
    private final TokenIcon imageIcon;
    private final AssetDefinitionService assetDefinition;
    private final TokensService tokensService;

    private TokensAdapterCallback tokensAdapterCallback;

    public TokenGridHolder(int resId, ViewGroup parent, AssetDefinitionService assetService, TokensService tSvs) {
        super(resId, parent);

        clickLayer = findViewById(R.id.click_layer);
        imageIcon = findViewById(R.id.token_icon);
        name = findViewById(R.id.token_name);
        tokensService = tSvs;
        assetDefinition = assetService;
    }

    @Override
    public void bind(@Nullable TokenCardMeta tcm, @NonNull Bundle addition) {
        if (tcm != null) {
            Token token = tokensService.getToken(tcm.getChain(), tcm.getAddress());
            if (token == null) return; //TODO: Generate placeholder
            imageIcon.bindData(token, assetDefinition);
            name.setText(token.getFullName(assetDefinition, token.balance.intValue()));

            /*if (token.isERC721()) {
                ERC721Token tkn = (ERC721Token) token;
                Collection<NFTAsset> assets = tkn.getTokenAssets().values();
                if (assets != null && assets.size() > 0) {
                    NFTAsset firstAsset = assets.iterator().next();
                    if (firstAsset != null) {
                        Glide.with(getContext())
                                .load(firstAsset.getThumbnail())
                                .override(72)
                                .into(imageIcon);
                        name.setText(token.tokenInfo.name);
                        textIcon.setVisibility(View.GONE);
                        imageIcon.setVisibility(View.VISIBLE);
                    } else {
                        setupIcon(token);
                    }
                }
            } else if (token.isERC721Ticket()) {
                ERC721Ticket tkn = (ERC721Ticket) token;
                String tokenName = tkn.getTokenName(assetDefinition, 0);
                name.setText(tokenName);
                setupIcon(token);
            } else {
                name.setText(token.tokenInfo.name);
                setupIcon(token);
            }*/

            clickLayer.setOnClickListener(v -> {
                if (tokensAdapterCallback != null) {
                    tokensAdapterCallback.onTokenClick(v, token, null, true);
                }
            });
        }
    }

    /*private void setupIcon(@NotNull Token token) {
        imageIcon.setVisibility(View.GONE);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), Utils.getChainColour(token.tokenInfo.chainId)));
        textIcon.setText(Utils.getIconisedText(token.tokenInfo.name));
    }*/

    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback) {
        this.tokensAdapterCallback = tokensAdapterCallback;
    }
}