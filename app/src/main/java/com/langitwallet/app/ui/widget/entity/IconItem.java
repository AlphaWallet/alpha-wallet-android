package com.langitwallet.app.ui.widget.entity;

import static com.langitwallet.app.repository.TokensRealmSource.databaseKey;

public class IconItem {
    private final String url;

    public IconItem(String url, long chainId, String correctedAddress) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
