package com.alphawallet.app.ui.widget;

import java.io.Serializable;

import com.alphawallet.app.entity.DApp;

public interface OnDappClickListener extends Serializable {
    void onDappClick(DApp dapp);
}
