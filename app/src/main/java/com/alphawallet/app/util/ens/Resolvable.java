package com.alphawallet.app.util.ens;

public interface Resolvable
{
    String resolve(String ensName) throws Exception;
}
