package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTokenManageClickListener;
import com.alphawallet.app.widget.TokenIcon;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class TokenListHolder extends BinderViewHolder<TokenCardMeta> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    final RelativeLayout layout;
    final TextView tokenName;
    final SwitchMaterial switchEnabled;
    final View overlay;
    final TokenIcon tokenIcon;
    long chainId;

    //need to cache this locally, unless we cache every string we need in the constructor
    private final AssetDefinitionService assetDefinition;
    private final TokensService tokensService;

    public Token token;
    public TokenCardMeta data;
    public int position;
    private OnTokenManageClickListener onTokenClickListener;

    public TokenListHolder(int resId, ViewGroup parent, AssetDefinitionService assetService, TokensService tokensService)
    {
        super(resId, parent);

        this.assetDefinition = assetService;
        this.tokensService = tokensService;

        layout = itemView.findViewById(R.id.layout_list_item);
        tokenName = itemView.findViewById(R.id.name);
        switchEnabled = itemView.findViewById(R.id.switch_enabled);
        overlay = itemView.findViewById(R.id.view_overlay);
        tokenIcon = itemView.findViewById(R.id.token_icon);

        layout.setOnClickListener(this);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TokenCardMeta data, @NonNull Bundle addition)
    {
        this.data = data;
        position = addition.getInt("position");
        token = tokensService.getToken(data.getChain(), data.getAddress());

        if (token == null)
        {
            bindEmptyText(data);
            return;
        }

        tokenName.setText(token.getFullName(assetDefinition, 1));
        chainId = token.tokenInfo.chainId;
        switchEnabled.setOnCheckedChangeListener(null);
        switchEnabled.setChecked(data.isEnabled);
        switchEnabled.setOnCheckedChangeListener(this);
        tokenIcon.bindData(token, assetDefinition);

        if (data.isEnabled)
        {
            overlay.setVisibility(View.GONE);
        }
        else
        {
            overlay.setVisibility(View.VISIBLE);
        }
    }

    private void bindEmptyText(TokenCardMeta data)
    {
        tokenIcon.setVisibility(View.GONE);
        tokenName.setText(data.balance);
        switchEnabled.setOnCheckedChangeListener(null);
        switchEnabled.setChecked(data.isEnabled);
        switchEnabled.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        switchEnabled.setChecked(!switchEnabled.isChecked());
    }

    public void setOnTokenClickListener(OnTokenManageClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
    {
        if (onTokenClickListener != null)
        {
            onTokenClickListener.onTokenClick(token, position, isChecked);
        }
    }
}