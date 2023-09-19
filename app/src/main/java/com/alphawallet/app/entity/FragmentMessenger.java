package com.alphawallet.app.entity;

/**
 * Created by James on 1/02/2019.
 * Stormbird in Singapore
 */
public interface FragmentMessenger
{
    void tokenScriptError(String message);

    void playStoreUpdateReady(int versionUpdate);

    void externalUpdateReady(String version);
}
