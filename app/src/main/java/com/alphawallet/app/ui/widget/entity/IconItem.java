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

    private final static Map<String, ConcurrentLinkedQueue<String>> iconCheck = new ConcurrentHashMap<>();

    public IconItem(String url, String correctedAddress, int chainId, String parentClassName) {
        this.url = url;
        this.fetchFromCache = hasBeenChecked(correctedAddress, parentClassName);
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

    private boolean hasBeenChecked(String addr, String className)
    {
        ConcurrentLinkedQueue<String> checkedAddrs = iconCheck.get(className);
        if (checkedAddrs == null)
        {
            checkedAddrs = new ConcurrentLinkedQueue<>();
            iconCheck.put(className, checkedAddrs);
        }

        if (checkedAddrs.contains(addr))
        {
            return true;
        }
        else
        {
            checkedAddrs.add(addr);
            return false;
        }
    }
}
