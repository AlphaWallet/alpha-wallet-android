package com.wallet.crypto.trustapp.ui.widget.entity;

import java.util.ArrayList;
import java.util.List;

public abstract class SortedItem<T> {
    protected final List<Integer> tags = new ArrayList<>();

    public final int viewType;
    public final T value;
    public final int weight;

    public SortedItem(int viewType, T value, int weight) {
        this.viewType = viewType;
        this.value = value;
        this.weight = weight;
    }

    public abstract int compare(SortedItem other);

    public abstract boolean areContentsTheSame(SortedItem newItem);

    public abstract boolean areItemsTheSame(SortedItem other);
}
