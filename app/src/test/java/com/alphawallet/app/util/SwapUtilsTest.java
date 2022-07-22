package com.alphawallet.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.entity.lifi.Quote;

import org.junit.Test;

import java.util.ArrayList;

public class SwapUtilsTest
{
    @Test
    public void should_return_formatted_total_gas_fees()
    {
        ArrayList<Quote.Estimate.GasCost> gasCostList = new ArrayList<>();
        Quote.Estimate.GasCost gasCost1 = new Quote.Estimate.GasCost();
        gasCost1.amount = "1000000000000000000";
        gasCost1.token = new Quote.Estimate.GasCost.Token();
        gasCost1.token.symbol = "ETH";
        gasCost1.token.decimals = 18;

        Quote.Estimate.GasCost gasCost2 = new Quote.Estimate.GasCost();
        gasCost2.amount = "2000000000000000000";
        gasCost2.token = new Quote.Estimate.GasCost.Token();
        gasCost2.token.symbol = "MATIC";
        gasCost2.token.decimals = 18;

        gasCostList.add(gasCost1);
        gasCostList.add(gasCost2);

        assertThat(SwapUtils.getTotalGasFees(gasCostList), equalTo("1.0000 ETH" + System.lineSeparator() + "2.0000 MATIC"));
    }

    @Test
    public void should_return_formatted_gas_fee()
    {
        Quote.Estimate.GasCost gasCost = new Quote.Estimate.GasCost();
        gasCost.amount = "1000000000000000000";
        gasCost.token = new Quote.Estimate.GasCost.Token();
        gasCost.token.symbol = "ETH";
        gasCost.token.decimals = 18;

        assertThat(SwapUtils.getGasFee(gasCost), equalTo("1.0000 ETH"));
    }

    @Test
    public void should_return_current_price()
    {
        Quote quote = new Quote();
        quote.action = new Quote.Action();
        quote.action.fromToken = new Connection.LToken();
        quote.action.toToken = new Connection.LToken();
        quote.action.fromToken.priceUSD = "5";
        quote.action.toToken.priceUSD = "1000";

        assertThat(SwapUtils.getCurrentPrice(quote), equalTo("5000"));
    }

    @Test
    public void should_return_formatted_minimum_received()
    {
        Quote quote = new Quote();
        quote.action = new Quote.Action();
        quote.action.toToken = new Connection.LToken();
        quote.estimate = new Quote.Estimate();
        quote.estimate.toAmountMin = "1000000";
        quote.action.toToken.decimals = 6;
        quote.action.toToken.symbol = "ETH";

        assertThat(SwapUtils.getMinimumAmountReceived(quote), equalTo("1.000000 ETH"));
    }

    @Test
    public void should_return_formatted_current_price()
    {
        Quote quote = new Quote();
        quote.action = new Quote.Action();
        quote.action.fromToken = new Connection.LToken();
        quote.action.toToken = new Connection.LToken();
        quote.action.fromToken.priceUSD = "5";
        quote.action.fromToken.symbol = "ETH";
        quote.action.toToken.priceUSD = "1000";
        quote.action.toToken.symbol = "USDC";

        String expected = "1 ETH ≈ 5000 USDC";
        assertThat(SwapUtils.getFormattedCurrentPrice(quote), equalTo(expected));
    }
}