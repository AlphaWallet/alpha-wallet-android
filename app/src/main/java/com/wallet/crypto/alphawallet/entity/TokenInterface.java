package com.wallet.crypto.alphawallet.entity;

import com.wallet.crypto.alphawallet.ui.AddTokenActivity;
import com.wallet.crypto.alphawallet.ui.widget.holder.TokenHolder;

import java.util.List;

/**
 * Created by James on 22/01/2018.
 */

public interface TokenInterface {
    void setupContent(TokenHolder tokenHolder);
    void addTokenSetupPage(AddTokenActivity layout);
    String populateIDs(List<Integer> indexList, boolean keepZeros);
}
