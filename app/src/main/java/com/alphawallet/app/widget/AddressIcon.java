package com.alphawallet.app.widget;

import static androidx.core.content.ContextCompat.getColorStateList;
import static com.alphawallet.app.util.Utils.loadFile;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.nio.charset.StandardCharsets;

public class AddressIcon extends ConstraintLayout
{
    private final ImageView icon;
    private final TextView textIcon;
    private final WebView svgIcon;
    private final ImageView svgMask;

    private String symbol;
    private String primaryURI;
    private String address;
    private long chainId;
    private Request currentRq;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public AddressIcon(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        inflate(context, R.layout.item_address_icon, this);

        icon = findViewById(R.id.icon);
        textIcon = findViewById(R.id.text_icon);
        svgIcon = findViewById(R.id.web_view_for_svg);
        svgMask = findViewById(R.id.web_mask_circle);

        findViewById(R.id.circle).setVisibility(View.GONE);
    }

    public void clearLoad()
    {
        handler.removeCallbacks(null);
        if (currentRq != null && currentRq.isRunning())
        {
            currentRq.clear();
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void bindData(String primaryURI, long chainId, String tokenAddress, String symbol)
    {
        this.handler.removeCallbacks(null);
        this.symbol = symbol;
        this.chainId = chainId;
        this.primaryURI = primaryURI;
        this.address = tokenAddress;

        setupTextIcon();
        svgIcon.setVisibility(View.GONE);
        svgMask.setVisibility(View.GONE);

        if (TextUtils.isEmpty(tokenAddress))
        {
            loadFromPrimaryURI();
        }
        else
        {
            displayTokenIcon();
        }
    }

    /**
     * As a preference, use AW icons
     */
    private void displayTokenIcon()
    {
        currentRq = Glide.with(this)
                .load(Utils.getTokenImageUrl(address))
                .placeholder(R.drawable.ic_token_eth)
                .circleCrop()
                .listener(requestListener)
                .into(new DrawableImageViewTarget(icon)).getRequest();
    }

    //Mixture of (mostly) SVG and some PNG. Webview is inefficient, but is simplest solution
    private void loadFromPrimaryURI()
    {
        if (!TextUtils.isEmpty(primaryURI))
        {
            setWebView(Utils.parseIPFS(primaryURI));
        }
    }

    //Use webview for SVG
    private void setWebView(String imageUrl)
    {
        String loader = loadFile(getContext(), R.raw.token_graphic).replace("[URL]", imageUrl);
        String base64 = android.util.Base64.encodeToString(loader.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

        textIcon.setVisibility(View.GONE);
        icon.setVisibility(View.GONE);
        svgIcon.setVisibility(View.VISIBLE);
        svgMask.setVisibility(View.VISIBLE);
        svgIcon.loadData(base64, "text/html; charset=utf-8", "base64");
    }

    private void setupTextIcon()
    {
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(getColorStateList(getContext(), EthereumNetworkBase.getChainColour(chainId)));
        textIcon.setText(Utils.getIconisedText(symbol));
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            handler.post(() -> loadFromPrimaryURI());
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            textIcon.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(resource);
            findViewById(R.id.circle).setVisibility(View.VISIBLE);
            return false;
        }
    };

    public void blankIcon()
    {
        clearLoad();
        icon.setImageDrawable(null);
    }
}
