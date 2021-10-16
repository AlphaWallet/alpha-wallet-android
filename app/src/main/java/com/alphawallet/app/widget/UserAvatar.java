package com.alphawallet.app.widget;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.entity.AvatarWriteCallback;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.Blockies;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;

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

    public UserAvatar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), getContext());
    }

    public void bind(Wallet wallet)
    {
        bind(wallet, null);
    }

    public void bind(Wallet wallet, AvatarWriteCallback avCallback)
    {
        if (iconRequest != null && iconRequest.isRunning()) iconRequest.clear();
        if (loadAvatarDisposable != null && !loadAvatarDisposable.isDisposed()) loadAvatarDisposable.dispose();

        //does wallet have an Avatar?
        if (!TextUtils.isEmpty(wallet.ENSAvatar) && wallet.ENSAvatar.length() > 1)
        {
            loadAvatar(wallet.ENSAvatar, null, wallet);
        }
        else
        {
            setImageBitmap(Blockies.createIcon(wallet.address.toLowerCase()));
        }

        if (avCallback != null)
        {
            loadAvatarDisposable = ensResolver.getENSUrl(wallet.ENSname)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(iconUrl -> loadAvatar(iconUrl, avCallback, wallet));
        }
    }

    private void loadAvatar(String iconUrl, AvatarWriteCallback avCallback, Wallet wallet)
    {
        if (!TextUtils.isEmpty(iconUrl))
        {
            wallet.ENSAvatar = iconUrl;
            if (avCallback != null) avCallback.avatarFound(wallet);
            iconRequest = Glide.with(getContext())
                    .load(iconUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(this).getRequest();
        }
    }
}
