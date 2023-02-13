package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.BinderViewHolder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class SortedItem<T> {
    protected final List<Integer> tags = new ArrayList<>();

    public int viewType;
    public final T value;
    public TokenPosition weight;
    public BinderViewHolder view;

    public SortedItem(int viewType, T value, TokenPosition weight) {
        this.viewType = viewType;
        this.value = value;
        this.weight = weight;
    }

    public int compare(SortedItem other)
    {
        if (value instanceof TokenSortedItem && other.value instanceof TokenSortedItem) //we may need to order tokens with the same name
        {
            return ((TokenSortedItem) value).compare(other);
        }
        else
        {
            return weight.compare(other.weight);
        }
    }

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

    public void setIsChecked(boolean b) { }

    public void setExposeRadio(boolean expose) { }

    public List<BigInteger> getTokenIds()
    {
        return new ArrayList<>();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortedItem<?> that = (SortedItem<?>) o;
        return viewType == that.viewType && tags.equals(that.tags) && value.equals(that.value) && weight.equals(that.weight) && view.equals(that.view);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tags, viewType, value, weight, view);
    }
}
