package com.alphawallet.app.ui.widget.entity;

public class NetworkItem {
    private String name;
    private long chainId;
    private boolean isSelected;

    public NetworkItem(String name, long chainId, boolean isSelected) {
        this.name = name;
        this.chainId = chainId;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public long getChainId() { return chainId; }

    public void setName(String name, long chainId) {
        this.name = name;
        this.chainId = chainId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
