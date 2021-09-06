package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.gridlayout.widget.GridLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;

import java.util.Map;

/**
 * Created by JB on 2/09/2021.
 */
public class NFTAttributeLayout extends LinearLayout
{
    private final GridLayout grid;
    private final TextView labelAttributes;

    public NFTAttributeLayout(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        View view = inflate(context, R.layout.item_nft_attributes, this);

        grid = view.findViewById(R.id.grid);
        labelAttributes = view.findViewById(R.id.label_attributes);

        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
    }

    public void bind(Token token, NFTAsset asset)
    {
        Map<String, String> attributes = asset.getAttributes();
        setAttributeLabel(token, attributes.size());
        for (String key : attributes.keySet())
        {
            View attributeView = View.inflate(getContext(), R.layout.item_attribute, null);
            TextView traitType = attributeView.findViewById(R.id.trait);
            TextView traitValue = attributeView.findViewById(R.id.value);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            attributeView.setLayoutParams(params);
            traitType.setText(key);
            traitValue.setText(attributes.get(key));
            grid.addView(attributeView);
        }
    }

    private void setAttributeLabel(Token token, int size)
    {
        if (size > 0 && token.tokenInfo.name.toLowerCase().contains("cryptokitties"))
        {
            labelAttributes.setText(R.string.label_cattributes);
        }
        else if (size > 0)
        {
            labelAttributes.setText(R.string.label_attributes);
        }
        else
        {
            labelAttributes.setVisibility(View.GONE);
        }
    }
}
