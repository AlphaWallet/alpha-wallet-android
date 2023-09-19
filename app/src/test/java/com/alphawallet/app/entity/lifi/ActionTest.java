package com.alphawallet.app.entity.lifi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

public class ActionTest
{
    @Test
    public void should_return_current_price()
    {
        Action action = new Action();
        action.fromToken = new Token();
        action.toToken = new Token();
        action.fromToken.priceUSD = "5";
        action.fromToken.decimals = 18;
        action.toToken.priceUSD = "1000";
        action.toToken.decimals = 18;

        assertThat(action.getCurrentPrice(), equalTo("0.005"));
    }
}