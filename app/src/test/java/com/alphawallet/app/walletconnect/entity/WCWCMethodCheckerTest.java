package com.alphawallet.app.walletconnect.entity;

import com.alphawallet.app.walletconnect.util.WCMethodChecker;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WCWCMethodCheckerTest
{
    @Test
    public void test_includes()
    {
        assertTrue(WCMethodChecker.includes("wc_sessionRequest"));
        assertTrue(WCMethodChecker.includes("wc_sessionUpdate"));
        assertTrue(WCMethodChecker.includes("eth_sign"));
        assertTrue(WCMethodChecker.includes("personal_sign"));
        assertTrue(WCMethodChecker.includes("eth_signTypedData"));
        assertTrue(WCMethodChecker.includes("eth_signTransaction"));
        assertTrue(WCMethodChecker.includes("eth_sendTransaction"));
        assertTrue(WCMethodChecker.includes("get_accounts"));
    }
    
    @Test
    public void test_not_includes()
    {
        assertFalse(WCMethodChecker.includes("wc_not_found"));
    }
}
