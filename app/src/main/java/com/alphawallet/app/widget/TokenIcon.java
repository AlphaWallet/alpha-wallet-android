package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
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

public class TokenIcon extends ConstraintLayout {

    private ImageView icon;
    private TextView textIcon;
    private ImageView statusIcon;

    private OnTokenClickListener onTokenClickListener;
    private Token token;
    private CustomViewTarget viewTarget;
    private String tokenName;
    private AssetDefinitionService assetDefinition;
    private boolean showStatus = false;

    public TokenIcon(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.item_token_icon, this);

        getAttrs(context, attrs);

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
        }
        finally
        {
            a.recycle();
        }
    }

    private void bindViews()
    {
        icon = findViewById(R.id.icon);
        textIcon = findViewById(R.id.text_icon);
        statusIcon = findViewById(R.id.status_icon);
        View layout = findViewById(R.id.view_container);

        statusIcon.setVisibility(View.GONE);

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
            IconItem iconItem = assetDefinition.fetchIconForToken(token);

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
        statusIcon.setVisibility(View.VISIBLE);
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
        }
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
        textIcon.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), Utils.getChainColour(token.tokenInfo.chainId)));
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
    private RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
