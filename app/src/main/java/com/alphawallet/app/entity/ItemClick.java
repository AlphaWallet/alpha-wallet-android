package com.alphawallet.app.entity;

/**
 * Created by JB on 9/04/2020.
 */
public class ItemClick
{
    public final String buttonText;
    public final int buttonId;

    public ItemClick(String text, int id)
    {
        buttonId = id;
        buttonText = text;
    }
}
