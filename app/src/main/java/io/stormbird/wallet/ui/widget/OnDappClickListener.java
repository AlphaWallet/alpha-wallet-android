package io.stormbird.wallet.ui.widget;

import java.io.Serializable;

import io.stormbird.wallet.entity.DApp;

public interface OnDappClickListener extends Serializable {
    void onDappClick(DApp dapp);
}
