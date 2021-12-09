package com.alphawallet.app.ui.widget.holder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.AssetDisplayActivity;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.token.entity.TicketRange;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaGridHolder extends BinderViewHolder<TicketRange> implements Runnable {
    public static final int VIEW_TYPE = 1305;
    protected final Token token;
    private final TextView titleText;
    private final TextView tokenIdText;
    private final NFTImageView tokenImageView;
    private final RelativeLayout clickLayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Activity activity;
    private final boolean clickThrough;
    private TokensAdapterCallback tokenClickListener;
    private boolean activeClick;
    @Nullable
    private Disposable assetLoader;

    public OpenseaGridHolder(int resId, ViewGroup parent, @NotNull Token token, Activity activity, boolean clickThrough)
    {
        super(resId, parent);
        titleText = findViewById(R.id.name);
        tokenIdText = findViewById(R.id.token_id);
        tokenImageView = findViewById(R.id.asset_detail);
        clickLayer = findViewById(R.id.holding_view);
        this.token = token;
        this.activity = activity;
        this.clickThrough = clickThrough;
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        activeClick = false;
        if (data == null) return;

        if (assetLoader != null && !assetLoader.isDisposed())
        {
            assetLoader.dispose();
        }

        //retrieve asset from token
        BigInteger tokenId = data.tokenIds.get(0);
        NFTAsset asset = token.getAssetForToken(tokenId);

        tokenImageView.blankViews();

        displayAsset(data, asset, true);

        addClickListener(data);
    }

    private NFTAsset storeAsset(BigInteger tokenId, NFTAsset fetchedAsset, NFTAsset oldAsset)
    {
        fetchedAsset.updateFromRaw(oldAsset);
        if (activity != null && activity instanceof AssetDisplayActivity)
        {
            ((AssetDisplayActivity) activity).storeAsset(tokenId, fetchedAsset);
        }

        token.addAssetToTokenBalanceAssets(tokenId, fetchedAsset);
        return fetchedAsset;
    }

    private void handleError(Throwable e, BigInteger tokenId)
    {
        if (BuildConfig.DEBUG) e.printStackTrace();
        NFTAsset asset = token.getAssetForToken(tokenId.toString());
        String assetName;
        if (asset.getName() != null && !asset.getName().equals("null"))
        {
            assetName = asset.getName();
        } else
        {
            assetName = "ID# " + tokenId.toString();
        }
        titleText.setText(assetName);
    }

    private void displayAsset(TicketRange data, NFTAsset asset, boolean entry)
    {
        BigInteger tokenId = data.tokenIds.get(0);

        if (entry && asset.needsLoading())
        {
            titleText.setText(asset.getName());

            assetLoader = Single.fromCallable(() -> {
                return token.fetchTokenMetadata(data.tokenIds.get(0));//fetch directly from token
            }).map(newAsset -> storeAsset(tokenId, newAsset, asset))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(a -> displayAsset(data, a, false), e -> handleError(e, tokenId));

            return;
        }

        String assetName;
        if (asset.getName() != null && !asset.getName().equals("null"))
        {
            assetName = asset.getName();
            titleText.setText(assetName.substring(0, assetName.indexOf("#")).trim());
            tokenIdText.setText(String.format("#%s", tokenId));
        } else
        {
            assetName = "ID# " + tokenId.toString();
            titleText.setText(assetName);
            tokenIdText.setVisibility(View.GONE);
        }

        if (data.exposeRadio)
        {
            asset.exposeRadio = true;
        } else
        {
            asset.exposeRadio = false;
        }

        tokenImageView.setupTokenImageThumbnail(asset);
    }

    private void addClickListener(TicketRange data)
    {
        if (clickThrough)
        {
            clickLayer.setOnClickListener(v -> handleClick(v, data));
            clickLayer.setOnLongClickListener(v -> handleLongClick(v, data));
        }
    }

    private void handleClick(View v, TicketRange data)
    {
        BigInteger tokenId = data.tokenIds.get(0);

        if (data.exposeRadio)
        {
            if (!data.isChecked)
            {
                tokenClickListener.onTokenClick(v, token, data.tokenIds, true);
                data.isChecked = true;
            } else
            {
                tokenClickListener.onTokenClick(v, token, data.tokenIds, false);
                data.isChecked = false;
            }
        } else
        {
//            if (activeClick) return;
//
//            activeClick = true;

            tokenClickListener.onTokenClick(v, token, data.tokenIds, false);
//
//            handler.postDelayed(this, 500);
//            Intent intent = new Intent(getContext(), Erc1155AssetDetailActivity.class);
//            intent.putExtra(C.EXTRA_NFTASSET, token.getAssetForToken(tokenId.toString()));
//            intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
//            intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
//            intent.putExtra(C.EXTRA_TOKEN_ID, tokenId.toString());
//            if (activity != null)
//            {
//                activity.startActivityForResult(intent, C.TERMINATE_ACTIVITY);
//            }
//            else
//            {
//                getContext().startActivity(intent);
//            }
        }
    }

    private boolean handleLongClick(View v, TicketRange data)
    {
        //open up the radio view and signal to holding app
        tokenClickListener.onLongTokenClick(v, token, data.tokenIds);
        data.isChecked = true;
        return true;
    }

    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback)
    {
        tokenClickListener = tokensAdapterCallback;
    }

    @Override
    public void run()
    {
        activeClick = false;
    }
}
