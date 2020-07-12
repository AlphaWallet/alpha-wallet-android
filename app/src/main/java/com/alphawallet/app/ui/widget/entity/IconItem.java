package com.alphawallet.app.ui.widget.entity;

import com.bumptech.glide.signature.ObjectKey;

public class IconItem {
    private final String url;
    private final boolean fetchFromCache;
    private final String correctedAddress;
    private final int chainId;

    public IconItem(String url, boolean fetchFromCache, String correctedAddress, int chainId) {
        this.url = url;
        this.fetchFromCache = fetchFromCache;
        this.correctedAddress = correctedAddress;
        this.chainId = chainId;
    }

    public String getUrl() {
        return url;
    }

    public boolean onlyFetchFromCache() {
        return fetchFromCache;
    }

    public ObjectKey getSignature() {
        return new ObjectKey(correctedAddress + "-" + chainId);
    }
}
