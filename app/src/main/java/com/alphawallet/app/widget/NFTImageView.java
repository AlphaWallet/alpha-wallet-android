package com.alphawallet.app.widget;

import static com.alphawallet.app.util.Utils.isIPFS;
import static com.alphawallet.app.util.Utils.loadFile;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Attestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * Created by JB on 30/05/2021.
 */
public class NFTImageView extends RelativeLayout implements View.OnTouchListener
{
    private final ImageView image;
    private final RelativeLayout webLayout;
    private final WebView webView;
    private final ConstraintLayout holdingView;
    private final RelativeLayout fallbackLayout;
    private final TokenIcon fallbackIcon;
    private final ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer mediaPlayer;
    private int webViewHeight = 0;
    private int heightUpdates;
    private final static int STANDARD_THUMBNAIL_HEIGHT = 156; //standard height in dp of thumbnail icon; don't allow lower than this

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<>()
    {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource)
        {
            //couldn't load using glide
            String msg = e != null ? e.toString() : "";
            if (msg.contains(C.GLIDE_URL_INVALID)) //URL not valid: use the attribute name
            {
                handler.post(() -> {
                    progressBar.setVisibility(GONE);
                    fallbackLayout.setVisibility(VISIBLE);
                });
            }
            else if (model != null) //or fallback to webview if there was some other problem
            {
                setWebView(model.toString(), ImageType.IMAGE);
                fallbackLayout.setVisibility(GONE);
            }
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource)
        {
            progressBar.setVisibility(GONE);
            fallbackLayout.setVisibility(GONE);
            return false;
        }
    };
    private Request loadRequest;
    private String imageUrl;
    private boolean isThumbnail;

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
        mediaPlayer = null;

        webLayout.setVisibility(GONE);
        webView.setVisibility(GONE);

        if (loadRequest != null && loadRequest.isRunning())
        {
            loadRequest.clear();
        }

        //setup view attributes
        setAttrs(context, attrs);
    }

    public void setupTokenImageThumbnail(NFTAsset asset)
    {
        setupTokenImageThumbnail(asset, false);
    }

    public void setupTokenImageThumbnail(NFTAsset asset, boolean onlyRoundTopCorners)
    {
        heightUpdates = 0;
        fallbackIcon.setupFallbackTextIcon(asset.getName());
        isThumbnail = true;
        loadImage(asset.getThumbnail(), asset.getBackgroundColor());
        if (onlyRoundTopCorners)
        {
            ((ImageView)findViewById(R.id.overlay_rect)).setImageResource(R.drawable.mask_rounded_corners_top_only);
            ((ImageView)findViewById(R.id.image_overlay_rect)).setImageResource(R.drawable.mask_rounded_corners_top_only);
        }
    }

    public void setupTokenImage(NFTAsset asset) throws IllegalArgumentException
    {
        heightUpdates = 0;
        isThumbnail = false;
        String anim = asset.getAnimation();
        fallbackIcon.setupFallbackTextIcon(asset.getName());

        if (anim != null && !isGlb(anim) && !isAudio(anim) && !isIPFS(anim)) //IPFS anims don't seem to render correctly
        {
            if (!shouldLoad(anim)) return;
            //attempt to load animation
            setWebView(anim, ImageType.ANIM);
        }
        else if (shouldLoad(asset.getImage()))
        {
            loadImage(asset.getImage(), asset.getBackgroundColor());
            playAudioIfAvailable(anim);
        }
    }

    public void setImageResource(int resourceId)
    {
        image.setImageResource(resourceId);
    }

    private void loadImage(String url, String backgroundColor) throws IllegalArgumentException
    {
        if (!Utils.stillAvailable(getContext())) return;

        this.imageUrl = url;
        image.setVisibility(View.VISIBLE);
        webLayout.setVisibility(GONE);

        try
        {
            int color = Color.parseColor("#" + backgroundColor);
            ColorStateList sl = ColorStateList.valueOf(color);
            holdingView.setBackgroundTintList(sl);
        }
        catch (Exception e)
        {
            holdingView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
        }

        loadRequest = Glide.with(getContext())
                .load(url)
                .transition(withCrossFade())
                .override(Target.SIZE_ORIGINAL)
                .timeout(30 * 1000)
                .listener(requestListener)
                .into(new DrawableImageViewTarget(image)).getRequest();

        startImageListener();
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setWebView(String imageUrl, ImageType hint)
    {
        progressBar.setVisibility(VISIBLE);
        webView.setOnTouchListener(this);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebChromeClient(new WebChromeClient());
        startWebViewListener();
        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view, url);
                progressBar.setVisibility(GONE);
            }
        });

        //determine how to display this URL
        final DisplayType useType = new DisplayType(imageUrl, hint);

        handler.post(() -> {
            this.imageUrl = imageUrl;
            image.setVisibility(GONE);
            webLayout.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);

            if (useType.getImageType() == ImageType.WEB)
            {
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                webView.loadUrl(imageUrl);
            }
            else if (useType.getImageType() == ImageType.ANIM)
            {
                String loaderAnim = loadFile(getContext(), R.raw.token_anim).replace("[URL]", imageUrl).replace("[MIME]", useType.getMimeType());
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
                webView.getSettings().setAllowContentAccess(true);
                webView.getSettings().setBlockNetworkLoads(false);
                webView.getSettings().setDomStorageEnabled(true);
                String base64 = android.util.Base64.encodeToString(loaderAnim.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                webView.loadData(base64, "text/html; charset=utf-8", "base64");
            }
            else if (useType.getImageType() == ImageType.MODEL)
            {
                String loader = loadFile(getContext(), R.raw.token_model).replace("[URL]", imageUrl);
                String base64 = android.util.Base64.encodeToString(loader.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                webView.loadData(base64, "text/html; charset=utf-8", "base64");
            }
            else
            {
                String loader = loadFile(getContext(), R.raw.token_graphic).replace("[URL]", imageUrl);
                String base64 = android.util.Base64.encodeToString(loader.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                webView.loadData(base64, "text/html; charset=utf-8", "base64");
                if (isThumbnail)
                {
                    setWebViewHeight(image.getHeight());
                }
            }
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
            webViewHeight = Utils.dp2px(getContext(), a.getInteger(R.styleable.ERC721ImageView_webview_height, 0));
        }
        finally
        {
            a.recycle();
        }
    }

    // View Sizing
    private void startWebViewListener()
    {
        if (isThumbnail)
        {
            return;
        }

        webView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (heightUpdates < 3)
            {
                int height = bottom - top;
                heightUpdates++;
                if (heightUpdates == 3) //3rd re-arrange would always be the final
                {
                    updateWebView(height, 0);
                }
                else
                {
                    updateWebView(height, 500);
                }
            }
        });
    }

    private void startImageListener()
    {
        if (!isThumbnail)
        {
            return;
        }

        image.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (heightUpdates == 0)
            {
                int height = bottom - top;
                if (height > 0)
                {
                    updateImageView(height);
                }
            }
        });
    }

    private void updateImageView(final int height)
    {
        handler.post(() -> {
            setImageViewHeight(height);
            heightUpdates = 3;
        });
    }

    private void updateWebView(final int height, long delay)
    {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            setWebViewHeight(Math.max(height, webViewHeight));
            heightUpdates = 3;
        }, delay);
    }

    private void setWebViewHeight(int height)
    {
        if (height > 0)
        {
            ViewGroup.LayoutParams webLayoutParams = webLayout.getLayoutParams();
            webLayoutParams.height = height;
            webLayout.setLayoutParams(webLayoutParams);
        }
    }

    private void setImageViewHeight(int height)
    {
        final int defaultHeight = Utils.dp2px(getContext(), STANDARD_THUMBNAIL_HEIGHT);
        if (height < defaultHeight)
        {
            ViewGroup.LayoutParams imageLayoutParams = image.getLayoutParams();
            imageLayoutParams.height = defaultHeight;
            image.setLayoutParams(imageLayoutParams);
        }
    }

    public void showFallbackLayout(Token token)
    {
        fallbackLayout.setVisibility(View.VISIBLE);
        fallbackIcon.bindData(token);
        fallbackIcon.setOnTokenClickListener(new TokensAdapterCallback()
        {
            @Override
            public void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected)
            {
                performClick();
            }

            @Override
            public void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds)
            {

            }
        });
    }

    private boolean shouldLoad(String url)
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

    public boolean isDisplayingImage()
    {
        return !TextUtils.isEmpty(imageUrl);
    }

    private boolean isGlb(String url)
    {
        return (url != null && MimeTypeMap.getFileExtensionFromUrl(url).equals("glb"));
    }

    private static final List<String> audioTypes = new ArrayList<>(Arrays.asList("mp3", "ogg", "wav", "flac", "aac", "opus", "weba"));

    private boolean isAudio(String url)
    {
        if (url == null)
        {
            return false;
        }
        else
        {
            return audioTypes.contains(MimeTypeMap.getFileExtensionFromUrl(url));
        }
    }

    private void playAudioIfAvailable(String url)
    {
        if (!isAudio(url))
        {
            return;
        }

        //set up MediaPlayer
        mediaPlayer = new MediaPlayer();

        try
        {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setLooping(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void onDestroy()
    {
        if (mediaPlayer != null)
        {
            try
            {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            catch (Exception e)
            {
                Timber.w(e);
            }
        }
    }

    public void onPause()
    {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
        }
    }

    public void onResume()
    {
        if (mediaPlayer != null && !mediaPlayer.isPlaying())
        {
            mediaPlayer.start();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        switch (motionEvent.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                performClick();
                break;
            }
            default:
                break;
        }
        return true;
    }

    public void setAttestationImage(Token token)
    {
        if (token.getInterfaceSpec() == ContractType.ATTESTATION)
        {
            Attestation attn = (Attestation) token;
            if (attn.isSmartPass())
            {
                setImageResource(R.drawable.smart_pass);
            }
            else
            {
                setImageResource(R.drawable.zero_one_block);
            }
        }
    }

    private static class DisplayType
    {
        private final ImageType type;
        private final String mimeStr;

        // Should handle most cases; this is a handler for anim or drop through cases,
        // Previously these were not handled so this is a big improvement in display handling
        public DisplayType(String url, ImageType hint)
        {
            if (url == null || url.length() < 5)
            {
                type = hint;
                mimeStr = "";
                return;
            }

            String extension = MimeTypeMap.getFileExtensionFromUrl(url);

            switch (extension)
            {
                case "":
                    mimeStr = "";
                    if (hint == ImageType.IMAGE || hint == ImageType.ANIM)
                    {
                        type = hint;
                    }
                    else
                    {
                        type = ImageType.WEB;
                    }
                    break;
                case "mp4":
                case "webm":
                case "avi":
                case "mpeg":
                case "mpg":
                case "m2v":
                    type = ImageType.ANIM;
                    mimeStr = "video/" + extension;
                    break;
                case "bmp":
                case "png":
                case "jpg":
                case "svg":
                case "glb": //currently avoid handling these
                default:
                    type = ImageType.IMAGE;
                    mimeStr = "image/" + extension;
                    break;
            }
        }

        public String getMimeType()
        {
            return mimeStr;
        }

        public ImageType getImageType()
        {
            return type;
        }
    }

    private enum ImageType
    {
        IMAGE, ANIM, WEB, MODEL, AUDIO
    }
}
