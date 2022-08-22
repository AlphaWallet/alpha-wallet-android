package com.alphawallet.app.web3j.ens;

public interface Resolvable
{
    String resolve(String ensName) throws Exception;
}
