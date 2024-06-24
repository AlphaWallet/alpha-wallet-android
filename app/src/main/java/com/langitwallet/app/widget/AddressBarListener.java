package com.langitwallet.app.widget;

import android.webkit.WebBackForwardList;

public interface AddressBarListener
{
    boolean onLoad(String urlText);

    void onClear();

    WebBackForwardList loadNext();

    WebBackForwardList loadPrevious();

    WebBackForwardList onHomePagePressed();
}
