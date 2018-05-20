package io.stormbird.wallet.ui.widget.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.holder.ChangeTokenHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChangeTokenCollectionAdapter extends RecyclerView.Adapter<ChangeTokenHolder> {
    private final List<Token> items = new ArrayList<>();

    private final OnTokenClickListener onTokenClickListener;

    public ChangeTokenCollectionAdapter(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    @Override
    public ChangeTokenHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ChangeTokenHolder tokenHolder = new ChangeTokenHolder(R.layout.item_change_token, parent);
        tokenHolder.setOnTokenClickListener(onTokenClickListener);
        return tokenHolder;
    }

    @Override
    public void onBindViewHolder(ChangeTokenHolder holder, int position) {
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
