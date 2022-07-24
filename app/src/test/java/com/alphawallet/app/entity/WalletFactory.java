package com.alphawallet.app.entity;

public class WalletFactory
{
    public static Wallet createHDKeyWallet(String address)
    {
        Wallet wallet = new Wallet(address);
        wallet.type = WalletType.HDKEY;
        return wallet;
    }
}
