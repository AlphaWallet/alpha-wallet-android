package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;

import timber.log.Timber;

public class NodeStatusInfo extends LinearLayout
{
    Context context;
    ImageView icon;
    TextView message;

    public NodeStatusInfo(Context context, AttributeSet attr)
    {
        super(context, attr);
        this.context = context;
        inflate(context, R.layout.item_node_status_info, this);

        icon = findViewById(R.id.image);
        message = findViewById(R.id.text);

        setupAttrs(context, attr);
    }

    private void setupAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.NodeStatusInfo,
                0, 0
        );

        try
        {
            setIcon(a.getResourceId(R.styleable.NodeStatusInfo_android_icon, R.drawable.ic_help));
            setMessage(a.getString(R.styleable.NodeStatusInfo_android_text));
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    public void setMessage(String msg)
    {
        message.setText(context.getString(R.string.node_status_label, msg));
    }

    public void setIcon(@DrawableRes int res)
    {
        icon.setImageDrawable(ContextCompat.getDrawable(context, res));
    }
}
