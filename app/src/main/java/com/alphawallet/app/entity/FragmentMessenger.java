package com.alphawallet.app.entity;

import com.google.android.play.core.appupdate.AppUpdateInfo;

/**
 * Created by James on 1/02/2019.
 * Stormbird in Singapore
 */
public interface FragmentMessenger
{
    void TokensReady();
    void AddToken(String address);
    void tokenScriptError(String message);
    void updateReady(AppUpdateInfo updateInfo);
}
