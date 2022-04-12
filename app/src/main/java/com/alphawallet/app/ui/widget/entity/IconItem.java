package com.alphawallet.app.ui.widget.entity;

import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;

import com.bumptech.glide.signature.ObjectKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IconItem {
    private final String url;
    private final UseIcon useText;

    private final static Map<String, Boolean> iconLoadType = new ConcurrentHashMap<>();

    public IconItem(String url, long chainId, String correctedAddress) {
        this.url = url;
        this.useText = getLoadType(chainId, correctedAddress);
    }

    private UseIcon getLoadType(long chainId, String correctedAddress)
    {
        String key = databaseKey(chainId, correctedAddress);
        return iconLoadType.containsKey(key)
                ? (iconLoadType.get(key) ? UseIcon.SECONDARY : UseIcon.NO_ICON)
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

    //Use Secondary icon
    public static void secondaryFound(long chainId, String address)
    {
        iconLoadType.put(databaseKey(chainId, address.toLowerCase()), true);
    }

    //Use TextIcon
    public static void noIconFound(long chainId, String address)
    {
        iconLoadType.put(databaseKey(chainId, address.toLowerCase()), false);
    }

    /**
     * Resets the failed icon fetch checking - try again to load failed icons
     */
    public static void resetCheck()
    {
        iconLoadType.clear();
    }
}
