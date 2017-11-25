package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by marat on 11/16/17.
 *
 */

/* Represents the following JSON
{
    "tokenInfo": {
        "address": "0x0cf0ee63788a0849fe5297f3407f701e122cc023",
        "name": "DATAcoin",
        "decimals": 18,
        "symbol": "DATA",
        "totalSupply": "987154514000000000000000000",
        "owner": "0x1bb7804d12fa4f70ab63d0bbe8cb0b1992694338",
        "lastUpdated": 1510851220,
        "totalIn": 2.165e+26,
        "totalOut": 2.165e+26,
        "issuancesCount": 0,
        "holdersCount": 433497,
        "price": false
    },
    "balance": 1.0541017210196e+18,
    "totalIn": 0,
    "totalOut": 0
}
*/

public class EPToken {

        @SerializedName("tokenInfo")
        private EPTokenInfo tokenInfo;

        @SerializedName("balance")
        private double balance;

        public EPTokenInfo getTokenInfo() {
                return tokenInfo;
        }

        public double getBalance() {
                return balance;
        }
}
