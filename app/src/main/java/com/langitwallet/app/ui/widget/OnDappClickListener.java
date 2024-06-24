package com.langitwallet.app.ui.widget;

import com.langitwallet.app.entity.DApp;

import java.io.Serializable;

public interface OnDappClickListener extends Serializable {
    void onDappClick(DApp dapp);
}
