package io.stormbird.wallet.ui.widget.entity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;

public abstract class SortedItem<T> {
    protected final List<Integer> tags = new ArrayList<>();

    public final int viewType;
    public T value;
    public final int weight;
    public BinderViewHolder view;

    public SortedItem(int viewType, T value, int weight) {
        this.viewType = viewType;
        this.value = value;
        this.weight = weight;
    }

    public abstract int compare(SortedItem other);

    public abstract boolean areContentsTheSame(SortedItem newItem);

    public abstract boolean areItemsTheSame(SortedItem other);

    public boolean isRadioExposed()
    {
        return false;
    }

    public boolean isItemChecked()
    {
        return false;
    }

    public void setIsChecked(boolean b) { };

    public void setExposeRadio(boolean expose) { };

    public List<BigInteger> getTokenIds()
    {
        return new ArrayList<>();
    }
}
