package com.alphawallet.app.di;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;
import com.alphawallet.shadows.ShadowKeyProviderFactoryNonProduction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactoryNonProduction.class})
public class EthereumNetworkBaseNonProductionTest
{
    @Test
    public void should_use_free_node_when_getNodeURLByNetworkId_in_non_production_mode()
    {
        assertThat(EthereumNetworkBase.getNodeURLByNetworkId(1L), equalTo("https://main-rpc.linkpool.io"));
    }
}
