package com.alphawallet.app.entity;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import org.jetbrains.annotations.NotNull;

/**
 * Created by JB on 24/10/2021.
 */
@GlideModule
public class AlphaWalletGlideModule extends AppGlideModule
{
    @Override
    public void applyOptions(@NotNull Context context, GlideBuilder builder) {
        builder.setLogLevel(Log.ERROR);
    }
}
