package com.alphawallet.app.router;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.ui.CoinbasePayActivity;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.ui.SplashActivity;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;
import com.alphawallet.shadows.ShadowKeyService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class, ShadowKeyService.class})
public class CoinbasePayRouterTest
{
    private CoinbasePayRouter coinbasePayRouter;
    private SplashActivity activity;

    @Before
    public void setUp()
    {
        ActivityController<SplashActivity> activityController = Robolectric.buildActivity(SplashActivity.class);
        activity = activityController.get();
        coinbasePayRouter = new CoinbasePayRouter();
    }

    @Test
    public void testOpen()
    {
        coinbasePayRouter.open(activity);

        Intent expectedIntent = new Intent(activity, CoinbasePayActivity.class);
        Intent actual = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertThat(expectedIntent.getComponent(), equalTo(actual.getComponent()));
    }

    @Test
    public void testBuyFromSelectedChain()
    {
        coinbasePayRouter.buyFromSelectedChain(activity, "ETH");

        Intent expectedIntent = new Intent(activity, CoinbasePayActivity.class);
        Intent actual = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertThat(actual.getComponent(), equalTo(expectedIntent.getComponent()));
        assertThat(actual.getStringExtra("blockchain"), equalTo("ETH"));
    }
}