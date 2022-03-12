package com.alphawallet.app.widget;

import static androidx.core.content.ContextCompat.getColorStateList;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.EthereumNetworkBase;
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
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;
import org.web3j.crypto.Keys;

public class TokenIcon extends ConstraintLayout
{
    private final ImageView icon;
    private final TextView textIcon;
    private final ImageView statusIcon;
    private final ProgressBar pendingProgress;
    private final ImageView statusBackground;
    private final ImageView chainIcon;
    private final ImageView chainIconBackground;

    private TokensAdapterCallback tokensAdapterCallback;
    private Token token;
    private String tokenName;
    private StatusType currentStatus;
    private String fallbackIconUrl;
    private Request currentRq;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final boolean squareToken;

    public TokenIcon(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        squareToken = getViewId(context, attrs);

        inflate(context, squareToken ? R.layout.item_token_icon_square : R.layout.item_token_icon, this);

        icon = findViewById(R.id.icon);
        textIcon = findViewById(R.id.text_icon);
        statusIcon = findViewById(R.id.status_icon);
        pendingProgress = findViewById(R.id.pending_progress);
        statusBackground = findViewById(R.id.status_icon_background);
        statusIcon.setVisibility(isInEditMode() ? View.VISIBLE : View.GONE);
        currentStatus = StatusType.NONE;
        chainIcon = findViewById(R.id.status_chain_icon);
        chainIconBackground = findViewById(R.id.chain_icon_background);

        bindViews();
    }

    private boolean getViewId(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TokenIcon,
                0, 0
        );

        boolean sq;

        try
        {
            sq = a.getBoolean(R.styleable.TokenIcon_square, false);
        }
        finally
        {
            a.recycle();
        }

        return sq;
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
        Pair<String, Boolean> iconFallback = assetDefinition.getFallbackUrlForToken(token);
        String mainIcon = iconFallback.second ? iconFallback.first : getPrimaryIconURL(token);
        this.fallbackIconUrl = iconFallback.second ? getPrimaryIconURL(token) : iconFallback.first;

        String correctedAddr = Keys.toChecksumAddress(token.getAddress());
        String tURL = Utils.getTokenImageUrl(correctedAddr);

        bind(token, new IconItem(mainIcon, tURL, token.tokenInfo.chainId));
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

        statusBackground.setVisibility(View.GONE);
        chainIconBackground.setVisibility(View.GONE);
        chainIcon.setVisibility(View.GONE);

        displayTokenIcon(iconItem);
    }

    public void setChainIcon(long chainId)
    {
        chainIconBackground.setVisibility(View.VISIBLE);
        chainIcon.setVisibility(View.VISIBLE);
        chainIcon.setImageResource(EthereumNetworkRepository.getSmallChainLogo(chainId));
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
                    .into(new DrawableImageViewTarget(icon)).getRequest();
        }
        else
        {
            loadFromAltRepo();
        }
    }

    private String getPrimaryIconURL(Token token)
    {
        String correctedAddr = Keys.toChecksumAddress(token.getAddress());
        return Utils.getTokenImageUrl(correctedAddr);
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
                statusIcon.setImageResource(EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId));
                statusBackground.setVisibility(View.VISIBLE);
                break;
            default:
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
        if (getContext() == null) return;
        if (getContext() instanceof Activity)
        {
            Activity myAct = (Activity) getContext();
            if (myAct.isFinishing() || myAct.isDestroyed())
            {
                return;
            }
        }

        final RequestOptions optionalCircleCrop = squareToken ? new RequestOptions() : new RequestOptions().circleCrop();

        handler.post(() ->
                currentRq = Glide.with(getContext())
                .load(this.fallbackIconUrl)
                .placeholder(R.drawable.ic_token_eth)
                .apply(optionalCircleCrop)
                .listener(requestListenerTW)
                .into(new DrawableImageViewTarget(icon)).getRequest());
    }

    /**
     * This method is used to set TextIcon and make Icon hidden as there is no icon available for the token.
     * @param token Token
     */
    private void setupTextIcon(@NotNull Token token, boolean loadFailed)
    {
        icon.setImageResource(R.drawable.ic_clock);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(getColorStateList(getContext(), EthereumNetworkBase.getChainColour(token.tokenInfo.chainId)));
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
            if (token != null)
            {
                IconItem.noIconFound(token.getAddress()); //don't try to load this asset again for this session
            }
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
        token = null;
    }
}
