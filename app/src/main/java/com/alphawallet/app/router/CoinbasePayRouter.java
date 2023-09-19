package com.alphawallet.app.router;

import android.app.Activity;
import android.content.Intent;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.CoinbasePayActivity;

public class CoinbasePayRouter
{
    /**
     * @param activity    - Calling activity
     * @param tokenSymbol - Token symbol of the asset you wish to purchase, e.g. "ETH", "USDC"
     */
    public void buyAsset(Activity activity, String tokenSymbol)
    {
        Intent intent = new Intent(activity, CoinbasePayActivity.class);
        intent.putExtra("asset", tokenSymbol);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    /**
     * @param activity   - Calling activity
     * @param blockchain - Select from supported chains from `CoinbasePayRepository.Blockchains`
     */
    public void buyFromSelectedChain(Activity activity, String blockchain)
    {
        Intent intent = new Intent(activity, CoinbasePayActivity.class);
        intent.putExtra("blockchain", blockchain);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    public void open(Activity activity)
    {
        Intent intent = new Intent(activity, CoinbasePayActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }
}
