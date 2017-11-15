package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by marat on 11/13/17.
{
    "id": "ethereum",
    "name": "Ethereum",
    "symbol": "ETH",
    "rank": "2",
    "price_usd": "314.862",
    "price_btc": "0.0474923",
    "24h_volume_usd": "1328580000.0",
    "market_cap_usd": "30133198205.0",
    "available_supply": "95702874.0",
    "total_supply": "95702874.0",
    "max_supply": null,
    "percent_change_1h": "-0.1",
    "percent_change_24h": "1.87",
    "percent_change_7d": "4.21",
    "last_updated": "1510589050"
}
*/

public class CMTicker {
    @SerializedName("price_usd")
    private String priceUsd;

    public String getPriceUsd() {
        return priceUsd;
    }

}
