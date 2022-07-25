package com.alphawallet.app.entity.lifi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

public class ConnectionTest
{
    @Test
    public void getFiatValue()
    {
        Connection.LToken lToken = new Connection.LToken();
        lToken.priceUSD = "6.72";
        lToken.balance = "1";

        assertThat(lToken.getFiatValue(), equalTo(6.72));
    }

    @Test
    public void getFiatValue_should_handle_exception()
    {
        Connection.LToken lToken = new Connection.LToken();
        lToken.priceUSD = "6.72";
        lToken.balance = "";
        assertThat(lToken.getFiatValue(), equalTo(0.0));

        lToken.priceUSD = "";
        lToken.balance = "1";
        assertThat(lToken.getFiatValue(), equalTo(0.0));

        lToken.priceUSD = null;
        lToken.balance = "1";
        assertThat(lToken.getFiatValue(), equalTo(0.0));

        lToken.priceUSD = "6.72";
        lToken.balance = null;
        assertThat(lToken.getFiatValue(), equalTo(0.0));
    }
}