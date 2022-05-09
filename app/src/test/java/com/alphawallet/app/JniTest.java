package com.alphawallet.app;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class JniTest
{
    @Test
    public void name()
    {
        assertNotNull(Foo.getInfuraKey());
    }
}

class Foo {
    static {
        System.loadLibrary("keys");
    }

    public static native String getInfuraKey();
}
