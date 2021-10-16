package com.alphawallet.app.widget;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import static java.lang.Thread.sleep;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.entity.AvatarWriteCallback;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.Blockies;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by JB on 15/10/2021.
 */
public class UserAvatar extends androidx.appcompat.widget.AppCompatImageView
{
    private final AWEnsResolver ensResolver;
    private Disposable loadAvatarDisposable;
    private Request iconRequest;
    private String walletAddress;
    private final CustomViewTarget<ImageView, Drawable> viewTarget;

    public UserAvatar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), getContext());
        viewTarget = new CustomViewTarget<ImageView, Drawable>(this)
        {
            @Override
            protected void onResourceCleared(@Nullable Drawable placeholder) { }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable)
            {
                // handle instances where avatar is set up incorrectly
                setImageBitmap(Blockies.createIcon(walletAddress.toLowerCase()));
            }

            @Override
            public void onResourceReady(@NotNull Drawable bitmap, Transition<? super Drawable> transition)
            {
                setImageDrawable(bitmap);
            }
        };
    }

    public void bind(Wallet wallet)
    {
        bind(wallet, null);
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
            setImageBitmap(Blockies.createIcon(wallet.address.toLowerCase()));
            if (wallet.ENSAvatar != null && wallet.ENSAvatar.length() == 1) return;
        }

        if (avCallback != null)
        {
            loadAvatarDisposable = ensResolver.getENSUrl(wallet.ENSname)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(iconUrl -> loadAvatar(iconUrl, avCallback, wallet, false), this::onError);
        }
    }

    private void onError(Throwable throwable)
    {
        if (BuildConfig.DEBUG) throwable.printStackTrace();
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

    private void loadAvatar(String iconUrl, AvatarWriteCallback avCallback, Wallet wallet, boolean alwaysLoad)
    {
        if ((loadAvatarDisposable != null || alwaysLoad) && !TextUtils.isEmpty(iconUrl))
        {
            wallet.ENSAvatar = iconUrl;
            if (avCallback != null) avCallback.avatarFound(wallet);
            iconRequest = Glide.with(getContext())
                    .load(iconUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(viewTarget).getRequest();
        }
        else
        {
            wallet.ENSAvatar = "-";
        }
    }
}
