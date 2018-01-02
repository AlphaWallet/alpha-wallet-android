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
import com.wallet.crypto.trustapp.ui.widget.entity.DateSortedItem;
import com.wallet.crypto.trustapp.ui.widget.entity.SortedItem;
import com.wallet.crypto.trustapp.ui.widget.entity.TimestampSortedItem;
import com.wallet.crypto.trustapp.ui.widget.entity.TransactionSortedItem;
import com.wallet.crypto.trustapp.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.TransactionDateHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.TransactionHolder;

public class TransactionsAdapter extends RecyclerView.Adapter<BinderViewHolder> {

    private final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem left, SortedItem right) {
            return left.compare(right);
        }

        @Override
        public boolean areContentsTheSame(SortedItem oldItem, SortedItem newItem) {
            return oldItem.areContentsTheSame(newItem);
        }

        @Override
        public boolean areItemsTheSame(SortedItem left, SortedItem right) {
            return left.areItemsTheSame(right);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
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
                TransactionHolder transactionHolder
                        = new TransactionHolder(R.layout.item_transaction, parent);
                transactionHolder.setOnTransactionClickListener(onTransactionClickListener);
                holder = transactionHolder;
            } break;
            case TransactionDateHolder.VIEW_TYPE: {
                holder = new TransactionDateHolder(R.layout.item_transactions_date_head, parent);
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
        for (Transaction transaction : transactions) {
            TransactionSortedItem sortedItem = new TransactionSortedItem(
                    TransactionHolder.VIEW_TYPE, transaction, TimestampSortedItem.DESC);
            items.add(sortedItem);
            items.add(DateSortedItem.round(transaction.timeStamp));
        }
        items.endBatchedUpdates();
    }

    public void clear() {
        items.clear();
    }
}
