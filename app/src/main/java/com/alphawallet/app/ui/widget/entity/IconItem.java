package com.alphawallet.app.ui.widget.entity;

import com.bumptech.glide.signature.ObjectKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IconItem {
    private final String url;
    private final boolean useText;
    private final String correctedAddress;
    private final int chainId;

    private final static Map<String, Boolean> iconLoadFails = new ConcurrentHashMap<>();

    public IconItem(String url, String correctedAddress, int chainId) {
        this.url = url;
        this.useText = iconLoadFails.containsKey(correctedAddress.toLowerCase());
        this.correctedAddress = correctedAddress;
        this.chainId = chainId;
    }

    public String getUrl() {
        return url;
    }

    public boolean useTextSymbol() {
        return useText;
    }

    public ObjectKey getSignature() {
        return new ObjectKey(correctedAddress + "-" + chainId);
    }

    public static void iconLoadFail(String address)
    {
        iconLoadFails.put(address.toLowerCase(), true);
    }

    /**
     * Resets the failed icon fetch checking - try again to load failed icons
     */
    public static void resetCheck()
    {
        iconLoadFails.clear();
    }
}
