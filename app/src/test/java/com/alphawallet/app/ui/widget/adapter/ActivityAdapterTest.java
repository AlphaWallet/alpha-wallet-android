package com.alphawallet.app.ui.widget.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.shadows.ShadowAnalyticsService;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;
import com.alphawallet.shadows.ShadowKeyService;
import com.alphawallet.shadows.ShadowRealmManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class, ShadowRealmManager.class, ShadowKeyService.class, ShadowAnalyticsService.class, ShadowPackageManager.class})
public class ActivityAdapterTest
{
    @Test
    public void test_isEmpty()
    {
        ActivityAdapter activityAdapter = new ActivityAdapter(null, null, null);
        activityAdapter.updateActivityItems(new ActivityMeta[]{});
        assertThat(activityAdapter.isEmpty(), equalTo(true));
        activityAdapter.updateActivityItems(new ActivityMeta[]{ new EventMeta("", "", "", System.currentTimeMillis(), 1L)});
        assertThat(activityAdapter.isEmpty(), equalTo(false));
    }
}
