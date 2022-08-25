package com.alphawallet.app.util;

import com.alphawallet.app.entity.coinbasepay.DestinationWallet;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CoinbasePayUtils
{
    public static String getDestWalletJson(DestinationWallet.Type type, String address, List<String> value)
    {
        List<DestinationWallet> destinationWallets = new ArrayList<>();
        destinationWallets.add(new DestinationWallet(type, address, value));
        return new Gson().toJson(destinationWallets);
    }
}
