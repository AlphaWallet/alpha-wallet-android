package com.alphawallet.app.widget;

import static com.alphawallet.app.util.Utils.loadFile;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.nio.charset.StandardCharsets;

/**
 * Created by JB on 30/05/2021.
 */
public class NFTImageView extends RelativeLayout
{
    private final ImageView image;
    private final RelativeLayout webLayout;
    private final WebView webView;
    private final RelativeLayout holdingView;
    private final RelativeLayout fallbackLayout;
    private final TokenIcon fallbackIcon;
    private final ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>()
    {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource)
        {
            //couldn't load using glide, fallback to webview
            if (model != null)
            {
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
    private Request loadRequest;
    private String imageUrl;
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
        loadImage(asset.getThumbnail(), asset.getBackgroundColor(), 1);
    }

    public void setupTokenImage(NFTAsset asset) throws IllegalArgumentException
    {
        if (shouldLoad(asset.getImage()))
        {
            showLoadingProgress(true);
            progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
            loadImage(asset.getImage(), asset.getBackgroundColor(), 16);
        }
    }

    private void loadImage(String url, String backgroundColor, int corners) throws IllegalArgumentException
    {
        if (!Utils.stillAvailable(getContext())) return;

        setWebViewHeight((int)getLayoutParams().width);

        this.imageUrl = url;
        fallbackLayout.setVisibility(View.GONE);
        image.setVisibility(View.VISIBLE);
        webLayout.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(backgroundColor))
        {
            int color = Color.parseColor("#" + backgroundColor);
            ColorStateList sl = ColorStateList.valueOf(color);
            holdingView.setBackgroundTintList(sl);
        }
        else
        {
            holdingView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
        }

        loadRequest = Glide.with(getContext())
                .load(url)
                .transform(new CenterCrop(), new RoundedCorners(corners))
                .transition(withCrossFade())
                .override(Target.SIZE_ORIGINAL)
                .timeout(30 * 1000)
                .listener(requestListener)
                .into(new DrawableImageViewTarget(image)).getRequest();
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
        }
        finally
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

    public void showLoadingProgress(boolean showProgress)
    {
        this.showProgress = showProgress;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return true;
    }

    public boolean shouldLoad(String url)
    {
        if (!TextUtils.isEmpty(url) && this.imageUrl == null)
        {
            return true;
        }
        else
        {
            if (TextUtils.isEmpty(url))
            {
                return false;
            }
            else
            {
                return !this.imageUrl.equals(url);
            }
        }
    }

    public void clearImage()
    {
        imageUrl = null;
    }
}
