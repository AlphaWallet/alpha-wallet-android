package com.alphawallet.app.entity;

/**
 * Created by James on 4/12/2018.
 * Stormbird in Singapore
 */

public interface ENSCallback
{
    void ENSComplete();
    void displayCheckingDialog(boolean shouldShow);
    void ENSResolved(String address, String ens);
    void ENSName(String name);
}
