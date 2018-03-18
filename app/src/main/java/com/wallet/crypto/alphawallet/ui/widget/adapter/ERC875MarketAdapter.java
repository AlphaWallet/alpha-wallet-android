package com.wallet.crypto.alphawallet.ui.widget.adapter;

import android.arch.lifecycle.Observer;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.OrderContractAddressPair;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.ui.widget.OnSalesOrderClickListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.SalesOrderSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.SortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TokenSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.entity.TotalBalanceSortedItem;
import com.wallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.OrderHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.TokenDescriptionHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 21/02/2018.
 */

public class ERC875MarketAdapter extends RecyclerView.Adapter<BinderViewHolder> {

    protected final OnSalesOrderClickListener onSalesOrderClickListener;
    private final List<SalesOrderSortedItem> currentList = new ArrayList<>();
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

    public ERC875MarketAdapter(OnSalesOrderClickListener onTokenClickListener, SalesOrder[] orders) {
        this.onSalesOrderClickListener = onTokenClickListener;
        setOrders(orders);
    }

    public ERC875MarketAdapter() {
        onSalesOrderClickListener = null;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case OrderHolder.VIEW_TYPE: {
                OrderHolder tokenHolder = new OrderHolder(R.layout.item_market_order, parent);
                tokenHolder.setOnOrderClickListener(onSalesOrderClickListener);
                holder = tokenHolder;
            } break;
            case TokenDescriptionHolder.VIEW_TYPE: {
                holder = new TokenDescriptionHolder(R.layout.item_token_description, parent);
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

    public void setOrders(SalesOrder[] orders) {
        currentList.clear();
        items.beginBatchedUpdates();
        items.clear();

        int i;

        for (i = 0; i < orders.length; i++)
        {
            SalesOrderSortedItem newItem = new SalesOrderSortedItem(orders[i], 10 + i);
            items.add(newItem);
            currentList.add(newItem);
        }
        items.endBatchedUpdates();
    }

    public void clear() {
        items.clear();
    }

    public void startUpdate()
    {
        items.beginBatchedUpdates();
        items.clear();
    }

    public void endUpdate()
    {
        items.endBatchedUpdates();
    }

    //we received the balance from the blockchain
    public void updateContent(OrderContractAddressPair balanceUpdate)
    {
        //we have sufficient information to update one of the tickets
        for (SalesOrderSortedItem thisItem : currentList)
        {
            SalesOrder order = thisItem.value;

            //updating this item?
            if (    order.contractAddress.equals(balanceUpdate.order.contractAddress) //order address matches
                &&  order.ownerAddress.equals(balanceUpdate.order.ownerAddress))
            {
                balanceUpdate.order.balanceInfo = balanceUpdate.balance;
                SalesOrderSortedItem newItem = new SalesOrderSortedItem(balanceUpdate.order, thisItem.weight);
                items.add(newItem);
            }
        }
    }
}
