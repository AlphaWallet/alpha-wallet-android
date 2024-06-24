package com.langitwallet.app.ui.widget;


import android.util.Pair;

import com.langitwallet.app.entity.nftassets.NFTAsset;

import java.math.BigInteger;

public interface OnAssetClickListener
{
    void onAssetClicked(Pair<BigInteger, NFTAsset> item);

    default void onAssetLongClicked(Pair<BigInteger, NFTAsset> item)
    {
    }
}
