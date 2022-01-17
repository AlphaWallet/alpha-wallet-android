package com.alphawallet.app.ui.widget.entity;

public class ManageTokensLabelSortedItem extends SortedItem<ManageTokensLabelData> {

    public ManageTokensLabelSortedItem(int type, ManageTokensLabelData data, int weight) {
        super(type, data, new TokenPosition(weight));
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}