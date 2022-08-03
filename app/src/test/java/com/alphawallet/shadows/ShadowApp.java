package com.alphawallet.shadows;

import com.alphawallet.app.App;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(App.class)
public class ShadowApp
{
    @Implementation
    public void onCreate()
    {
    }
}
