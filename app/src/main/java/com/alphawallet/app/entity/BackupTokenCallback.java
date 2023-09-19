package com.alphawallet.app.entity;

/**
 * Created by James on 18/07/2019.
 * Stormbird in Sydney
 */
public interface BackupTokenCallback
{
    default void backUpClick(Wallet wallet) { }
    default void remindMeLater(Wallet wallet) { };
}
