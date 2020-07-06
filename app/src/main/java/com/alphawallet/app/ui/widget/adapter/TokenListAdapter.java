package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.tools.Numeric;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TokenListAdapter extends RecyclerView.Adapter<TokenListAdapter.ViewHolder> {
    private LayoutInflater inflater;
    private final Context context;
    private final List<TokenCardMeta> data;
    ItemClickListener listener;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;

    public TokenListAdapter(Context context, AssetDefinitionService aService, TokensService tService, TokenCardMeta[] tokens, ItemClickListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.data = filterTokens(Arrays.asList(tokens));
        this.listener = listener;
        this.assetService = aService;
        this.tokensService = tService;
    }

    private List<TokenCardMeta> filterTokens(List<TokenCardMeta> tokens)
    {
        ArrayList<TokenCardMeta> filteredList = new ArrayList<>();
        for (TokenCardMeta t : tokens) {
            if (!t.isEthereum()) filteredList.add(t);
        }

        Collections.sort(filteredList);
        return filteredList;
    }

    @NonNull
    @Override
    public TokenListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.item_manage_token, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenListAdapter.ViewHolder viewHolder, int i) {
        TokenCardMeta tcm = data.get(i);
        final Token token = tokensService.getToken(tcm.getChain(), tcm.getAddress());
        viewHolder.setIsRecyclable(false);
        viewHolder.tokenName.setText(token.getFullName(assetService, 1));
        viewHolder.chainId = token.tokenInfo.chainId;
        viewHolder.displayTokenIcon(token);
        viewHolder.switchEnabled.setChecked(token.tokenInfo.isEnabled);
        viewHolder.switchEnabled.setTag(new Integer(i));
        viewHolder.switchEnabled.setOnCheckedChangeListener((v, b) -> {
            listener.onItemClick(token, b);
        });
    }

    public TokenCardMeta getItem(int id) {
        return data.get(id);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        RelativeLayout layout;
        TextView tokenName;
        Switch switchEnabled;
        final ImageView icon;
        final TextView textIcon;
        int chainId;

        private final CustomViewTarget viewTarget;

        ViewHolder(View itemView)
        {
            super(itemView);
            layout = itemView.findViewById(R.id.layout_list_item);
            tokenName = itemView.findViewById(R.id.name);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
            layout.setOnClickListener(this);
            icon = itemView.findViewById(R.id.icon);
            textIcon = itemView.findViewById(R.id.text_icon);

            viewTarget = new CustomViewTarget<ImageView, BitmapDrawable>(icon) {
                @Override
                protected void onResourceCleared(@Nullable Drawable placeholder) { }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable)
                {
                    setupTextIcon();
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

        private void displayTokenIcon(Token token)
        {
            int chainIcon = EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId);

            IconItem iconItem = assetService.fetchIconForToken(token);
            String tokenName = token.getFullName(assetService, token.getTicketCount());
            viewTarget.getView().setTag(tokenName);

            Glide.with(context.getApplicationContext())
                    .load(iconItem.getUrl())
                    .signature(iconItem.getSignature())
                    .onlyRetrieveFromCache(iconItem.onlyFetchFromCache()) //reduce URL checking, only check once per session
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(chainIcon))
                    .dontAnimate()
                    .into(viewTarget);
        }

        private void setupTextIcon() {
            icon.setVisibility(View.GONE);
            textIcon.setVisibility(View.VISIBLE);
            textIcon.setBackgroundTintList(ContextCompat.getColorStateList(context, Utils.getChainColour(chainId)));
            textIcon.setText(Utils.getIconisedText(tokenName.getText().toString()));
        }

        @Override
        public void onClick(View v) {
            switchEnabled.setChecked(!switchEnabled.isChecked());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            TokenCardMeta tcm = data.get(getAdapterPosition());
            final Token t = tokensService.getToken(tcm.getChain(), tcm.getAddress());
            listener.onItemClick(t, isChecked);
        }
    }

    public interface ItemClickListener {
        void onItemClick(Token token, boolean enabled);
    }
}
