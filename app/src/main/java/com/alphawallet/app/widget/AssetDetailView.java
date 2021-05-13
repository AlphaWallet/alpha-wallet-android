package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;

/**
 * Created by Jenny Jingjing Li on 13/05/2021
 */

public class AssetDetailView extends LinearLayout
{
    private TextView asset;

    public AssetDetailView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.item_asset_detail, this);
        asset = findViewById(R.id.text_asset_name);
    }

    public void setAssetName(String name)
    {
        asset.setText(name);
    }
}
