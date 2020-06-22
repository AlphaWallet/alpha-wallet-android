package com.alphawallet.app.entity;

/**
 * Created by JB on 22/06/2020.
 */
public class UnableToResolveENS extends Exception
{
    public UnableToResolveENS(String message)
    {
        super(message);
    }
}