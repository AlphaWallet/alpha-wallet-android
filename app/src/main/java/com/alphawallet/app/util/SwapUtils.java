package com.alphawallet.app.util;

import com.alphawallet.app.entity.lifi.Quote;

import java.math.BigDecimal;
import java.util.ArrayList;

public class SwapUtils
{
    private static final String GAS_PRICE_FORMAT = "%s %s";
    private static final String MINIMUM_RECEIVED_FORMAT = "%s %s";
    private static final String CURRENT_PRICE_FORMAT = "1 %s ≈ %s %s";

    public static String getTotalGasFees(ArrayList<Quote.Estimate.GasCost> gasCosts)
    {
        StringBuilder gas = new StringBuilder();
        for (Quote.Estimate.GasCost gc : gasCosts)
        {
            gas.append(SwapUtils.getGasFee(gc)).append(System.lineSeparator());
        }
        return gas.toString().trim();
    }

    public static String getGasFee(Quote.Estimate.GasCost gasCost)
    {
        return String.format(GAS_PRICE_FORMAT,
                BalanceUtils.getScaledValueFixed(new BigDecimal(gasCost.amount), gasCost.token.decimals, 4),
                gasCost.token.symbol);
    }

    public static String getFormattedCurrentPrice(Quote quote)
    {
        return String.format(CURRENT_PRICE_FORMAT,
                quote.action.fromToken.symbol,
                quote.getCurrentPrice(),
                quote.action.toToken.symbol);
    }

    public static String getMinimumAmountReceived(Quote quote)
    {
        return String.format(MINIMUM_RECEIVED_FORMAT,
                BalanceUtils.getShortFormat(quote.estimate.toAmountMin, quote.action.toToken.decimals),
                quote.action.toToken.symbol);
    }
}
