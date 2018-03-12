package com.wallet.crypto.alphawallet.ui.widget.adapter;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenClickListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.SortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TokenSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TotalBalanceSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;

import java.math.BigDecimal;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    public static final int FILTER_ALL = 0;
    public static final int FILTER_CURRENCY = 1;
    public static final int FILTER_ASSETS = 2;

    private int filterType;
    private Context context;

    protected final OnTokenClickListener onTokenClickListener;
    protected final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem o1, SortedItem o2) {
            return o1.compare(o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(SortedItem oldItem, SortedItem newItem) {
            return oldItem.areContentsTheSame(newItem);
        }

        @Override
        public boolean areItemsTheSame(SortedItem item1, SortedItem item2) {
            return item1.areItemsTheSame(item2);
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    });
    protected TotalBalanceSortedItem total = new TotalBalanceSortedItem(null);

    public TokensAdapter(Context context, OnTokenClickListener onTokenClickListener) {
        this.context = context;
        this.onTokenClickListener = onTokenClickListener;
    }

    public TokensAdapter(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    public TokensAdapter() {
        onTokenClickListener = null;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TokenHolder.VIEW_TYPE: {
                TokenHolder tokenHolder = new TokenHolder(R.layout.item_token, parent);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenHolder;
                setAnimation(holder.itemView);
            }
            break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            }
        }
        return holder;
    }

    private void setAnimation(View viewToAnimate) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_from_bottom);
            viewToAnimate.startAnimation(animation);
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        holder.bind(items.get(position).value);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setTokens(Token[] tokens) {
        //TODO: check tokens for change
        items.beginBatchedUpdates();
        items.clear();
        items.add(total);

        for (int i = 0; i < tokens.length; i++) {
            if (filterType == FILTER_CURRENCY) {
                if (tokens[i].isCurrency()) {
                    items.add(new TokenSortedItem(tokens[i], 10 + i));
                }
            } else if (filterType == FILTER_ASSETS) {
                if (!tokens[i].isCurrency()) {
                    items.add(new TokenSortedItem(tokens[i], 10 + i));
                }
            } else {
                items.add(new TokenSortedItem(tokens[i], 10 + i));
            }
        }
        items.endBatchedUpdates();
    }

    public void setTotal(BigDecimal totalInCurrency) {
        total = new TotalBalanceSortedItem(totalInCurrency);
        items.add(total);
    }

    public void setFilterType(int filterType) {
        this.filterType = filterType;
    }

    public void clear() {
        items.clear();
    }
}
