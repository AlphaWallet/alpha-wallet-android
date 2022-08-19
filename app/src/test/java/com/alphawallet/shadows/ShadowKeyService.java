package com.alphawallet.shadows;

import android.content.Context;

import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.KeyService;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(KeyService.class)
public class ShadowKeyService
{
    @Implementation
    public void __constructor__(Context ctx, AnalyticsServiceType<AnalyticsProperties> analyticsService) {

    }
}
