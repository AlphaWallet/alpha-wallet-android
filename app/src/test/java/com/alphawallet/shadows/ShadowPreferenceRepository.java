package com.alphawallet.shadows;

import android.content.Context;

import com.alphawallet.app.repository.SharedPreferenceRepository;
import com.alphawallet.app.service.AnalyticsService;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(SharedPreferenceRepository.class)
public class ShadowPreferenceRepository
{
    @Implementation
    public void __constructor__(Context context) {
    }
}
