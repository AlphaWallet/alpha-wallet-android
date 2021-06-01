package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.opensea.Asset;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.nio.charset.StandardCharsets;

import static com.alphawallet.app.util.Utils.loadFile;

/**
 * Created by JB on 30/05/2021.
 */
public class ERC721ImageView extends LinearLayout
{
    private final ImageView image;
    private final LinearLayout webLayout;
    private final WebView webView;
    private final LinearLayout holdingView;
    private final Handler handler = new Handler();

    public ERC721ImageView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_erc721_image, this);
        image = findViewById(R.id.image_asset);
        webLayout = findViewById(R.id.web_view_wrapper);
        webView = findViewById(R.id.image_web_view);
        holdingView = findViewById(R.id.layout_holder);

        //setup view attributes
        setAttrs(context, attrs);
    }

    public void setupTokenImageThumbnail(Asset asset)
    {
        loadTokenImage(asset, asset.getPreviewImageUrl());
    }

    public void setupTokenImage(Asset asset)
    {
        loadTokenImage(asset, asset.getBestImageUrl());
    }

    private void loadTokenImage(Asset asset, String imageUrl)
    {
        if (getContext() == null ||
                (getContext() instanceof Activity && ((Activity)getContext()).isDestroyed())) return;

        image.setVisibility(View.VISIBLE);

        Glide.with(getContext())
                .load(imageUrl)
                .listener(requestListener)
                .into(image);

        if (!asset.needsLoading() && asset.getBackgroundColor() != null && !asset.getBackgroundColor().equals("null")) {
            int color = Color.parseColor("#" + asset.getBackgroundColor());
            holdingView.setBackgroundColor(color);
        } else {
            holdingView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
        }
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            //couldn't load using glide, fallback to webview
            if (model != null) setWebView(model.toString());
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

    private void setWebView(String imageUrl)
    {
        String loader = loadFile(getContext(), R.raw.token_graphic).replace("[URL]", imageUrl);
        String base64 = android.util.Base64.encodeToString(loader.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

        handler.post(() -> {
            image.setVisibility(View.GONE);
            webLayout.setVisibility(View.VISIBLE);
            webView.loadData(base64, "text/html; charset=utf-8", "base64");
        });
    }

    private void setAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ERC721ImageView,
                0, 0);

        int height = a.getInteger(R.styleable.ERC721ImageView_webview_height, 0);
        if (height > 0)
        {
            int dpHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, getContext().getResources().getDisplayMetrics());
            ViewGroup.LayoutParams layoutParams = webLayout.getLayoutParams();
            layoutParams.height = dpHeight;
            webLayout.setLayoutParams(layoutParams);
            layoutParams = holdingView.getLayoutParams();
            layoutParams.height = dpHeight;
            findViewById(R.id.layout_holder).setLayoutParams(layoutParams);
        }
    }

    public void blankViews()
    {
        image.setVisibility(View.INVISIBLE);
        webLayout.setVisibility(View.GONE);
    }
}
