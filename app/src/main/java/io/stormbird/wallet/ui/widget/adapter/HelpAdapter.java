package io.stormbird.wallet.ui.widget.adapter;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.HelpItem;
import io.stormbird.wallet.ui.widget.entity.HelpSortedItem;
import io.stormbird.wallet.ui.widget.entity.SortedItem;
import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;
import io.stormbird.wallet.ui.widget.holder.HelpHolder;

import java.util.List;

public class HelpAdapter extends RecyclerView.Adapter<BinderViewHolder> {

    public HelpAdapter() {
    }

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

    public void setHelpItems(List<HelpItem> helpItems) {
        items.beginBatchedUpdates();
        items.clear();
        int i = 0;
        for (HelpItem helpItem : helpItems) {
            i++;
            HelpSortedItem sortedItem = new HelpSortedItem(
                    HelpHolder.VIEW_TYPE, helpItem, i
            );
            items.add(sortedItem);
        }
        items.endBatchedUpdates();
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;

        holder = new HelpHolder(R.layout.item_help, parent);

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
