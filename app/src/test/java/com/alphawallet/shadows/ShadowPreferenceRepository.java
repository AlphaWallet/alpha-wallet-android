package com.alphawallet.shadows;

import android.content.Context;

import com.langitwallet.app.repository.SharedPreferenceRepository;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(SharedPreferenceRepository.class)
public class ShadowPreferenceRepository
{
    @Implementation
    public void __constructor__(Context context) {
    }
}
