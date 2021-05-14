package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;

/**
 * Created by Jenny Jingjing Li on 13/05/2021
 */

public class AssetDetailView extends LinearLayout
{
    private TextView asset;
    private AssetDefinitionService assetDefinitionService;

    public AssetDetailView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.item_asset_detail, this);
        asset = findViewById(R.id.text_asset_name);
    }

    public void setupAssetDetail(Token token)
    {
        token.getFullName();
    }
}
