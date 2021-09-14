package com.alphawallet.app.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.Utils;
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
import org.web3j.crypto.Keys;

import static androidx.core.content.ContextCompat.getColorStateList;

public class TokenIcon extends ConstraintLayout
{
    private final ImageView icon;
    private final TextView textIcon;
    private final ImageView statusIcon;
    private final ProgressBar pendingProgress;

    private TokensAdapterCallback tokensAdapterCallback;
    private Token token;
    private final CustomViewTarget<ImageView, Drawable> viewTarget;
    private String tokenName;
    private StatusType currentStatus;
    private String fallbackIconUrl;
    private Request currentRq;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public TokenIcon(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        inflate(context, R.layout.item_token_icon, this);

        icon = findViewById(R.id.icon);
        textIcon = findViewById(R.id.text_icon);
        statusIcon = findViewById(R.id.status_icon);
        pendingProgress = findViewById(R.id.pending_progress);
        statusIcon.setVisibility(isInEditMode() ? View.VISIBLE : View.GONE);
        currentStatus = StatusType.NONE;

        bindViews();

        viewTarget = new CustomViewTarget<ImageView, Drawable>(icon)
        {
            @Override
            protected void onResourceCleared(@Nullable Drawable placeholder) { }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) { }

            @Override
            public void onResourceReady(@NotNull Drawable bitmap, Transition<? super Drawable> transition)
            {
                textIcon.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageDrawable(bitmap);
            }
        };
    }

    private void bindViews()
    {
        View layout = findViewById(R.id.view_container);
        layout.setOnClickListener(this::performTokenClick);
    }

    /**
     * This method is necessary to call from the binder to show information correctly.
     *
     * @param token Token object
     * @param assetDefinition Asset Definition Service for Icons
     */
    public void bindData(Token token, @NotNull AssetDefinitionService assetDefinition)
    {
        if (token == null || (this.token != null && this.token.equals(token))) { return; } //stop update flicker
        this.tokenName = token.getName(assetDefinition, token.getTokenCount());
        this.fallbackIconUrl = assetDefinition.getFallbackUrlForToken(token);

        bind(token, getIconUrl(token));
    }

    public void bindData(Token token)
    {
        if (token == null) return;
        this.tokenName = token.getName();
        this.fallbackIconUrl = Utils.getTWTokenImageUrl(token.tokenInfo.chainId, token.getAddress());
        bind(token, getIconUrl(token));
    }

    public void bindData(Token token, @NotNull TokensService svs)
    {
        if (token == null) return;
        this.tokenName = token.getName();
        this.fallbackIconUrl = svs.getFallbackUrlForToken(token);
        bind(token, getIconUrl(token));
    }

    private void bind(Token token, IconItem iconItem)
    {
        this.handler.removeCallbacks(null);
        this.token = token;

        displayTokenIcon(iconItem);
    }

    private void setupDefaultIcon(boolean loadFailed)
    {
        if (currentRq != null && currentRq.isRunning()) currentRq.clear(); //clear current load request if this is replacing an old asset that didn't finish loading
        if (token.isEthereum() || EthereumNetworkRepository.getChainOverrideAddress(token.tokenInfo.chainId).equalsIgnoreCase(token.getAddress()))
        {
            textIcon.setVisibility(View.GONE);
            icon.setImageResource(EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId));
            icon.setVisibility(View.VISIBLE);
            findViewById(R.id.circle).setVisibility(View.VISIBLE);
        }
        else
        {
            setupTextIcon(token, loadFailed);
        }
    }

    /**
     * Try to fetch Token Icon from the Token URL.
     */
    private void displayTokenIcon(IconItem iconItem)
    {
        setupDefaultIcon(iconItem.useTextSymbol());
        if (token.isEthereum()
                || token.getWallet().equalsIgnoreCase(token.getAddress())
                || iconItem.useTextSymbol()) return;

        if (iconItem.usePrimary())
        {
            currentRq = Glide.with(getContext())
                    .load(iconItem.getUrl())
                    .placeholder(R.drawable.ic_token_eth)
                    .listener(requestListener)
                    .into(viewTarget).getRequest();
        }
        else
        {
            loadFromAltRepo();
        }
    }

    private IconItem getIconUrl(Token token)
    {
        String correctedAddr = Keys.toChecksumAddress(token.getAddress());
        String tURL = Utils.getTokenImageUrl(correctedAddr);
        return new IconItem(tURL, correctedAddr, token.tokenInfo.chainId);
    }

    public void setStatusIcon(StatusType type)
    {
        boolean requireAnimation = statusIcon.getVisibility() == View.VISIBLE && type != currentStatus;
        statusIcon.setVisibility(View.VISIBLE);
        pendingProgress.setVisibility(View.GONE);
        switch (type)
        {
            case SENT:
                statusIcon.setImageResource(R.drawable.ic_sent_white_small);
                break;
            case RECEIVE:
                statusIcon.setImageResource(R.drawable.ic_receive_small);
                break;
            case PENDING:
                pendingProgress.setVisibility(View.VISIBLE);
                break;
            case FAILED:
                statusIcon.setImageResource(R.drawable.ic_rejected_small);
                break;
            case REJECTED:
                statusIcon.setImageResource(R.drawable.ic_transaction_rejected);
                break;
            case CONSTRUCTOR:
                statusIcon.setImageResource(R.drawable.ic_ethereum_logo);
                break;
            case SELF:
                statusIcon.setImageResource(R.drawable.ic_send_self_small);
                break;
            case NONE:
                statusIcon.setVisibility(View.GONE);
                break;
        }

        currentStatus = type;

        if (requireAnimation)
        {
            statusIcon.setAlpha(0.0f);
            statusIcon.animate().alpha(1.0f).setDuration(500);
        }
    }

    /**
     * Attempt to load the icon from the Database icon or TW icon repo
     */
    private void loadFromAltRepo()
    {
        handler.post(() ->
                currentRq = Glide.with(getContext())
                .load(this.fallbackIconUrl)
                .placeholder(R.drawable.ic_token_eth)
                .apply(new RequestOptions().circleCrop())
                .listener(requestListenerTW)
                .into(viewTarget).getRequest());
    }

    /**
     * This method is used to set TextIcon and make Icon hidden as there is no icon available for the token.
     * @param token Token
     */
    private void setupTextIcon(@NotNull Token token, boolean loadFailed)
    {
        if (loadFailed) icon.setVisibility(View.GONE);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(getColorStateList(getContext(), Utils.getChainColour(token.tokenInfo.chainId)));
        //try symbol first
        if (!TextUtils.isEmpty(token.tokenInfo.symbol) && token.tokenInfo.symbol.length() > 1)
        {
            textIcon.setText(Utils.getIconisedText(token.tokenInfo.symbol));
        }
        else
        {
            textIcon.setText(Utils.getIconisedText(tokenName));
        }
    }

    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback)
    {
        this.tokensAdapterCallback = tokensAdapterCallback;
    }

    private void performTokenClick(View v)
    {
        if (tokensAdapterCallback != null)
        {
            tokensAdapterCallback.onTokenClick(v, token, null, true);
        }
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            if (model == null || token == null || !model.toString().toLowerCase().contains(token.getAddress())) return false;

            loadFromAltRepo();
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            textIcon.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(resource);
            return false;
        }
    };

    private final RequestListener<Drawable> requestListenerTW = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource)
        {
            if (model == null || token == null || !model.toString().toLowerCase().contains(token.getAddress())) return false;

            icon.setVisibility(View.GONE);
            if (token != null)
                IconItem.noIconFound(token.getAddress()); //don't try to load this asset again for this session
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            if (model == null) return false;
            IconItem.secondaryFound(Utils.getTokenAddrFromUrl(model.toString()));
            if (token == null || !model.toString().toLowerCase().contains(token.getAddress())) return false;

            textIcon.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(resource);
            return false;
        }
    };

    public void showLocalCurrency()
    {
        String isoCode = TickerService.getCurrencySymbolTxt();
        icon.setImageResource(CurrencyRepository.getFlagByISO(isoCode));
    }
}
