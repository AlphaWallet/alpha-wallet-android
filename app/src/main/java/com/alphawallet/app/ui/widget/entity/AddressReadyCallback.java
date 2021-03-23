package com.alphawallet.app.ui.widget.entity;

/**
 * Created by JB on 28/10/2020.
 */
public interface AddressReadyCallback
{
    void addressReady(String address, String ensName);
    default void resolvedAddress(String address, String ensName) { }; //ENS finished resolving an address
}
