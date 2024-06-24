package com.langitwallet.app.ui.widget;

import com.langitwallet.app.entity.tokens.Token;

public interface OnTokenManageClickListener
{
    void onTokenClick(Token token, int position, boolean isChecked);
}
