package com.alphawallet.app.ui.widget.entity;

import com.bumptech.glide.signature.ObjectKey;

public class IconItem {
    private String url;
    private boolean fetchFromCache;
    private String correctedAddress;
    private ObjectKey signature;

    public IconItem(String url, boolean fetchFromCache, String correctedAddress, ObjectKey signature) {
        this.url = url;
        this.fetchFromCache = fetchFromCache;
        this.correctedAddress = correctedAddress;
        this.signature = signature;
    }

    public String getUrl() {
        return url;
    }

    public boolean isFetchFromCache() {
        return fetchFromCache;
    }

    public String getCorrectedAddress() {
        return correctedAddress;
    }

    public ObjectKey getSignature() {
        return signature;
    }
}
