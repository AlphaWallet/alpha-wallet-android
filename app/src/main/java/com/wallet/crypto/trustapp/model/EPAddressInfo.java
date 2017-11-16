package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by marat on 11/16/17.
 */

/* This represents the following json
{
	"address": "0x0122374ddd61ebdbe487f27225c8d55a96688714",
	"ETH": {
		"balance": 0.0081799448,
		"totalIn": 23.9400569448,
		"totalOut": 23.931877
	},
	"countTxs": 16,
	"tokens": [{
		"tokenInfo": {
			"address": "0xab95e915c123fded5bdfb6325e35ef5515f1ea69",
			"name": "XENON",
			"decimals": 18,
			"symbol": "XNN",
			"totalSupply": "1000000000000000000000000000",
			"owner": "0x",
			"lastUpdated": 1510850050,
			"issuancesCount": 0,
			"holdersCount": 756611,
			"description": "XenonNetwork (http://xenon.network/), an enterprise-scale blockchain launching in July 2018 begins a massive distribution of their native Xenon (XNN) ERC-20 compatible tokens to over 400,000 active ethereum addresses at the beginning of October. In addition to this, a similar distribution to bitcoin holders will occur in November, followed by a proof-of-individuality public token distribution from November through to June 2018.\n\nhttp://xenon.network",
			"price": false
		},
		"balance": 6.9576614445292e+20,
		"totalIn": 0,
		"totalOut": 0
	}, {
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
	}, {
		"tokenInfo": {
			"address": "0xd0a4b8946cb52f0661273bfbc6fd0e0c75fc6433",
			"name": "Storm Token",
			"decimals": "18",
			"symbol": "STORM",
			"totalSupply": "8422653913958765221271563784",
			"owner": "0x00250bf60e31c4ec7e8c04bcca4af8e294306e25",
			"lastUpdated": 1510845529,
			"issuancesCount": 0,
			"holdersCount": 974,
			"price": false
		},
		"balance": 2.156e+22,
		"totalIn": 0,
		"totalOut": 0
	}, {
		"tokenInfo": {
			"address": "0x519475b31653e46d20cd09f9fdcf3b12bdacb4f5",
			"name": "VIU",
			"decimals": "18",
			"symbol": "VIU",
			"totalSupply": "1000000000000000000000000000",
			"owner": "0x",
			"lastUpdated": 1510851386,
			"issuancesCount": 0,
			"holdersCount": 441605,
			"price": false
		},
		"balance": 1.984985648e+20,
		"totalIn": 0,
		"totalOut": 0
	}]
}
*/

public class EPAddressInfo {
    @SerializedName("tokens")
    private List<EPToken> tokens;

    public List<EPToken> getTokens() {
        return tokens;
    }
}
