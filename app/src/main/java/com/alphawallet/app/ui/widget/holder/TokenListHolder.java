package com.alphawallet.app.ui.widget.holder;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTokenManageClickListener;
import com.alphawallet.app.ui.widget.adapter.TokenListAdapter;
import com.alphawallet.app.ui.widget.entity.IconItem;
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

public class TokenListHolder extends BinderViewHolder<TokenCardMeta> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    final RelativeLayout layout;
    final TextView tokenName;
    final Switch switchEnabled;
    final ImageView icon;
    final TextView textIcon;
    final View overlay;
    private final CustomViewTarget viewTarget;
    int chainId;

    //need to cache this locally, unless we cache every string we need in the constructor
    private final AssetDefinitionService assetDefinition;
    private final TokensService tokensService;

    public Token token;
    public TokenCardMeta data;
    public int position;
    private OnTokenManageClickListener onTokenClickListener;

    public TokenListHolder(int resId, ViewGroup parent, AssetDefinitionService assetService, TokensService tokensService)
    {
        super(resId, parent);

        this.assetDefinition = assetService;
        this.tokensService = tokensService;

        layout = itemView.findViewById(R.id.layout_list_item);
        tokenName = itemView.findViewById(R.id.name);
        switchEnabled = itemView.findViewById(R.id.switch_enabled);
        icon = itemView.findViewById(R.id.icon);
        textIcon = itemView.findViewById(R.id.text_icon);
        overlay = itemView.findViewById(R.id.view_overlay);

        icon.setVisibility(View.INVISIBLE);
        textIcon.setVisibility(View.GONE);
        layout.setOnClickListener(this);
        itemView.setOnClickListener(this);

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
    }

    @Override
    public void bind(@Nullable TokenCardMeta data, @NonNull Bundle addition)
    {
        this.data = data;
        position = addition.getInt("position");
        token = tokensService.getToken(data.getChain(), data.getAddress());

        tokenName.setText(token.getFullName(assetDefinition, 1));
        chainId = token.tokenInfo.chainId;
        switchEnabled.setOnCheckedChangeListener(null);
        switchEnabled.setChecked(data.isEnabled);
        switchEnabled.setOnCheckedChangeListener(this);

        displayTokenIcon();

        if (data.isEnabled)
        {
            overlay.setVisibility(View.GONE);
        }
        else
        {
            overlay.setVisibility(View.VISIBLE);
        }
    }

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

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null)
        {
            switchEnabled.setChecked(!switchEnabled.isChecked());
        }
    }

    public void setOnTokenClickListener(OnTokenManageClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    private void setupTextIcon(@NotNull Token token) {
        icon.setVisibility(View.GONE);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), Utils.getChainColour(token.tokenInfo.chainId)));
        textIcon.setText(Utils.getIconisedText(token.getFullName(assetDefinition, 1)));
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
    {
        if (onTokenClickListener != null)
        {
            onTokenClickListener.onTokenClick(token, position, isChecked);
        }
    }
}