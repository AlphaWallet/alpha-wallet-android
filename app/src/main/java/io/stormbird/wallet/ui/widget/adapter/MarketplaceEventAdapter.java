package io.stormbird.wallet.ui.widget.adapter;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.MarketplaceEvent;
import io.stormbird.wallet.ui.widget.OnMarketplaceEventClickListener;
import io.stormbird.wallet.ui.widget.entity.MarketplaceEventSortedItem;
import io.stormbird.wallet.ui.widget.entity.SortedItem;
import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;
import io.stormbird.wallet.ui.widget.holder.MarketplaceEventHolder;

public class MarketplaceEventAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    private MarketplaceEvent marketplaceEvent;
    private final OnMarketplaceEventClickListener onMarketplaceEventClickListener;

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

    public MarketplaceEventAdapter() {
        onMarketplaceEventClickListener = null;
    }

    public MarketplaceEventAdapter(OnMarketplaceEventClickListener onMarketplaceEventClickListener) {
        this.onMarketplaceEventClickListener = onMarketplaceEventClickListener;
    }

    public void setMarketplaceEvents(MarketplaceEvent[] marketplaceEvents) {
        items.beginBatchedUpdates();
        items.clear();
        int i = 0;
        for (MarketplaceEvent marketplaceEvent : marketplaceEvents) {
            i++;
            MarketplaceEventSortedItem sortedItem = new MarketplaceEventSortedItem(
                    MarketplaceEventHolder.VIEW_TYPE, marketplaceEvent, i
            );
            items.add(sortedItem);
        }
        items.endBatchedUpdates();
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;

        MarketplaceEventHolder marketplaceEventHolder = new MarketplaceEventHolder(R.layout.item_marketplace_event, parent);

        marketplaceEventHolder.setOnMarketplaceEventClickListener(onMarketplaceEventClickListener);

        holder = marketplaceEventHolder;
//        switch (viewType) {
//            case MarketplaceEventHolder.VIEW_TYPE: {
//                holder = new MarketplaceEventHolder(R.layout.item_marketplace_event, parent);
//                Log.d("asd", "asdasd");
//            } break;
//            default:
//                Log.d("asd", "view type:  " +  viewType);
//                break;
//        }

        return holder;
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        holder.bind(items.get(position).value);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
