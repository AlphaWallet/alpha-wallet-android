package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public TokenDescriptionHolder(int resId, ViewGroup parent, Ticket t, AssetDefinitionService service) {
        super(resId, parent);
        title = findViewById(R.id.name);
        count = findViewById(R.id.amount);
        issuerName = findViewById(R.id.textViewIssuer);
        if (service != null)
        {
            issuer = service.getIssuerName(t.getAddress());
        }
        else
        {
            issuer = "";
        }
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition) {
        count.setText(String.valueOf(token.getTicketCount()));
        title.setText(token.tokenInfo.name);
        issuerName.setText(issuer);
    }
}
