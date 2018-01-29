package com.wallet.crypto.trustapp.entity;

import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.repository.entity.RealmTokenInfo;
import com.wallet.crypto.trustapp.ui.AddTokenActivity;
import com.wallet.crypto.trustapp.ui.widget.holder.TokenHolder;

import org.web3j.abi.datatypes.generated.Uint16;

import java.util.List;

/**
 * Created by James on 22/01/2018.
 */

public interface TokenInterface {
    void setupContent(TokenHolder tokenHolder);
    void addTokenSetupPage(AddTokenActivity layout);
    void storeRealmData(RealmTokenInfo obj);
    String populateIDs(List<Integer> indexList, boolean keepZeros);
}
