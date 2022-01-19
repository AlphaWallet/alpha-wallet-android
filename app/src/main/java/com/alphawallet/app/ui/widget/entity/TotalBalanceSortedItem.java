package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;

import java.math.BigDecimal;

public class TotalBalanceSortedItem extends SortedItem<BigDecimal> {

    public TotalBalanceSortedItem(BigDecimal value) {
        super(TotalBalanceHolder.VIEW_TYPE, value, 0);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return newItem.viewType == viewType
                || (((TotalBalanceSortedItem) newItem).value == null && value == null)
                || (((TotalBalanceSortedItem) newItem).value != null && value != null
                    && ((TotalBalanceSortedItem) newItem).value.compareTo(value) == 0);
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return other.viewType == viewType;
    }
}
