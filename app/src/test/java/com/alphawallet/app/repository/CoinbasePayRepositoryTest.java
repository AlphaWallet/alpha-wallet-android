package com.alphawallet.app.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.entity.coinbasepay.DestinationWallet;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class})
public class CoinbasePayRepositoryTest
{
    @Test
    public void uri_should_match() throws UnsupportedEncodingException
    {
        String appId = KeyProviderFactory.get().getCoinbasePayAppId();
        CoinbasePayRepository repository = new CoinbasePayRepository();
        String actualUri = repository.getUri(DestinationWallet.Type.ASSETS, "0x1234", Collections.singletonList("ETH"));
        String expectedEncodedUri = "https://pay.coinbase.com/buy/select-asset?appId=" + appId + "&destinationWallets=%5B%7B%22address%22%3A%220x1234%22%2C%22assets%22%3A%5B%22ETH%22%5D%7D%5D";
        assertThat(actualUri, equalTo(expectedEncodedUri));

        String decodedUri = URLDecoder.decode(actualUri, "UTF-8");
        String expectedDecodedUri = "https://pay.coinbase.com/buy/select-asset?appId=" + appId + "&destinationWallets=[{\"address\":\"0x1234\",\"assets\":[\"ETH\"]}]";
        assertThat(decodedUri, equalTo(expectedDecodedUri));
    }
}