package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.entity.Token;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.service.AssetDefinitionService;

/**
 * Created by James on 9/02/2018.
 */
public class TicketHolder extends BaseTicketHolder
{
    public static final int VIEW_TYPE = 1066;

    public TicketHolder(int resId, ViewGroup parent, Token ticket, AssetDefinitionService service)
    {
        super(resId, parent, ticket, service);
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        super.bind(data, addition);

        if (data.isBurned)
        {
            ticketRedeemed.setVisibility(View.VISIBLE);
        }
        else
        {
            ticketRedeemed.setVisibility(View.GONE);
        }

        ticketLayout.setOnClickListener(v -> {
            if (ticketDetailsLayout.getVisibility() == View.VISIBLE)
            {
                ticketDetailsLayout.setVisibility(View.GONE);
            }
            else
            {
                ticketDetailsLayout.setVisibility(View.VISIBLE);
            }
        });
    }
}
