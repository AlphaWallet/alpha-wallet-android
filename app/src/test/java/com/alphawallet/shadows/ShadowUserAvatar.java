package com.alphawallet.shadows;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.alphawallet.app.widget.UserAvatar;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowView;

@Implements(UserAvatar.class)
public class ShadowUserAvatar extends ShadowView
{
    public void __constructor__(Context context, AttributeSet attrs)
    {
    }

    @Implementation
    public void resetBinding()
    {
    }
}
