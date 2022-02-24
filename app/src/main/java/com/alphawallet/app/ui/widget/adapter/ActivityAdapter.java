package com.alphawallet.app.ui.widget.adapter;

/**
 * Created by JB on 7/07/2020.
 */

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.AdapterCallback;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.ActivityDataInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.DateSortedItem;
import com.alphawallet.app.ui.widget.entity.EventSortedItem;
import com.alphawallet.app.ui.widget.entity.LabelSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TimestampSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenTransferData;
import com.alphawallet.app.ui.widget.entity.TransactionSortedItem;
import com.alphawallet.app.ui.widget.entity.TransferSortedItem;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.EventHolder;
import com.alphawallet.app.ui.widget.holder.TransactionDateHolder;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;
import com.alphawallet.app.ui.widget.holder.TransferHolder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class ActivityAdapter extends RecyclerView.Adapter<BinderViewHolder<?>> implements AdapterCallback
{
    private final ActivitySortedList<SortedItem<?>> items = new ActivitySortedList<>(SortedItem.class, new ActivitySortedList.Callback<SortedItem<?>>() {
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

    private Wallet wallet;
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final ActivityDataInteract dataInteract;
    private final AssetDefinitionService assetService;
    private long fetchData = 0;
    private final Handler handler = new Handler();
    private int itemLimit = 0;
    private int lastItemPos = 0;
    private boolean pendingReset = false;

    public ActivityAdapter(TokensService service, FetchTransactionsInteract fetchTransactionsInteract,
                           AssetDefinitionService svs, ActivityDataInteract dataInteract) {
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.dataInteract = dataInteract;
        this.assetService = svs;
        tokensService = service;
    }

    public ActivityAdapter(TokensService service, FetchTransactionsInteract fetchTransactionsInteract, AssetDefinitionService svs)
    {
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        tokensService = service;
        this.dataInteract = null;
        this.assetService = svs;
    }

    @Override
    public BinderViewHolder<?> onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TransactionHolder.VIEW_TYPE:
                return new TransactionHolder(parent, tokensService, fetchTransactionsInteract,
                        assetService);
            case EventHolder.VIEW_TYPE:
                return new EventHolder(parent, tokensService, fetchTransactionsInteract,
                        assetService, this);
            case TransactionDateHolder.VIEW_TYPE:
                return new TransactionDateHolder(R.layout.item_transactions_date_head, parent);
            case LabelSortedItem.VIEW_TYPE:
                return new LabelHolder(R.layout.item_activity_label, parent);
            case TransferHolder.VIEW_TYPE:
                return new TransferHolder(parent, tokensService, fetchTransactionsInteract,
                        assetService);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, @SuppressLint("RecyclerView") int position)
    {
        Bundle addition = new Bundle();
        addition.putString(TransactionHolder.DEFAULT_ADDRESS_ADDITIONAL, wallet.address);
        holder.bind(items.get(position).value, addition);
        if (itemLimit > 0)
        {
            holder.setFromTokenView();
        }
        else if (position > lastItemPos && dataInteract != null && System.currentTimeMillis() > fetchData && position > items.size() - 100)
        {
            fetchData = System.currentTimeMillis() + 500;
            handler.post(checkData);
        }
        lastItemPos = position;
    }

    public void onRViewRecycled(RecyclerView.ViewHolder holder)
    {
        onViewRecycled((BinderViewHolder<?>)(holder));
    }

    @Override
    public void onViewRecycled(@NonNull BinderViewHolder holder)
    {
        super.onViewRecycled(holder);
        holder.onDestroyView();
    }

    private void fetchData(long earliestDate)
    {
        if (dataInteract != null) dataInteract.fetchMoreData(earliestDate);
    }

    private final Runnable checkData = () -> {
        //get final position time
        if (items.size() > 0)
        {
            SortedItem<?> item = items.get(items.size() - 1);
            long earliestDate = 0;
            if (item instanceof TimestampSortedItem)
            {
                Date itemDate = ((TimestampSortedItem) item).getTimestamp();
                earliestDate = itemDate.getTime();
            }

            fetchData(earliestDate);
        }
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

    @Override
    public long getItemId(int position)
    {
        Object obj = items.get(position);
        if (obj instanceof TransactionSortedItem)
        {
            TransactionMeta tm = ((TransactionSortedItem) obj).value;
            return tm.getUID();
        }
        else if (obj instanceof EventSortedItem)
        {
            EventMeta em = ((EventSortedItem) obj).value;
            return em.getUID();
        }
        else if (obj instanceof DateSortedItem)
        {
            return ((DateSortedItem)obj).getUID();
        }
        else if (obj instanceof TransferSortedItem)
        {
            return ((TransferSortedItem)obj).getUID();
        }
        else
        {
            //Not unique and may error
            Timber.d("Unable to determine unique item ID for this holder - you must define a specific UID method");
            return position;
        }
    }

    public void updateActivityItems(ActivityMeta[] activityItems)
    {
        if (activityItems.length == 0) return ;

        items.beginBatchedUpdates();
        if (itemLimit != 0)
        {
//            items.add(new LabelSortedItem(new Date(Long.MAX_VALUE))); //always at top of list
        }

        for (ActivityMeta item : activityItems)
        {
            if (item instanceof TransactionMeta)
            {
                TransactionSortedItem sortedItem = new TransactionSortedItem(TransactionHolder.VIEW_TYPE, (TransactionMeta)item, TimestampSortedItem.DESC);
                items.addTransaction(sortedItem); //event has higher UI priority than an event, don't overwrite
            }
            else if (item instanceof EventMeta)
            {
                EventSortedItem sortedItem = new EventSortedItem(EventHolder.VIEW_TYPE, (EventMeta)item, TimestampSortedItem.DESC);
                items.add(sortedItem);
            }
            else if (item instanceof TokenTransferData)
            {
                TransferSortedItem sortedItem = new TransferSortedItem(TransferHolder.VIEW_TYPE, (TokenTransferData)item, TimestampSortedItem.DESC);
                items.add(sortedItem);
            }
            items.add(DateSortedItem.round(item.getTimeStampSeconds()));
        }

        applyItemLimit();

        items.endBatchedUpdates();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    private void applyItemLimit()
    {
        if (itemLimit == 0) return;
        int count = 0;
        List<SortedItem> removalObjects = new ArrayList<>();

        //items should be sorted in date order already
        for (int p = 0; p < items.size(); p++)
        {
            if (items.get(p).value instanceof ActivityMeta)
            {
                count++;
            }

            if (count > itemLimit)
            {
                removalObjects.add(items.get(p));
            }
        }

        if (items.get(items.size()-1) instanceof DateSortedItem)
        {
            removalObjects.add(items.get(items.size()-1));
        }

        for (SortedItem sortedItem : removalObjects)
        {
            items.remove(sortedItem);
        }
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

    public void setItemLimit(int historyCount)
    {
        itemLimit = historyCount;
    }

    @Override
    public void resetRequired()
    {
        if (!pendingReset)
        {
            pendingReset = true;
            handler.postDelayed(resetAdapter, 1500);
        }
    }

    private final Runnable resetAdapter = () -> {
        pendingReset = false;
        notifyDataSetChanged();
    };

    public boolean isEmpty()
    {
        for (int i = 0; i < items.size(); i++)
        {
            Object item = items.get(i).value;
            if (item instanceof ActivityMeta)
            {
                return false;
            }
        }

        return true;
    }

    private static class LabelHolder extends BinderViewHolder<Date> {

        public LabelHolder(int resId, ViewGroup parent) {
            super(resId, parent);
        }

        @Override
        public void bind(@Nullable Date data, @NonNull Bundle addition) {

        }
    }

    private class ActivitySortedList<T> extends SortedList<T>
    {
        public ActivitySortedList(@NonNull Class klass, @NonNull Callback callback)
        {
            super(klass, callback);
        }

        public void addTransaction(T item)
        {
            if (item instanceof TransactionSortedItem)
            {
                TransactionSortedItem txSortedItem = (TransactionSortedItem)item;
                int index = items.indexOf(txSortedItem);
                if (index >= 0 && items.get(index).value instanceof EventMeta)
                {
                    EventMeta em = (EventMeta)items.get(index).value;
                    if (!em.hash.equals(txSortedItem.value.hash)) add(item); //don't replace matching Event
                }
                else
                {
                    add(item);
                }
            }
            else
            {
                Timber.tag("ActivityAdapter").e("Wrong item type in addTransaction (" + item.getClass().getName() + ")");
            }
        }
    }

    public void onDestroy(RecyclerView recyclerView)
    {
        //ensure all holders have their realm listeners cleaned up
        for (int childCount = recyclerView.getChildCount(), i = 0; i < childCount; ++i)
        {
            ((BinderViewHolder<?>)recyclerView.getChildViewHolder(recyclerView.getChildAt(i))).onDestroyView();
        }
    }
}

