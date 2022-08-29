package com.alphawallet.app.repository;

import android.net.Uri;
import android.text.TextUtils;

import com.alphawallet.app.entity.coinbasepay.DestinationWallet;
import com.alphawallet.app.util.CoinbasePayUtils;

import java.util.List;

public class CoinbasePayRepository implements CoinbasePayRepositoryType
{
    private static final String SCHEME = "https";
    private static final String AUTHORITY = "pay.coinbase.com";
    private static final String BUY_PATH = "buy";
    private static final String SELECT_ASSET_PATH = "select-asset";
    private final KeyProvider keyProvider = KeyProviderFactory.get();

    @Override
    public String getUri(DestinationWallet.Type type, String address, List<String> list)
    {
        String appId = keyProvider.getCoinbasePayAppId();
        if (TextUtils.isEmpty(appId))
        {
            return "";
        }
        else
        {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(SCHEME)
                    .authority(AUTHORITY)
                    .appendPath(BUY_PATH)
                    .appendPath(SELECT_ASSET_PATH)
                    .appendQueryParameter(RequestParams.APP_ID, keyProvider.getCoinbasePayAppId())
                    .appendQueryParameter(RequestParams.DESTINATION_WALLETS, CoinbasePayUtils.getDestWalletJson(type, address, list));

            return builder.build().toString();
        }
    }

    public static class Blockchains
    {
        public static final String ETHEREUM = "ethereum";
        public static final String SOLANA = "solana";
        public static final String AVALANCHE_C_CHAIN = "avalanche-c-chain";
    }

    private static class RequestParams
    {
        public static final String APP_ID = "appId";
        public static final String ADDRESS = "address";
        public static final String DESTINATION_WALLETS = "destinationWallets";
        public static final String ASSETS = "assets";
        public static final String BLOCKCHAINS = "blockchains";
    }
}
