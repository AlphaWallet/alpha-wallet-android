package com.alphawallet.app.ui.widget.entity;

import com.bumptech.glide.signature.ObjectKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IconItem {
    private final String url;
    private final boolean fetchFromCache;
    private final String correctedAddress;
    private final int chainId;

    private final static Map<String, Boolean> iconCheck = new ConcurrentHashMap<>();

    public IconItem(String url, String correctedAddress, int chainId) {
        this.url = url;
        this.fetchFromCache = iconCheck.containsKey(correctedAddress);
        this.correctedAddress = correctedAddress;
        this.chainId = chainId;

        iconCheck.put(correctedAddress, true);
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

    public static void invalidateCheck(String address)
    {
        iconCheck.remove(address);
    }
}
