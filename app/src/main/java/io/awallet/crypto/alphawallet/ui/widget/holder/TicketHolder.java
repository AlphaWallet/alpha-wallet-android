package io.awallet.crypto.alphawallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.repository.AssetDefinition;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

/**
 * Created by James on 9/02/2018.
 */
public class TicketHolder extends BaseTicketHolder
{
    public static final int VIEW_TYPE = 1066;

    public TicketHolder(int resId, ViewGroup parent, AssetDefinition definition, Token ticket)
    {
        super(resId, parent, definition, ticket);
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
