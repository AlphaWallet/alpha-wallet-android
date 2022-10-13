package com.alphawallet.app.widget;

public interface AddressBarListener
{
    boolean onLoad(String urlText);
    void onClear();

    void loadNext();

    void loadPrevious();
}
