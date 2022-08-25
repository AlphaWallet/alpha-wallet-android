package com.alphawallet.app.router;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.ui.CoinbasePayActivity;
import com.alphawallet.app.ui.SplashActivity;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;
import com.alphawallet.shadows.ShadowKeyService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class, ShadowKeyService.class})
public class CoinbasePayRouterTest
{
    @Test
    public void testOpen()
    {
        ActivityScenario<SplashActivity> launch = ActivityScenario.launch(SplashActivity.class);
        launch.onActivity(activity -> {
            CoinbasePayRouter coinbasePayRouter = new CoinbasePayRouter();
            coinbasePayRouter.open(activity);

            Intent expectedIntent = new Intent(activity, CoinbasePayActivity.class);
            Intent actual = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
            assertThat(expectedIntent.getComponent(), equalTo(actual.getComponent()));
        });
    }

    @Test
    public void testBuyFromSelectedChain()
    {
        ActivityScenario<SplashActivity> launch = ActivityScenario.launch(SplashActivity.class);
        launch.onActivity(activity -> {
            CoinbasePayRouter coinbasePayRouter = new CoinbasePayRouter();
            coinbasePayRouter.buyFromSelectedChain(activity, "ETH");

            Intent expectedIntent = new Intent(activity, CoinbasePayActivity.class);
            Intent actual = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
            assertThat(actual.getComponent(), equalTo(expectedIntent.getComponent()));
            assertThat(actual.getStringExtra("blockchain"), equalTo("ETH"));
        });
    }
}