package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Created by JB on 26/08/2021.
 */
public class StandardHeader extends LinearLayout
{
    private TextView headerText;
    private ChainName chainName;
    private SwitchMaterial switchMaterial;
    private View separator;

    public StandardHeader(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_standard_header, this);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.StandardHeader,
                0, 0
        );

        try
        {
            int headerId = a.getResourceId(R.styleable.StandardHeader_headerText, R.string.empty);
            boolean showSwitch = a.getBoolean(R.styleable.StandardHeader_showSwitch, false);
            boolean showChainName = a.getBoolean(R.styleable.StandardHeader_showChain, false);

            headerText = findViewById(R.id.text_header);
            chainName = findViewById(R.id.chain_name);
            switchMaterial = findViewById(R.id.switch_material);
            separator = findViewById(R.id.separator);

            headerText.setText(headerId);

            if (showSwitch)
            {
                switchMaterial.setVisibility(View.VISIBLE);
            }
            else
            {
                switchMaterial.setVisibility(View.GONE);
            }

            if (showChainName)
            {
                chainName.setVisibility(View.VISIBLE);
            }
            else
            {
                chainName.setVisibility(View.GONE);
            }
        }
        finally
        {
            a.recycle();
        }
    }

    public void setText(String text)
    {
        headerText.setText(text);
    }

    public void setText(int resId)
    {
        headerText.setText(resId);
    }

    public ChainName getChainName()
    {
        return chainName;
    }

    public SwitchMaterial getSwitch()
    {
        return switchMaterial;
    }

    public void hideSeparator()
    {
        separator.setVisibility(View.GONE);
    }
}
