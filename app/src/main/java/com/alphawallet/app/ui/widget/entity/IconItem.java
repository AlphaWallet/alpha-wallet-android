package com.alphawallet.app.ui.widget.entity;

import com.bumptech.glide.signature.ObjectKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IconItem {
    private final String url;
    private final UseIcon useText;
    private final String correctedAddress;
    private final long chainId;

    private final static Map<String, Boolean> iconLoadType = new ConcurrentHashMap<>();

    public IconItem(String url, String correctedAddress, long chainId) {
        this.url = url;
        this.useText = getLoadType(correctedAddress);
        this.correctedAddress = correctedAddress;
        this.chainId = chainId;
    }

    private UseIcon getLoadType(String correctedAddress)
    {
        return iconLoadType.containsKey(correctedAddress.toLowerCase())
                ? (iconLoadType.get(correctedAddress.toLowerCase()) ? UseIcon.SECONDARY : UseIcon.NO_ICON)
                : UseIcon.PRIMARY;
    }

    public String getUrl() {
        return url;
    }

    public boolean useTextSymbol() {
        return useText == UseIcon.NO_ICON;
    }

    public boolean usePrimary() {
        return useText == UseIcon.PRIMARY;
    }

    public ObjectKey getSignature() {
        return new ObjectKey(correctedAddress + "-" + chainId);
    }

    //Use Secondary icon
    public static void secondaryFound(String address)
    {
        iconLoadType.put(address.toLowerCase(), true);
    }

    //Use TextIcon
    public static void noIconFound(String address)
    {
        iconLoadType.put(address.toLowerCase(), false);
    }

    /**
     * Resets the failed icon fetch checking - try again to load failed icons
     */
    public static void resetCheck()
    {
        iconLoadType.clear();
    }
}
