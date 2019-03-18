package io.stormbird.wallet.ui.widget;

import java.io.Serializable;

import io.stormbird.wallet.entity.DApp;

public interface OnDappHomeNavClickListener extends Serializable {
    void onDappHomeNavClick(int position);
}
