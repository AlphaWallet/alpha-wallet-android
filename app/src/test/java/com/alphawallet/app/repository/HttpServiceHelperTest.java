package com.alphawallet.app.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.shadows.ShadowApp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.web3j.protocol.http.HttpService;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class})
public class HttpServiceHelperTest
{
    private final HttpService httpService = new HttpService();

    @Test
    public void should_addRequiredCredentials_for_Klaytn_baobab() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(1001L, httpService, "klaytn-key", true);
        HashMap<String, String> headers = httpService.getHeaders();
        assertThat(headers.get("x-chain-id"), equalTo("1001"));
        assertThat(headers.get("Authorization"), equalTo("Basic klaytn-key"));
    }

    @Test
    public void should_addRequiredCredentials_for_KLAYTN() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(8217, httpService, "klaytn-key", true);
        HashMap<String, String> headers = httpService.getHeaders();
        assertThat(headers.get("x-chain-id"), equalTo("8217"));
        assertThat(headers.get("Authorization"), equalTo("Basic klaytn-key"));
    }

    @Test
    public void should_not_addRequiredCredentials_for_KLAYTN_when_not_use_production_key() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(8217, httpService, "klaytn-key", false);
        HashMap<String, String> headers = httpService.getHeaders();
        assertFalse(headers.containsKey("x-chain-id"));
        assertFalse(headers.containsKey("Authorization"));
    }

    @Test
    public void should_not_addRequiredCredentials_for_non_KLAYTN_chain() throws Exception
    {
        HttpServiceHelper.addRequiredCredentials(1, httpService, "klaytn-key", false);
        HashMap<String, String> headers = httpService.getHeaders();
        assertFalse(headers.containsKey("x-chain-id"));
        assertFalse(headers.containsKey("Authorization"));
    }
}
