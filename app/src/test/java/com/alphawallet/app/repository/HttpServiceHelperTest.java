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

    //TODO: This test is currently worthless as we don't use Klaytn keys. Refactor to test the waterfall method
}
