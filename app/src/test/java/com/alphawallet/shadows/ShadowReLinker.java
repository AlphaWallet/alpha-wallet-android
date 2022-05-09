package com.alphawallet.shadows;

import android.content.Context;

import com.getkeepsafe.relinker.ReLinker;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(ReLinker.class)
public class ShadowReLinker
{
    @Implementation
    public static void loadLibrary(final Context context,
                                   final String library,
                                   final String version)
    {
        System.loadLibrary(library);
//        System.load("/Users/seabornlee/Library/Java/Extensions/lib" + library + ".so");
    }
}
