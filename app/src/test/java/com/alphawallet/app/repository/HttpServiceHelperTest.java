package com.alphawallet.app.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowSystem;
import com.alphawallet.utils.ReflectionUtil;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.web3j.protocol.http.HttpService;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowSystem.class})
public class HttpServiceHelperTest
{
    private HttpService httpService;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        ReflectionUtil.setFinalFieldTo(EthereumNetworkBase.class, "usesProductionKey", true);
    }

    @Before
    public void setUp()
    {
        httpService = new HttpService();
    }

    @Test
    public void should_addRequiredCredentials_for_Klaytn_baobab() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(1001L, httpService, "klaytn-key");
        HashMap<String, String> headers = httpService.getHeaders();
        assertThat(headers.get("x-chain-id"), equalTo("1001"));
        assertThat(headers.get("Authorization"), equalTo("Basic klaytn-key"));
    }

    @Test
    public void should_addRequiredCredentials_for_KLAYTN() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(8217, httpService, "klaytn-key");
        HashMap<String, String> headers = httpService.getHeaders();
        assertThat(headers.get("x-chain-id"), equalTo("8217"));
        assertThat(headers.get("Authorization"), equalTo("Basic klaytn-key"));
    }

}
