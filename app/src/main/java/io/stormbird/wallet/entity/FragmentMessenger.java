package io.stormbird.wallet.entity;

/**
 * Created by James on 1/02/2019.
 * Stormbird in Singapore
 */
public interface FragmentMessenger
{
    void TokensReady();
    void AddToken(String address);
}
