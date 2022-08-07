package com.alphawallet.app.ui;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.di.ProviderModule;
import com.alphawallet.shadows.ShadowAnalyticsService;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowEthereumNetworkBase;
import com.alphawallet.shadows.ShadowKeyService;
import com.alphawallet.shadows.ShadowRealmManager;
import com.alphawallet.shadows.ShadowSystemWrapper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@UninstallModules(ProviderModule.class)
@Config(application = HiltTestApplication.class, shadows = {ShadowApp.class, ShadowSystemWrapper.class, ShadowEthereumNetworkBase.class, ShadowRealmManager.class, ShadowKeyService.class, ShadowAnalyticsService.class, ShadowPackageManager.class})
public class HomeActivityTest
{
    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Before
    public void init()
    {
        hiltRule.inject();
    }

    @Test
    public void should_show_splash_when_first_launch()
    {
        ShadowPackageManager shadowPackageManager = new ShadowPackageManager();
        shadowPackageManager.setInstallSourceInfo(RuntimeEnvironment.getApplication().getPackageName(), "", "");

        ActivityScenario<HomeActivity> launch = ActivityScenario.launch(HomeActivity.class);
        launch.onActivity(activity -> {
            Intent expectedIntent = new Intent(activity, SplashActivity.class);
            Intent actual = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
            assertEquals(expectedIntent.getComponent(), actual.getComponent());
        });
    }
}
