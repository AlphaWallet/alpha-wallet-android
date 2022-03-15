package com.alphawallet.app.ui.QRScanning;

import com.alphawallet.app.walletconnect.util.WalletConnectHelper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WalletConnectHelperTest
{
    @Test
    public void testGetChainId()
    {
        long id = WalletConnectHelper.getChainId("eip155:42");
        assertThat(id, equalTo(42L));
    }

    @Test
    public void testIsWalletConnectV1()
    {
        String textV1 = "wc:2a332c16-a4eb-4691-b2f8-f986d96cd362@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=535e4408c9d7897bd89e37dbef79cb72e7a71d6669bb9505ce4158e3d34e67e6";
        assertTrue(WalletConnectHelper.isWalletConnectV1(textV1));

        String textV2 = "wc:0aff88f09bf3870eda6e5ed2fa5556a67870e2103b71493f30d496e0c7b86f92@2?controller=false&publicKey=30bad0662bf0a411fc423e59a95cb868af9733df759a90bc569620f2b490bc4d&relay=%7B%22protocol%22%3A%22waku%22%7D";
        assertFalse(WalletConnectHelper.isWalletConnectV1(textV2));
    }
}