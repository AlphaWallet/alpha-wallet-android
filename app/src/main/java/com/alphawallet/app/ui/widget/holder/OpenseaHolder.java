package com.alphawallet.app.ui.widget.holder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.AssetDisplayActivity;
import com.alphawallet.app.ui.TokenDetailActivity;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.util.KittyUtils;
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
public class OpenseaHolder extends BinderViewHolder<TicketRange> implements Runnable {
    public static final int VIEW_TYPE = 1302;
    protected final Token token;
    private final TextView titleText;
    private final TextView generation;
    private final TextView cooldown;
    private final LinearLayout layoutDetails;
    private final LinearLayout clickLayer;
    private final ProgressBar loadingSpinner;
    private final NFTImageView tokenImageView;
    private TokensAdapterCallback tokenClickListener;
    private final AppCompatRadioButton itemSelect;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean activeClick;
    private final Activity activity;
    private final boolean clickThrough;

    @Nullable
    private Disposable assetLoader;

    public OpenseaHolder(int resId, ViewGroup parent, @NotNull Token token, Activity activity, boolean clickThrough) {
        super(resId, parent);
        titleText = findViewById(R.id.name);
        generation = findViewById(R.id.generation);
        cooldown = findViewById(R.id.cooldown);
        itemSelect = findViewById(R.id.radioBox);
        layoutDetails = findViewById(R.id.layout_details);
        loadingSpinner = findViewById(R.id.loading_spinner);
        tokenImageView = findViewById(R.id.asset_detail);
        clickLayer = findViewById(R.id.click_layer);
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

        layoutDetails.setVisibility(View.GONE);
        tokenImageView.blankViews();

        displayAsset(data, asset, true);

        addClickListener(data);
    }

    private NFTAsset storeAsset(BigInteger tokenId, NFTAsset fetchedAsset, NFTAsset oldAsset)
    {
        fetchedAsset.updateFromRaw(oldAsset);
        if (activity != null && activity instanceof AssetDisplayActivity)
        {
            ((AssetDisplayActivity)activity).storeAsset(tokenId, fetchedAsset);
        }

        token.addAssetToTokenBalanceAssets(tokenId, fetchedAsset);
        return fetchedAsset;
    }

    private void handleError(Throwable e, BigInteger tokenId)
    {
        if (BuildConfig.DEBUG) e.printStackTrace();
        NFTAsset asset = token.getAssetForToken(tokenId.toString());
        loadingSpinner.setVisibility(View.GONE);
        String assetName;
        if (asset.getName() != null && !asset.getName().equals("null")) {
            assetName = asset.getName();
        } else {
            assetName = "ID# " + tokenId.toString();
        }
        loadingSpinner.setVisibility(View.GONE);
        titleText.setText(assetName);
    }

    private void displayAsset(TicketRange data, NFTAsset asset, boolean entry)
    {
        BigInteger tokenId = data.tokenIds.get(0);

        if (entry && asset.needsLoading())
        {
            loadingSpinner.setVisibility(View.VISIBLE);
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
        if (asset.getName() != null && !asset.getName().equals("null")) {
            assetName = asset.getName();
        } else {
            assetName = "ID# " + tokenId.toString();
        }
        loadingSpinner.setVisibility(View.GONE);
        titleText.setText(assetName);

        if (data.exposeRadio)
        {
            asset.exposeRadio = true;
            itemSelect.setVisibility(View.VISIBLE);
            itemSelect.setChecked(data.isChecked);
        }
        else
        {
            asset.exposeRadio = false;
            itemSelect.setVisibility(View.GONE);
        }

        if (asset.getAttributeValue("generation") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getAttributeValue("generation")));
            layoutDetails.setVisibility(View.VISIBLE);
        } else if (asset.getAttributeValue("gen") != null){
            generation.setText(String.format("Gen %s",
                    asset.getAttributeValue("gen")));
            layoutDetails.setVisibility(View.VISIBLE);
        } else {
            generation.setVisibility(View.GONE);
        }

        if (asset.getAttributeValue("cooldown_index") != null) {
            cooldown.setText(String.format("%s Cooldown",
                    KittyUtils.parseCooldownIndex(
                            asset.getAttributeValue("cooldown_index"))));
            layoutDetails.setVisibility(View.VISIBLE);
        } else if (asset.getAttributeValue("cooldown") != null) { // Non-CK
            cooldown.setText(String.format("%s Cooldown",
                    asset.getAttributeValue("cooldown")));
            layoutDetails.setVisibility(View.VISIBLE);
        } else {
            cooldown.setVisibility(View.GONE);
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
                itemSelect.setChecked(true);
            }
            else
            {
                tokenClickListener.onTokenClick(v, token, data.tokenIds, false);
                data.isChecked = false;
                itemSelect.setChecked(false);
            }
        }
        else
        {
            if (activeClick) return;
            activeClick = true;
            handler.postDelayed(this, 500);
            Intent intent = new Intent(getContext(), TokenDetailActivity.class);
            intent.putExtra(C.EXTRA_NFTASSET, token.getAssetForToken(tokenId.toString()));
            intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
            intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
            intent.putExtra(C.EXTRA_TOKEN_ID, tokenId.toString());
            if (activity != null)
            {
                activity.startActivityForResult(intent, C.TERMINATE_ACTIVITY);
            }
            else
            {
                getContext().startActivity(intent);
            }
        }
    }

    private boolean handleLongClick(View v, TicketRange data)
    {
        //open up the radio view and signal to holding app
        tokenClickListener.onLongTokenClick(v, token, data.tokenIds);
        data.isChecked = true;
        itemSelect.setChecked(true);
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
