package com.alphawallet.app.viewmodel;

public class PasswordPhraseCounter
{
    private final int inputWordCount;

    public PasswordPhraseCounter(int inputWordCount)
    {
        this.inputWordCount = inputWordCount;
    }

    public String getText()
    {
        return inputWordCount + "/" + getMaxCount();
    }

    private int getMaxCount()
    {
        return inputWordCount > 12 ? 24 : 12;
    }

    public boolean match()
    {
        return inputWordCount == 12 || inputWordCount == 24;
    }

    public boolean notEnough()
    {
        return inputWordCount < getMaxCount();
    }

    public boolean exceed()
    {
        return inputWordCount > 24;
    }
}
