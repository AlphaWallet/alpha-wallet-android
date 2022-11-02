package com.alphawallet.app.di;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class})
public class EthereumNetworkBaseTest
{
    @Test
    public void should_getNodeURLByNetworkId_when_use_production_key()
    {
        assertThat(EthereumNetworkBase.getNodeURLByNetworkId(61L), equalTo("https://www.ethercluster.com/etc"));
        assertThat(EthereumNetworkBase.getNodeURLByNetworkId(100L), equalTo("https://rpc.ankr.com/gnosis"));
    }

    @Test
    public void should_construct_infura_url_when_getNodeURLByNetworkId_given_production_key()
    {
        assertThat(EthereumNetworkBase.getNodeURLByNetworkId(1L), equalTo("https://mainnet.infura.io/v3/fake-key-for-testing"));
        assertThat(EthereumNetworkBase.getNodeURLByNetworkId(3L), equalTo("https://rpc.ankr.com/eth_ropsten"));
    }

    @Test
    public void should_get_main_net_url_when_getNodeURLByNetworkId_given_not_existed_network_id()
    {
        assertThat(EthereumNetworkBase.getNodeURLByNetworkId(Integer.MAX_VALUE), equalTo("https://mainnet.infura.io/v3/fake-key-for-testing"));
    }
}
