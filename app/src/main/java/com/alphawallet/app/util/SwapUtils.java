package com.alphawallet.app.util;

import com.alphawallet.app.entity.lifi.Action;
import com.alphawallet.app.entity.lifi.Estimate;
import com.alphawallet.app.entity.lifi.FeeCost;
import com.alphawallet.app.entity.lifi.GasCost;
import com.alphawallet.app.entity.lifi.Quote;

import java.math.BigDecimal;
import java.util.ArrayList;

public class SwapUtils
{
    private static final String CURRENT_PRICE_FORMAT = "1 %s ≈ %s %s";
    private static final String GAS_PRICE_FORMAT = "%s %s";
    private static final String FEE_FORMAT = "%s %s";
    private static final String MINIMUM_RECEIVED_FORMAT = "%s %s";

    public static String getTotalGasFees(ArrayList<GasCost> gasCosts)
    {
        if (gasCosts != null)
        {
            StringBuilder gas = new StringBuilder();
            for (GasCost gc : gasCosts)
            {
                gas.append(SwapUtils.getGasFee(gc)).append(System.lineSeparator());
            }
            return gas.toString().trim();
        }
        else
        {
            return "";
        }
    }

    public static String getGasFee(GasCost gasCost)
    {
        return String.format(GAS_PRICE_FORMAT,
                BalanceUtils.getScaledValueFixed(new BigDecimal(gasCost.amount), gasCost.token.decimals, 4),
                gasCost.token.symbol);
    }

    public static String getOtherFees(ArrayList<FeeCost> feeCosts)
    {
        if (feeCosts != null)
        {
            StringBuilder fees = new StringBuilder();
            for (FeeCost fc : feeCosts)
            {
                fees.append(fc.name);
                fees.append(": ");
                fees.append(SwapUtils.getFee(fc)).append(System.lineSeparator());
            }
            return fees.toString().trim();
        }
        else
        {
            return "";
        }
    }

    public static String getFee(FeeCost feeCost)
    {
        return String.format(FEE_FORMAT,
                BalanceUtils.getScaledValueFixed(new BigDecimal(feeCost.amount), feeCost.token.decimals, 4),
                feeCost.token.symbol);
    }

    public static String getFormattedCurrentPrice(Action action)
    {
        return String.format(CURRENT_PRICE_FORMAT,
                action.fromToken.symbol,
                action.getCurrentPrice(),
                action.toToken.symbol);
    }

    public static String getFormattedMinAmount(Estimate estimate, Action action)
    {
        return String.format(MINIMUM_RECEIVED_FORMAT,
                BalanceUtils.getScaledValue(estimate.toAmountMin, action.toToken.decimals, 4),
                action.toToken.symbol);
    }
}
