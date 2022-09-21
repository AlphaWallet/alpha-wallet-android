package com.alphawallet.app.ui.widget.entity;

public class ProgressInfo
{
    private boolean shouldShow;
    private int messageRes;

    public ProgressInfo(boolean shouldShow, int messageRes)
    {
        this.shouldShow = shouldShow;
        this.messageRes = messageRes;
    }

    public ProgressInfo(boolean shouldShow)
    {
        this.shouldShow = shouldShow;
    }

    public boolean shouldShow()
    {
        return shouldShow;
    }

    public int getMessage()
    {
        return messageRes;
    }
}
