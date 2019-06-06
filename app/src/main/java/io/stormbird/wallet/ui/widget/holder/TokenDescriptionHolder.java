package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;

/**
 * Created by James on 12/02/2018.
 */

public class TokenDescriptionHolder extends BinderViewHolder<Token>
{
    public static final int VIEW_TYPE = 1067;

    private final TextView count;
    private final TextView title;
    private final TextView issuerName;
    private final String issuer;
    private final AssetDefinitionService assetService;
    private final int assetCount;

    public TokenDescriptionHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService service, int tokenCount) {
        super(resId, parent);
        title = findViewById(R.id.name);
        count = findViewById(R.id.amount);
        issuerName = findViewById(R.id.textViewIssuer);
        assetService = service;
        if (service != null)
        {
            issuer = service.getIssuerName(t);
        }
        else
        {
            issuer = "";
        }
        assetCount = tokenCount;
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition) {
        count.setText(String.valueOf(assetCount));
        String tokenName = token.tokenInfo.name;
        if (assetService.getAssetDefinition(token.tokenInfo.chainId, token.getAddress()) != null)
        {
            String nameCandidate = assetService.getAssetDefinition(token.tokenInfo.chainId, token.getAddress()).getTokenName(token.getTicketCount());
            if (nameCandidate != null && nameCandidate.length() > 0) tokenName = nameCandidate;
        }
        title.setText(tokenName);
        issuerName.setText(issuer);
    }
}
