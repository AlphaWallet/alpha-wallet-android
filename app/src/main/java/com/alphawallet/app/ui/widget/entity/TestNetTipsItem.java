package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.TestNetTipsHolder;


public class TestNetTipsItem extends SortedItem<Void> {

    public TestNetTipsItem(int weight) {
        super(TestNetTipsHolder.VIEW_TYPE, null, new TokenPosition(weight));
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return true;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return true;
    }
}
