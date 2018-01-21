package com.wallet.crypto.trustapp.ui.widget.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.TicketInfo;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.ui.widget.OnTokenClickListener;
import com.wallet.crypto.trustapp.ui.widget.holder.TokenHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TokensAdapter extends RecyclerView.Adapter<TokenHolder> {

    private final OnTokenClickListener onTokenClickListener;
    private final List<Token> items = new ArrayList<>();

    public TokensAdapter(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    @Override
    public TokenHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TokenHolder tokenHolder = new TokenHolder(R.layout.item_token, parent);
        tokenHolder.setOnTokenClickListener(onTokenClickListener);
        return tokenHolder;
    }

    @Override
    public void onBindViewHolder(TokenHolder holder, int position) {
        Token t = items.get(position);
        if (t.tokenInfo instanceof TicketInfo)
        {
            View v = holder.itemView;
            ImageView iv = v.findViewById(R.id.logo);
            if (iv != null) iv.setImageResource(R.mipmap.ic_alpha);
        }
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setTokens(Token[] tokens) {
        items.clear();
        items.addAll(Arrays.asList(tokens));
        notifyDataSetChanged();
    }
}
