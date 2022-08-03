package com.alphawallet.shadows;

import android.app.Application;

import com.alphawallet.app.App;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(App.class)
public class ShadowApp extends Application
{
    @Implementation
    public void onCreate()
    {
    }
}
