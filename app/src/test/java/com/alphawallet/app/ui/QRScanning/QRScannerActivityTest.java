package com.alphawallet.app.ui.QRScanning;

import com.alphawallet.shadows.ShadowRealm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRealm.class})
public class QRScannerActivityTest
{

    @Test
    @Config(sdk = 23)
    public void given_api_23_when_onCreate_then_notify_feature_not_supported()
    {
        try (ActivityScenario<QRScannerActivity> scenario = ActivityScenario.launch(QRScannerActivity.class))
        {
            assertThat(scenario.getState(), equalTo(Lifecycle.State.DESTROYED));
            assertThat(ShadowToast.getTextOfLatestToast(), equalTo("QR scanning requires Android 7.0 (API level 24) or above."));
        }
    }
}