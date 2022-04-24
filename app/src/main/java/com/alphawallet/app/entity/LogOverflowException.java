package com.alphawallet.app.entity;

import org.web3j.protocol.core.Response;

/**
 * Created by JB on 23/04/2022.
 */
public class LogOverflowException extends Exception
{
    public final Response.Error error;

    public LogOverflowException(Response.Error err)
    {
        super(err.getMessage());
        error = err;
    }
}