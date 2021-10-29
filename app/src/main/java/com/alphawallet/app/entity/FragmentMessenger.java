package com.alphawallet.app.entity;

/**
 * Created by James on 1/02/2019.
 * Stormbird in Singapore
 */
public interface FragmentMessenger
{
    void tokenScriptError(String message);
    void updateReady(int versionUpdate);
}
