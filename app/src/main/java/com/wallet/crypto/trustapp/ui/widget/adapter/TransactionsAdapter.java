package com.wallet.crypto.trustapp.ui.widget.adapter;

import android.os.Bundle;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.OnTransactionClickListener;
import com.wallet.crypto.trustapp.ui.widget.entity.ListItem;
import com.wallet.crypto.trustapp.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.TransactionHolder;

public class TransactionsAdapter extends RecyclerView.Adapter<BinderViewHolder> {

    private final SortedList<ListItem> items = new SortedList<>(ListItem.class, new SortedList.Callback<ListItem>() {
        @Override
        public int compare(ListItem left, ListItem right) {
            return 0;
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(ListItem oldItem, ListItem newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(ListItem item1, ListItem item2) {
            return false;
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
    private final OnTransactionClickListener onTransactionClickListener;

    private Wallet wallet;
    private NetworkInfo network;

    public TransactionsAdapter(OnTransactionClickListener onTransactionClickListener) {
        this.onTransactionClickListener = onTransactionClickListener;
    }

    @Override
    public BinderViewHolder<?> onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TransactionHolder.VIEW_TYPE: {
                TransactionHolder h
                        = new TransactionHolder(R.layout.item_transaction, parent);
                h.setOnTransactionClickListener(onTransactionClickListener);
                holder = h;
            }
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        Bundle addition = new Bundle();
        addition.putString(TransactionHolder.DEFAULT_ADDRESS_ADDITIONAL, wallet.address);
        addition.putString(TransactionHolder.DEFAULT_SYMBOL_ADDITIONAL, network.symbol);
        holder.bind(items.get(position).value, addition);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    public void setDefaultWallet(Wallet wallet) {
        this.wallet = wallet;
        notifyDataSetChanged();
    }

    public void setDefaultNetwork(NetworkInfo network) {
        this.network = network;
        notifyDataSetChanged();
    }

    public void addTransactions(Transaction[] transactions) {
        items.beginBatchedUpdates();
        items.addAll(ListItem.create(transactions, TransactionHolder.VIEW_TYPE));
        items.endBatchedUpdates();
    }
}
