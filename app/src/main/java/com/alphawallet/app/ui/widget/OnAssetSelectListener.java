package com.alphawallet.app.ui.widget;


import com.alphawallet.app.entity.nftassets.NFTAsset;

public interface OnAssetSelectListener
{
    void onAssetSelected(NFTAsset asset, int position);
    void onAssetUnselected();
}
