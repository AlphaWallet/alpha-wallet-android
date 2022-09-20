package com.alphawallet.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.alphawallet.app.entity.lifi.Action;
import com.alphawallet.app.entity.lifi.Estimate;
import com.alphawallet.app.entity.lifi.GasCost;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.entity.lifi.Token;

import org.junit.Test;

import java.util.ArrayList;

public class SwapUtilsTest
{
    @Test
    public void should_return_formatted_total_gas_fees()
    {
        ArrayList<GasCost> gasCostList = new ArrayList<>();
        GasCost gasCost1 = new GasCost();
        gasCost1.amount = "1000000000000000000";
        gasCost1.token = new Token();
        gasCost1.token.symbol = "ETH";
        gasCost1.token.decimals = 18;

        GasCost gasCost2 = new GasCost();
        gasCost2.amount = "2000000000000000000";
        gasCost2.token = new Token();
        gasCost2.token.symbol = "MATIC";
        gasCost2.token.decimals = 18;

        gasCostList.add(gasCost1);
        gasCostList.add(gasCost2);

        assertThat(SwapUtils.getTotalGasFees(gasCostList), equalTo("1.0000 ETH" + System.lineSeparator() + "2.0000 MATIC"));
    }

    @Test
    public void should_return_formatted_gas_fee()
    {
        GasCost gasCost = new GasCost();
        gasCost.amount = "1000000000000000000";
        gasCost.token = new Token();
        gasCost.token.symbol = "ETH";
        gasCost.token.decimals = 18;

        assertThat(SwapUtils.getGasFee(gasCost), equalTo("1.0000 ETH"));
    }

    @Test
    public void should_return_formatted_minimum_received()
    {
        Action action = new Action();
        action.toToken = new Token();
        action.toToken.decimals = 6;
        action.toToken.symbol = "ETH";

        Estimate estimate1 = new Estimate();
        estimate1.toAmountMin = "1000000";
        assertThat(SwapUtils.getFormattedMinAmount(estimate1, action), equalTo("1 ETH"));

        Estimate estimate2 = new Estimate();
        estimate2.toAmountMin = "1234567";
        assertThat(SwapUtils.getFormattedMinAmount(estimate2, action), equalTo("1.2345 ETH"));
    }

    @Test
    public void should_return_formatted_current_price()
    {
        Action action = new Action();
        action.fromToken = new Token();
        action.toToken = new Token();
        action.fromToken.priceUSD = "2000";
        action.fromToken.symbol = "ETH";
        action.fromToken.decimals = 18;
        action.toToken.priceUSD = "1";
        action.toToken.symbol = "USDC";
        action.toToken.decimals = 6;

        String expected = "1 ETH ≈ 2000 USDC";
        assertThat(SwapUtils.getFormattedCurrentPrice(action), equalTo(expected));
    }
}