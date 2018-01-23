package com.wallet.crypto.trustapp.ui.widget.adapter;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.ui.widget.OnTokenClickListener;
import com.wallet.crypto.trustapp.ui.widget.entity.SortedItem;
import com.wallet.crypto.trustapp.ui.widget.entity.TokenSortedItem;
import com.wallet.crypto.trustapp.ui.widget.entity.TotalBalanceSortedItem;
import com.wallet.crypto.trustapp.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.TokenHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.TotalBalanceHolder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder> {

     private final OnTokenClickListener onTokenClickListener;
     private final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
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

    public TokensAdapter(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TokenHolder.VIEW_TYPE: {
                TokenHolder tokenHolder = new TokenHolder(R.layout.item_token, parent);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenHolder;
            } break;
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            }
        }

        return holder;
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
        items.beginBatchedUpdates();
        items.clear();
        for (int i = 0; i < tokens.length; i++) {
            items.add(new TokenSortedItem(tokens[i], 10 + i));
        }
        items.endBatchedUpdates();
    }

    public void setTotal(BigDecimal totalInCurrency) {
        items.add(new TotalBalanceSortedItem(totalInCurrency));
    }

    public void clear() {
        items.clear();
    }
}
