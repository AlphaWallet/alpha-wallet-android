package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
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
    private TextView textControl;
    private ImageView imageControl;
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
            boolean showTextControl = a.getBoolean(R.styleable.StandardHeader_showTextControl, false);
            boolean showImageControl = a.getBoolean(R.styleable.StandardHeader_showImageControl, false);
            int controlText = a.getResourceId(R.styleable.StandardHeader_controlText, -1);
            int controlImageRes = a.getResourceId(R.styleable.StandardHeader_controlImageRes, -1);

            headerText = findViewById(R.id.text_header);
            chainName = findViewById(R.id.chain_name);
            switchMaterial = findViewById(R.id.switch_material);
            separator = findViewById(R.id.separator);
            textControl = findViewById(R.id.text_control);
            imageControl = findViewById(R.id.image_control);

            headerText.setText(headerId);

            switchMaterial.setVisibility(showSwitch ? View.VISIBLE : View.GONE);
            chainName.setVisibility(showChainName ? View.VISIBLE : View.GONE);

            if (showTextControl)
            {
                textControl.setVisibility(View.VISIBLE);
                textControl.setText(controlText);
            }
            else
            {
                textControl.setVisibility(View.GONE);
            }

            if (showImageControl)
            {
                imageControl.setVisibility(View.VISIBLE);
                imageControl.setImageResource(controlImageRes);
            }
            else
            {
                imageControl.setVisibility(View.GONE);
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

    public TextView getTextControl()
    {
        return textControl;
    }

    public ImageView getImageControl()
    {
        return imageControl;
    }

    public void hideSeparator()
    {
        separator.setVisibility(View.GONE);
    }
}
