package com.alphawallet.app.entity.tokens;


import java.util.Map;

public class ERC1155Token { // extends Token implements Parcelable TODO

    private Map<Long, ERC1155Asset> assets;

    public Map<Long, ERC1155Asset> getAssets()
    {
        return assets;
    }

    public void setAssets(Map<Long, ERC1155Asset> assets)
    {
        this.assets = assets;
    }
}
