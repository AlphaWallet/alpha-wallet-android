package com.alphawallet.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.alphawallet.app.entity.coinbasepay.DestinationWallet;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class CoinbasePayUtilsTest
{
    @Test
    public void dest_wallet_json_should_match()
    {
        List<String> assetList = Collections.singletonList("USDC");
        String json = CoinbasePayUtils.getDestWalletJson(DestinationWallet.Type.ASSETS, "0x1234", assetList);
        String expectedResult = "[{\"address\":\"0x1234\",\"assets\":[\"USDC\"]}]";
        assertThat(json, equalTo(expectedResult));
    }
}