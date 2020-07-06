package com.alphawallet.app.ui.widget.adapter;

/**
 * Created by JB on 7/07/2020.
 */

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.Event;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.ActivityDataInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTransactionClickListener;
import com.alphawallet.app.ui.widget.entity.DateSortedItem;
import com.alphawallet.app.ui.widget.entity.EventSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TimestampSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.ui.widget.entity.TransactionSortedItem;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.TransactionDateHolder;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityAdapter extends RecyclerView.Adapter<BinderViewHolder> {
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
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final ActivityDataInteract dataInteract;
    private long fetchData = 0;
    private final Handler handler = new Handler();

    public ActivityAdapter(OnTransactionClickListener onTransactionClickListener, TokensService service,
                           FetchTransactionsInteract fetchTransactionsInteract, ActivityDataInteract dataInteract) {
        this.onTransactionClickListener = onTransactionClickListener;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.dataInteract = dataInteract;
        tokensService = service;
        setHasStableIds(true);
    }

    public ActivityAdapter(OnTransactionClickListener onTransactionClickListener, TokensService service,
                               FetchTransactionsInteract fetchTransactionsInteract, int layoutResId) {
        this.onTransactionClickListener = onTransactionClickListener;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        tokensService = service;
        setHasStableIds(true);
        this.layoutResId = layoutResId;
        this.dataInteract = null;
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
        if (dataInteract != null && System.currentTimeMillis() > fetchData && position > items.size() - 100)
        {
            fetchData = System.currentTimeMillis() + 10*DateUtils.SECOND_IN_MILLIS;
            handler.post(checkData);
        }
    }

    private void fetchData(long earliestDate)
    {
        if (dataInteract != null) dataInteract.fetchMoreData(earliestDate);
    }

    private Runnable checkData = () -> {
        //get final position time
        SortedItem item = items.get(items.size() - 1);
        long earliestDate = 0;
        if (item instanceof TransactionSortedItem)
        {
            earliestDate = ((TransactionSortedItem)item).value.timeStamp;
        }
        else if (item instanceof DateSortedItem)
        {
            earliestDate = ((DateSortedItem)item).value.getTime();
        }
        else if (item instanceof EventSortedItem)
        {
            earliestDate = ((EventSortedItem)item).value.timeStamp;
        }

        fetchData(earliestDate);
    };

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

//    @Override
//    public long getItemId(int position) {
//        return position;
//    }

    @Override
    public long getItemId(int position) {
        Object obj = items.get(position);
        if (obj instanceof TransactionSortedItem) {
            TransactionMeta tm = ((TransactionSortedItem)obj).value;
            return tm.getUID();
        } else {
            return position;
        }
    }

    public int updateActivityItems(ActivityMeta[] activityItems)
    {
        if (activityItems.length == 0) return 0;

        items.beginBatchedUpdates();
        for (ActivityMeta item : activityItems)
        {
            if (item instanceof TransactionMeta)
            {
                TransactionSortedItem sortedItem = new TransactionSortedItem(TransactionHolder.VIEW_TYPE, (TransactionMeta)item, TimestampSortedItem.DESC);
                items.add(sortedItem);
            }
            else if (item instanceof EventMeta)
            {
                //EventSortedItem sortedItem = new EventSortedItem(EventHolder.VIEW_TYPE, (EventMeta)item, TimestampSortedItem.DESC);
                //items.add(sortedItem);
            }
            items.add(DateSortedItem.round(item.timeStamp));
        }

        items.endBatchedUpdates();
        return activityItems.length;
    }

    public void addNewTransactions(ActivityMeta[] activityItems)
    {
        if (activityItems.length == 0) return;
        //DateSortedItem lastDate = items.size() > 0 ? (DateSortedItem)items.get(0) : null;
        int itemsChanged = updateActivityItems(activityItems);
        //update top 10 transactions or less
        if (itemsChanged > 0)
        {
            itemsChanged = items.size() < 10 ? items.size() : 10;
            notifyItemRangeChanged(0, itemsChanged);
        }
    }

    //TODO: Display events in Actions list
    public void addEvents(Event[] events)
    {

    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public void updateTransaction(RealmTransaction tx)
    {
        TransactionMeta txMeta = new TransactionMeta(tx.getHash(), tx.getTimeStamp(), tx.getTo(), tx.getChainId(), tx.getBlockNumber().equals("0"));
        TransactionSortedItem sortedItem = new TransactionSortedItem(TransactionHolder.VIEW_TYPE, txMeta, TimestampSortedItem.DESC);
        items.beginBatchedUpdates();
        items.add(sortedItem);
        items.add(DateSortedItem.round(txMeta.timeStamp));
        items.endBatchedUpdates();
    }

    public void updateItems(List<ContractLocator> tokenContracts)
    {
        //find items ssd
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).viewType == TransactionHolder.VIEW_TYPE
                && items.get(i).value instanceof TransactionMeta)
            {
                TransactionMeta tm = (TransactionMeta)items.get(i).value;
                if (tm.contractAddress != null && hasMatchingContract(tokenContracts, tm.contractAddress.toLowerCase()))
                {
                    notifyItemChanged(i);
                }
            }
        }
    }

    private TransactionMeta findMetaInAdapter(TransactionMeta meta)
    {
        //find items ssd
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).viewType == TransactionHolder.VIEW_TYPE
                    && items.get(i).value instanceof TransactionMeta)
            {
                TransactionMeta tm = (TransactionMeta)items.get(i).value;
                return tm;
            }
        }

        return null;
    }

    private boolean hasMatchingContract(List<ContractLocator> tokenContracts, String itemContractAddr)
    {
        for (ContractLocator cl : tokenContracts)
        {
            if (cl.address.equalsIgnoreCase(itemContractAddr))
            {
                return true;
            }
        }

        return false;
    }
}

