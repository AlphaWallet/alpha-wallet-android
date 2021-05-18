package com.alphawallet.app.ui.widget.entity;

public class TokenInfoHeader {
    private String name;
    private int chainId;
    private boolean isSelected;

    public TokenInfoHeader(String name, int chainId, boolean isSelected) {
        this.name = name;
        this.chainId = chainId;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public int getChainId() { return chainId; }

    public void setName(String name, int chainId) {
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
