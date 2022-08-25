package com.alphawallet.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.alphawallet.app.entity.coinbasepay.DestinationWallet;
import com.alphawallet.app.repository.CoinbasePayRepository;

import org.junit.Test;

import java.util.List;

public class CoinbasePayUtilsTest
{
    @Test
    public void token_should_be_in_asset_list()
    {
        List<String> assetList = CoinbasePayUtils.getAssetList("ETH");
        assertThat(assetList.size(), equalTo(1));
        assertThat(assetList.get(0), equalTo("ETH"));
    }

    @Test
    public void network_should_be_in_blockchain_list()
    {
        List<String> blockchainList = CoinbasePayUtils.getBlockchainList(CoinbasePayRepository.Blockchains.ETHEREUM);
        assertThat(blockchainList.size(), equalTo(1));
        assertThat(blockchainList.get(0), equalTo(CoinbasePayRepository.Blockchains.ETHEREUM));
    }

    @Test
    public void dest_wallet_json_should_match()
    {
        List<String> assetList = CoinbasePayUtils.getAssetList("USDC");
        String json = CoinbasePayUtils.getDestWalletJson(DestinationWallet.Type.ASSETS, "0x1234", assetList);
        String expectedResult = "[{\"address\":\"0x1234\",\"assets\":[\"USDC\"]}]";
        assertThat(json, equalTo(expectedResult));
    }
}