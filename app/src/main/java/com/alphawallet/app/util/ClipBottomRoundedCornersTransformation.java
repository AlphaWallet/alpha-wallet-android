package com.alphawallet.app.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class ClipBottomRoundedCornersTransformation extends BitmapTransformation
{
    private int mClipBottom;

    public ClipBottomRoundedCornersTransformation(int clipBottom)
    {
        mClipBottom = clipBottom;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight)
    {
        Bitmap bitmap = Bitmap.createBitmap(toTransform.getWidth(), toTransform.getHeight() - mClipBottom, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Rect srcRect = new Rect(0, 0, toTransform.getWidth(), toTransform.getHeight() - mClipBottom);
        RectF dstRect = new RectF(0, 0, toTransform.getWidth(), toTransform.getHeight() - mClipBottom);
        canvas.drawBitmap(toTransform, srcRect, dstRect, null);
        return bitmap;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest)
    {
        messageDigest.update(("clip_bottom_" + mClipBottom).getBytes());
    }
}
