package com.alphawallet.app.ui.widget.entity;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PriceAlertTest
{

    @Test
    public void should_match_above_expect_price()
    {
        PriceAlert priceAlert = PriceAlertFactory.create();
        priceAlert.setAbove(true);
        priceAlert.setCurrency("AUD");
        priceAlert.setValue("2500");

        double currentTokenPriceInUSD = 1824D;
        double usdToAudRate = 1.37D;
        boolean matched = priceAlert.match(usdToAudRate, currentTokenPriceInUSD);
        assertFalse(matched);

        currentTokenPriceInUSD += 1;
        assertTrue(priceAlert.match(usdToAudRate, currentTokenPriceInUSD));
    }

    @Test
    public void should_match_below_expect_price()
    {
        PriceAlert priceAlert = PriceAlertFactory.create();
        priceAlert.setAbove(false);
        priceAlert.setCurrency("AUD");
        priceAlert.setValue("2500");

        double currentTokenPriceInUSD = 1825D;
        double usdToAudRate = 1.37D;
        boolean matched = priceAlert.match(usdToAudRate, currentTokenPriceInUSD);
        assertFalse(matched);

        currentTokenPriceInUSD -= 1;
        assertTrue(priceAlert.match(usdToAudRate, currentTokenPriceInUSD));
    }
}