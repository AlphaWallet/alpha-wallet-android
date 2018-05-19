package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.entity.MarketplaceEvent;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.ui.BrowseMarketActivity;
import io.stormbird.wallet.ui.widget.entity.TicketRangeParcel;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.wallet.C.Key.MARKETPLACE_EVENT;
import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.Key.TICKET_RANGE;

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
        TicketRangeParcel parcel = new TicketRangeParcel(range);
        Intent intent = new Intent(context, BrowseMarketActivity.class);
        intent.putExtra(TICKET, ticket);
        intent.putExtra(TICKET_RANGE, parcel);
        context.startActivity(intent);
    }
}
