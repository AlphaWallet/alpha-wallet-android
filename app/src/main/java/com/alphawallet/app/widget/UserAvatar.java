package com.alphawallet.app.widget;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.util.Utils.loadFile;
import static com.alphawallet.app.util.ens.EnsResolver.USE_ENS_CHAIN;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.entity.AvatarWriteCallback;
import com.alphawallet.app.util.ens.AWEnsResolver;
import com.alphawallet.app.util.Blockies;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.nio.charset.StandardCharsets;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by JB on 15/10/2021.
 */
public class UserAvatar extends LinearLayout
{
    private final AWEnsResolver ensResolver;
    private Disposable loadAvatarDisposable;
    private Request iconRequest;
    private String walletAddress;

    private final ImageView image;
    private final RelativeLayout webLayout;
    private final WebView webView;
    private BindingState state;

    public UserAvatar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_asset_image, this);
        image = findViewById(R.id.image_asset);
        webLayout = findViewById(R.id.web_view_wrapper);
        webView = findViewById(R.id.image_web_view);
        findViewById(R.id.overlay).setVisibility(View.VISIBLE);

        ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(USE_ENS_CHAIN), getContext());
        state = BindingState.NONE;
    }

    public void bind(Wallet wallet)
    {
        bind(wallet, null);
    }

    public void resetBinding()
    {
        image.setVisibility(View.GONE);
        webLayout.setVisibility(View.GONE);
        state = BindingState.NONE;
        walletAddress = null;
    }

    public void setWaiting()
    {
        findViewById(R.id.avatar_progress_spinner).setVisibility(View.VISIBLE);
    }

    public void finishWaiting()
    {
        findViewById(R.id.avatar_progress_spinner).setVisibility(View.GONE);
    }

    public void bindAndFind(@NonNull Wallet wallet)
    {
        walletAddress = wallet.address;
        if ((state == BindingState.NONE || state == BindingState.BLOCKIE) && !wallet.address.equalsIgnoreCase(ZERO_ADDRESS))
        {
            bind(wallet, null);
        }

        if ((state == BindingState.NONE || state == BindingState.BLOCKIE) && !TextUtils.isEmpty(wallet.ENSname))
        {
            resolveAvatar(wallet, null);
        }
        else if (state == BindingState.SCANNING_ENS)
        {
            setBlockie(wallet.address);
        }
    }

    public void bind(final Wallet wallet, AvatarWriteCallback avCallback)
    {
        if (iconRequest != null && iconRequest.isRunning()) iconRequest.clear();
        if (loadAvatarDisposable != null && !loadAvatarDisposable.isDisposed()) loadAvatarDisposable.dispose();

        walletAddress = wallet.address;

        //does wallet have an Avatar?
        if (!TextUtils.isEmpty(wallet.ENSAvatar) && wallet.ENSAvatar.length() > 1)
        {
            loadAvatar(wallet.ENSAvatar, null, wallet, true);
        }
        else
        {
            setBlockie(wallet.address);
            if (wallet.ENSAvatar != null && wallet.ENSAvatar.length() == 1) return;
        }

        if (avCallback != null)
        {
            resolveAvatar(wallet, avCallback);
        }
    }

    private void resolveAvatar(Wallet wallet, AvatarWriteCallback callback)
    {
        if (!TextUtils.isEmpty(wallet.ENSname))
        {
            state = BindingState.SCANNING_ENS;
            loadAvatarDisposable = ensResolver.getENSUrl(wallet.ENSname)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(iconUrl -> loadAvatar(iconUrl, callback, wallet, false), this::onError);
        }
    }

    private void onError(Throwable throwable)
    {
        setBlockie(walletAddress);
        Timber.e(throwable);
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        if (iconRequest != null && iconRequest.isRunning()) iconRequest.clear();
        if (loadAvatarDisposable != null && !loadAvatarDisposable.isDisposed())
        {
            loadAvatarDisposable.dispose();
            loadAvatarDisposable = null;
        }
    }

    private void setBlockie(String address)
    {
        state = BindingState.BLOCKIE;
        if (TextUtils.isEmpty(address) || address.equalsIgnoreCase(ZERO_ADDRESS)) return;
        image.setVisibility(View.VISIBLE);
        webLayout.setVisibility(View.GONE);
        image.setImageBitmap(Blockies.createIcon(address.toLowerCase()));
    }

    private void loadAvatar(String iconUrl, AvatarWriteCallback avCallback, Wallet wallet, boolean alwaysLoad)
    {
        if ((loadAvatarDisposable == null && !alwaysLoad)) { return; } //view was destroyed
        if (TextUtils.isEmpty(iconUrl))
        {
            wallet.ENSAvatar = "-";
            setBlockie(wallet.address);
            return;
        }
        else if (avCallback != null && (wallet.ENSAvatar == null || !wallet.ENSAvatar.equalsIgnoreCase(iconUrl)))
        {
            //update avatar if changed
            avCallback.avatarFound(wallet);
        }

        wallet.ENSAvatar = iconUrl;
        state = BindingState.IMAGE;

        if (iconUrl.toLowerCase().endsWith(".svg"))
        {
            setWebView(iconUrl);
        }
        else
        {
            image.setVisibility(View.VISIBLE);
            webLayout.setVisibility(View.GONE);
            iconRequest = Glide.with(getContext())
                    .load(iconUrl)
                    .apply(new RequestOptions().circleCrop())
                    .listener(requestListener)
                    .into(image).getRequest();
        }
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            setBlockie(walletAddress);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            image.setVisibility(View.VISIBLE);
            image.setImageDrawable(resource);
            return false;
        }
    };

    //Use webview for SVG
    private void setWebView(String imageUrl)
    {
        String loader = loadFile(getContext(), R.raw.token_graphic).replace("[URL]", imageUrl);
        String base64 = android.util.Base64.encodeToString(loader.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

        image.setVisibility(View.GONE);
        webLayout.setVisibility(View.VISIBLE);
        webView.loadData(base64, "text/html; charset=utf-8", "base64");
    }

    private enum BindingState
    {
        NONE, BLOCKIE, SCANNING_ENS, IMAGE
    }
}
