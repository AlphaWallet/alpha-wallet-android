package com.alphawallet.shadows;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(System.class)
public class ShadowSystem
{
    @Implementation
    public static void loadLibrary(String libname) {
    }
}
