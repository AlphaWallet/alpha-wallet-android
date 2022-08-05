package com.alphawallet.shadows;

import com.alphawallet.app.util.SystemWrapper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(SystemWrapper.class)
public class ShadowSystemWrapper
{
    @Implementation
    public static void loadKeysLibrary()
    {
    }
}
