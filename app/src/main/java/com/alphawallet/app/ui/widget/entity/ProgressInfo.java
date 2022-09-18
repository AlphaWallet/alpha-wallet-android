package com.alphawallet.app.ui.widget.entity;

public class ProgressInfo
{
    boolean shouldShow;
    int messageRes;

    public ProgressInfo(boolean shouldShow, int messageRes)
    {
        this.shouldShow = shouldShow;
        this.messageRes = messageRes;
    }

    public boolean shouldShow()
    {
        return shouldShow;
    }

    public void setShouldShow(boolean shouldShow)
    {
        this.shouldShow = shouldShow;
    }

    public int getMessage()
    {
        return messageRes;
    }

    public void setMessage(int messageRes)
    {
        this.messageRes = messageRes;
    }
}
