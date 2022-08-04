package com.alphawallet.app.repository;

import static org.mockito.Mockito.verify;

import com.alphawallet.utils.ReflectionUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.web3j.protocol.http.HttpService;

@RunWith(PowerMockRunner.class)
public class HttpServiceHelperTest
{
    @Mock
    private HttpService httpService;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        ReflectionUtil.setFinalFieldTo(EthereumNetworkBase.class, "usesProductionKey", true);
    }

    @Test
    public void should_addRequiredCredentials_for_Klaytn_baobab() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(1001L, httpService, "klaytn-key");
        verify(httpService).addHeader("x-chain-id", "1001");
        verify(httpService).addHeader("Authorization", "Basic klaytn-key");
    }

    @Test
    public void should_addRequiredCredentials_for_Klaytn() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(8217, httpService, "klaytn-key");
        verify(httpService).addHeader("x-chain-id", "8217");
        verify(httpService).addHeader("Authorization", "Basic klaytn-key");
    }

}
