package com.alphawallet.app.widget;

import static com.alphawallet.app.util.Utils.loadFile;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.nio.charset.StandardCharsets;

/**
 * Created by JB on 30/05/2021.
 */
public class NFTImageView extends RelativeLayout {
    private final ImageView image;
    private final RelativeLayout webLayout;
    private final WebView webView;
    private final RelativeLayout holdingView;
    private final RelativeLayout fallbackLayout;
    private final TokenIcon fallbackIcon;
    private final ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Request loadRequest;
    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource)
        {
            //couldn't load using glide, fallback to webview
            if (model != null) {
                progressBar.setVisibility(View.GONE);
                setWebView(model.toString());
            }
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource)
        {
            progressBar.setVisibility(View.GONE);
            return false;
        }
    };

    private boolean hasContent;
    private boolean showProgress;

    public NFTImageView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_asset_image, this);
        image = findViewById(R.id.image_asset);
        webLayout = findViewById(R.id.web_view_wrapper);
        webView = findViewById(R.id.image_web_view);
        holdingView = findViewById(R.id.layout_holder);
        fallbackLayout = findViewById(R.id.layout_fallback);
        fallbackIcon = findViewById(R.id.icon_fallback);
        progressBar = findViewById(R.id.avatar_progress_spinner);

        webLayout.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);

        if (loadRequest != null && loadRequest.isRunning())
        {
            loadRequest.clear();
        }

        //setup view attributes
        setAttrs(context, attrs);
    }

    public void setupTokenImageThumbnail(NFTAsset asset)
    {
        loadTokenImage(asset, asset.getThumbnail());
    }

    public void setupTokenImage(NFTAsset asset)
    {
        progressBar.setVisibility(showProgress? View.VISIBLE : View.GONE);
        loadTokenImage(asset, asset.getImage());
    }

    private void loadTokenImage(NFTAsset asset, String imageUrl)
    {
        image.setVisibility(View.VISIBLE);

        loadRequest = Glide.with(image.getContext())
                .load(imageUrl)
                .centerCrop()
                .transition(withCrossFade())
                .override(Target.SIZE_ORIGINAL)
                .listener(requestListener)
                .into(new DrawableImageViewTarget(image)).getRequest();

        if (!asset.needsLoading() && asset.getBackgroundColor() != null && !asset.getBackgroundColor().equals("null"))
        {
            int color = Color.parseColor("#" + asset.getBackgroundColor());
            holdingView.setBackgroundColor(color);
        }
        else
        {
            holdingView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
        }

        hasContent = true;
    }

    private void setWebView(String imageUrl)
    {
        String loader = loadFile(getContext(), R.raw.token_graphic).replace("[URL]", imageUrl);
        String base64 = android.util.Base64.encodeToString(loader.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        handler.post(() -> {
            image.setVisibility(View.GONE);
            webLayout.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);
            webView.loadData(base64, "text/html; charset=utf-8", "base64");
        });
    }

    private void setAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ERC721ImageView,
                0, 0);

        try
        {
            int height = a.getInteger(R.styleable.ERC721ImageView_webview_height, 0);
            if (height > 0)
            {
                setWebViewHeight(Utils.dp2px(getContext(), height));
            }
        } finally
        {
            a.recycle();
        }
    }

    public void setWebViewHeight(int height)
    {
        ViewGroup.LayoutParams webLayoutParams = webLayout.getLayoutParams();
        webLayoutParams.height = height;
        webLayout.setLayoutParams(webLayoutParams);
    }

    public void showFallbackLayout(Token token)
    {
        fallbackLayout.setVisibility(View.VISIBLE);
        fallbackIcon.bindData(token);

        hasContent = true;
    }

    public boolean hasContent()
    {
        return hasContent;
    }

    public void showLoadingProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return true;
    }
}
