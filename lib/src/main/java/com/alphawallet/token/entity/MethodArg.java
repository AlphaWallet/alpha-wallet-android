package com.alphawallet.token.entity;

/**
 * Created by James on 2/05/2019.
 * Stormbird in Sydney
 */

// A param to pass into a smart contract function call
public class MethodArg
{
    public String parameterType; //type of param eg uint256, address etc
    public TokenscriptElement element; // contains either the value or reference to the value

    public boolean isTokenId()
    {
        return element.isToken();
    }

    public int getTokenIndex()
    {
        return element.getTokenIndex();
    }
}
