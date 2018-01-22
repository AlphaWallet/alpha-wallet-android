package com.wallet.crypto.trustapp.entity;

import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.repository.entity.RealmTokenInfo;
import com.wallet.crypto.trustapp.ui.AddTokenActivity;

/**
 * Created by James on 22/01/2018.
 */

public interface TokenInterface {
    void setupContent(ImageView icon, TextView symbol);
    void addTokenSetupPage(AddTokenActivity layout);
    void storeRealmData(RealmTokenInfo obj);
}
