package com.alphawallet.app;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import android.os.Bundle;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;

import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.testdoubles.FakeAndroidKeyStore;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.shadows.ShadowLibraryHelper;
import com.alphawallet.shadows.ShadowRealm;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;

@RunWith(RobolectricTestRunner.class)
@HiltAndroidTest
@Config(application = HiltTestApplication.class, shadows = {ShadowRealm.class, ShadowLibraryHelper.class})
public class SendActivityTest
{
    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Before
    public void setUp() throws Exception
    {
        FakeAndroidKeyStore.INSTANCE.getSetup();
        hiltRule.inject();
    }

    @Inject
    TokensService tokensService;

    @Test
    @Config(sdk = 28)
    public void should_scan_invalid_qr_code()
    {
        assertNotNull(tokensService);
        tokensService.setCurrentAddress("0x1234567890123456789012345678901234567890");

        Bundle bundle = new Bundle();
        bundle.putString(C.EXTRA_NETWORKID, String.valueOf(MAINNET_ID));
        bundle.putString(C.EXTRA_CONTRACT_ADDRESS, "0xC18360217D8F7Ab5e7c516566761Ea12Ce7F9D72");
        bundle.putString(C.EXTRA_ADDRESS, "0xC18360217D8F7Ab5e7c516566761Ea12Ce7F9D72");

        try (ActivityScenario<SendActivity> scenario = ActivityScenario.launch(SendActivity.class, bundle))
        {
            assertThat(scenario.getState(), equalTo(Lifecycle.State.CREATED));
        }
    }
}
