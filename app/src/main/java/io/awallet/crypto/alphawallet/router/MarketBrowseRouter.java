package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.MarketplaceEvent;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.ui.BrowseMarketActivity;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import static io.awallet.crypto.alphawallet.C.Key.MARKETPLACE_EVENT;
import static io.awallet.crypto.alphawallet.C.Key.TICKET;
import static io.awallet.crypto.alphawallet.C.Key.TICKET_RANGE;

/**
 * Created by James on 19/02/2018.
 */

public class MarketBrowseRouter
{
    public void open(Context context) {
        Intent intent = new Intent(context, BrowseMarketActivity.class);
        context.startActivity(intent);
    }

    public void open(Context context, MarketplaceEvent marketplaceEvent) {
        Intent intent = new Intent(context, BrowseMarketActivity.class);
        intent.putExtra(MARKETPLACE_EVENT, marketplaceEvent);
        context.startActivity(intent);
    }

    public void openRange(Context context, Ticket ticket, TicketRange range) {
        Intent intent = new Intent(context, BrowseMarketActivity.class);
        intent.putExtra(TICKET, ticket);
        intent.putExtra(TICKET_RANGE, range);
        context.startActivity(intent);
    }
}
