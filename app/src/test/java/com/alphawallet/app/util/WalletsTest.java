package com.alphawallet.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;

import org.junit.Test;

public class WalletsTest
{
    @Test
    public void should_not_return_watch_wallets()
    {
        Wallet wallet = new Wallet("0x1");
        wallet.type = WalletType.WATCH;
        Wallet wallet2 = new Wallet("0x2");
        wallet2.type = WalletType.KEYSTORE;
        Wallet wallet3 = new Wallet("0x3");
        wallet3.type = WalletType.HDKEY;

        Wallet[] wallets = {wallet, wallet2, wallet3};
        Wallet[] filtered = Wallets.filter(wallets);

        assertThat(filtered.length, equalTo(2));
        assertThat(filtered[0].address, equalTo("0x2"));
        assertThat(filtered[1].address, equalTo("0x3"));
    }
}
