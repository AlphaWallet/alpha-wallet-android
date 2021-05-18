package com.alphawallet.app.ui.widget.entity;

public class PriceAlertItem {
    private String value;
    private boolean indicator;
    private boolean enabled;

    public PriceAlertItem(String value, boolean indicator, boolean enabled)
    {
        this.value = value;
        this.indicator = indicator;
        this.enabled = enabled;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean getIndicator()
    {
        return indicator;
    }

    public void setIndicator(boolean indicator)
    {
        this.indicator = indicator;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
