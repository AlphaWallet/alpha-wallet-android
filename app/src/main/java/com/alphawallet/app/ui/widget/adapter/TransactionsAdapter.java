package com.alphawallet.app.ui.widget.adapter;

import android.os.Bundle;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.alphawallet.app.R;

import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTransactionClickListener;
import com.alphawallet.app.ui.widget.entity.DateSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TimestampSortedItem;
import com.alphawallet.app.ui.widget.entity.TransactionSortedItem;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.TransactionDateHolder;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;

import java.util.*;

public class TransactionsAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    private int layoutResId = -1;

    private final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem left, SortedItem right)
        {
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
    //private NetworkInfo network;
    private Map<String, TransactionSortedItem> checkMap = new HashMap<>();
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;

    public TransactionsAdapter(OnTransactionClickListener onTransactionClickListener, TokensService service,
                               FetchTransactionsInteract fetchTransactionsInteract) {
        this.onTransactionClickListener = onTransactionClickListener;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        tokensService = service;
        setHasStableIds(true);
    }

    public TransactionsAdapter(OnTransactionClickListener onTransactionClickListener, TokensService service,
                               FetchTransactionsInteract fetchTransactionsInteract, int layoutResId) {
        this.onTransactionClickListener = onTransactionClickListener;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        tokensService = service;
        setHasStableIds(true);
        this.layoutResId = layoutResId;
    }

    @Override
    public BinderViewHolder<?> onCreateViewHolder(ViewGroup parent, int viewType) {
        int resId;
        if (this.layoutResId != -1) {
            resId = R.layout.item_recent_transaction;
        } else {
            resId = R.layout.item_transaction;
        }
        BinderViewHolder holder = null;
        switch (viewType) {
            case TransactionHolder.VIEW_TYPE: {
                TransactionHolder transactionHolder
                        = new TransactionHolder(resId, parent, tokensService, fetchTransactionsInteract);
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
        //addition.putString(TransactionHolder.DEFAULT_SYMBOL_ADDITIONAL, network.symbol);
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

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int updateTransactions(Transaction[] transactions)
    {
        if (transactions.length == 0) return 0;
        int oldSize = items.size();

        items.beginBatchedUpdates();
        for (Transaction transaction : transactions)
        {
            TransactionMeta data = new TransactionMeta(transaction.hash, transaction.timeStamp);
            TransactionSortedItem sortedItem = new TransactionSortedItem(
                    TransactionHolder.VIEW_TYPE, data, TimestampSortedItem.DESC);
            items.add(sortedItem);
            items.add(DateSortedItem.round(transaction.timeStamp));
        }

        items.endBatchedUpdates();
        return items.size() - oldSize;
    }

    public void addNewTransactions(Transaction[] transactions)
    {
        if (transactions.length == 0) return;
        DateSortedItem lastDate = items.size() > 0 ? (DateSortedItem)items.get(0) : null;
        int itemsChanged = updateTransactions(transactions);
        if (itemsChanged > 0)
        {
            int startItem = 0;
            if (lastDate != null && lastDate.areItemsTheSame(items.get(0)))
            {
                startItem = 1;
            }
            notifyItemRangeChanged(startItem, items.size() - startItem);
        }
    }

    public void addTransactions(Transaction[] transactions)
    {
        items.beginBatchedUpdates();
        for (Transaction transaction : transactions)
        {
            TransactionMeta data = new TransactionMeta(transaction.hash, transaction.timeStamp);
            TransactionSortedItem sortedItem = new TransactionSortedItem(
                    TransactionHolder.VIEW_TYPE, data, TimestampSortedItem.DESC);
                items.add(sortedItem);
        }
        items.endBatchedUpdates();
        notifyDataSetChanged();
    }

    public int updateRecentTransactions(Transaction[] transactions)
    {
        boolean found;
        int itemsChanged = 0;
        //see if any update required
        for (Transaction txCheck : transactions)
        {
            found = false;
            for (int i = 0; i < items.size(); i++)
            {
                if (items.get(i).viewType == TransactionHolder.VIEW_TYPE
                    && txCheck.hash.equals(((TransactionSortedItem)items.get(i)).value.hash))
                { found = true; break; }
            }
            if (!found) itemsChanged++;
        }

        if (itemsChanged > 0)
        {
            items.clear();
            addTransactions(transactions);
        }

        return itemsChanged;
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }
}
