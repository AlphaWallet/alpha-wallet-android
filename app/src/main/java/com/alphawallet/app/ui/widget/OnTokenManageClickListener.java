package com.alphawallet.app.ui.widget;

import com.alphawallet.app.entity.tokens.Token;

public interface OnTokenManageClickListener
{
    void onTokenClick(Token token, int position, boolean isChecked);
}
