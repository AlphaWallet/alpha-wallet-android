package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;
import io.stormbird.wallet.entity.MarketplaceEvent;
import io.stormbird.wallet.ui.BrowseMarketActivity;

import static io.stormbird.wallet.C.Key.MARKETPLACE_EVENT;

/**
 * Created by James on 19/02/2018.
 */

public class MarketBrowseRouter
{
    public void open(Context context, MarketplaceEvent marketplaceEvent) {
        Intent intent = new Intent(context, BrowseMarketActivity.class);
        intent.putExtra(MARKETPLACE_EVENT, marketplaceEvent);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }
}
