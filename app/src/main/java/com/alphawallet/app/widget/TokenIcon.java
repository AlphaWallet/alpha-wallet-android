package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
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
    private final ProgressKnobkerry pendingProgress;

    private OnTokenClickListener onTokenClickListener;
    private Token token;
    private CustomViewTarget viewTarget;
    private String tokenName;
    private AssetDefinitionService assetDefinition;
    private boolean showStatus = false;
    private boolean largeIcon = false;
    private boolean smallIcon = false;
    private StatusType currentStatus;

    public TokenIcon(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        getAttrs(context, attrs);

        if (largeIcon)
        {
            inflate(context, R.layout.item_token_icon_large, this);
        }
        else
        {
            inflate(context, R.layout.item_token_icon, this);
        }

        icon = findViewById(R.id.icon);
        textIcon = findViewById(R.id.text_icon);
        statusIcon = findViewById(R.id.status_icon);
        pendingProgress = findViewById(R.id.pending_progress);
        statusIcon.setVisibility(isInEditMode() ? View.VISIBLE : View.GONE);
        currentStatus = StatusType.NONE;

        bindViews();
    }

    private void getAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TokenIcon,
                0, 0
        );

        try
        {
            showStatus = a.getBoolean(R.styleable.TokenIcon_showStatus, false);
            largeIcon = a.getBoolean(R.styleable.TokenIcon_largeIcon, false);
        }
        finally
        {
            a.recycle();
        }
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
    public void bindData(Token token, AssetDefinitionService assetDefinition)
    {
        if (token == null) return;
        this.token = token;
        this.tokenName = token.getFullName(assetDefinition, token.getTicketCount());
        this.assetDefinition = assetDefinition;

        viewTarget = new CustomViewTarget<ImageView, BitmapDrawable>(icon) {
            @Override
            protected void onResourceCleared(@Nullable Drawable placeholder) { }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable)
            {
                setupTextIcon(token);
            }

            @Override
            public void onResourceReady(@NotNull BitmapDrawable bitmap, Transition<? super BitmapDrawable> transition)
            {
                textIcon.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageDrawable(bitmap);
            }
        };

        displayTokenIcon();
    }

    /**
     * Try to fetch Token Icon from the Token URL.
     */
    private void displayTokenIcon()
    {
        int chainIcon = EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId);

        // This appears more complex than necessary;
        // This is because we are dealing with: new token holder view, refreshing views and recycled views
        // If the token is a basechain token, immediately show the chain icon - no need to load
        // Otherwise, try to load the icon resource. If there's no icon resource then generate a text token Icon (round circle with first four chars from the name)
        // Only reveal the icon immediately before populating it - this stops the update flicker.
        if (token.isEthereum())
        {
            textIcon.setVisibility(View.GONE);
            icon.setImageResource(chainIcon);
            icon.setVisibility(View.VISIBLE);
        }
        else
        {
            setupTextIcon(token);
            IconItem iconItem = assetDefinition != null ? assetDefinition.fetchIconForToken(token) : getIconUrl(token);

            Glide.with(getContext().getApplicationContext())
                    .load(iconItem.getUrl())
                    .signature(iconItem.getSignature())
                    .onlyRetrieveFromCache(iconItem.onlyFetchFromCache()) //reduce URL checking, only check once per session
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(chainIcon))
                    .listener(requestListener)
                    .into(viewTarget);
        }
    }

    private IconItem getIconUrl(Token token)
    {
        String correctedAddr = Keys.toChecksumAddress(token.getAddress());
        String tURL = Utils.getTokenImageUrl(token.tokenInfo.chainId, correctedAddr);
        return new IconItem(tURL, false, correctedAddr, token.tokenInfo.chainId);
    }

    /**
     * This method is used to set Custom image to the Token Icon
     * @param imageResource Drawable resource identifier
     */
    public void setTokenImage(int imageResource)
    {
        icon.setImageResource(imageResource);
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
                statusIcon.setImageResource(R.drawable.ic_timer_small);
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

    public void startPendingSpinner(long startTime, long completionTime)
    {
        pendingProgress.startAnimation(startTime, completionTime);
    }

    /**
     * This method is used to change visibility of "Status" Icon i.e. Send Or Receive.
     * @param isVisible Boolean parameter to either make Transaction icon visible or hidden
     */
    public void changeStatusVisibility(boolean isVisible)
    {
        showStatus = isVisible;
        statusIcon.setVisibility(showStatus ? View.VISIBLE : View.GONE);
    }

    /**
     * This method is used to set TextIcon and make Icon hidden as there is no icon available for the token.
     * @param token Token
     */
    private void setupTextIcon(@NotNull Token token)
    {
        icon.setVisibility(View.GONE);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(getColorStateList(getContext(), Utils.getChainColour(token.tokenInfo.chainId)));
        textIcon.setText(Utils.getIconisedText(tokenName));
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener)
    {
        this.onTokenClickListener = onTokenClickListener;
    }

    private void performTokenClick(View v)
    {
        if (onTokenClickListener != null)
        {
            onTokenClickListener.onTokenClick(v, token, null, true);
        }
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            textIcon.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(resource);
            return true;
        }
    };

    public void showLocalCurrency()
    {
        String isoCode = TickerService.getCurrencySymbolTxt();
        icon.setImageResource(CurrencyRepository.getFlagByISO(isoCode));
    }
}
