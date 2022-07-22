package com.alphawallet.app.entity.lifi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

public class QuoteTest
{
    @Test
    public void should_return_current_price()
    {
        Quote quote = new Quote();
        quote.action = new Quote.Action();
        quote.action.fromToken = new Connection.LToken();
        quote.action.toToken = new Connection.LToken();
        quote.action.fromToken.priceUSD = "5";
        quote.action.toToken.priceUSD = "1000";

        assertThat(quote.getCurrentPrice(), equalTo("5000"));
    }
}