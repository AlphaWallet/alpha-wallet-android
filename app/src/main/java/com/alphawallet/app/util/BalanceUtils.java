package com.alphawallet.app.util;

import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.tokens.Token;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class BalanceUtils
{
    private static String weiInEth  = "1000000000000000000";
    private static int showDecimalPlaces = 5;

    private static String getDigitalPattern(int precision)
    {
        return getDigitalPattern(precision, 0);
    }

    private static String getDigitalPattern(int precision, int fixed)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("###,###,###,###,##0");
        if (precision > 0)
        {
            sb.append(".");
            for (int i = 0; i < fixed; i++) sb.append("0");
            for (int i = 0; i < (precision-fixed); i++) sb.append("#");
        }
        return sb.toString();
    }

    private static String convertToLocale(String value)
    {
        return value;

        // TODO: Add localised values, need to do a global value rollout with override.
        /*char separator = DecimalFormatSymbols.getInstance().getGroupingSeparator();
        if (separator != ',')
        {
            char decimalPoint = DecimalFormatSymbols.getInstance().getDecimalSeparator();
            value = value.replace('.', '^');
            value = value.replace(',', separator);
            value = value.replace('^', decimalPoint);
        }

        return value;*/
    }

    public static BigDecimal weiToEth(BigDecimal wei) {
        return Convert.fromWei(wei, Convert.Unit.ETHER);
    }

    public static String ethToUsd(String priceUsd, String ethBalance) {
        BigDecimal usd = new BigDecimal(ethBalance).multiply(new BigDecimal(priceUsd));
        usd = usd.setScale(2, RoundingMode.DOWN);
        return usd.toString();
    }

    public static String EthToWei(String eth) {
        BigDecimal wei = new BigDecimal(eth).multiply(new BigDecimal(weiInEth));
        return wei.toBigInteger().toString();
    }

    public static String UnitToEMultiplier(String value, BigDecimal decimalPlaces) {
        BigDecimal val = new BigDecimal(value).multiply(decimalPlaces);
        return val.toBigInteger().toString();
    }

    public static BigDecimal weiToGweiBI(BigInteger wei) {
        return Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI);
    }

    public static String weiToGwei(BigInteger wei) {
        return Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI).toPlainString();
    }

    public static String weiToGweiInt(BigDecimal wei) {
        return getScaledValue(Convert.fromWei(wei, Convert.Unit.GWEI), 0, 0);
    }

    public static String weiToGwei(BigDecimal wei, int precision) {
        BigDecimal value = Convert.fromWei(wei, Convert.Unit.GWEI);
        return scaledValue(value, getDigitalPattern(precision), 0);
        //return getScaledValue(wei, Convert.Unit.GWEI.getWeiFactor().intValue(), precision);
        //return Convert.fromWei(new BigDecimal(wei), Convert.Unit.GWEI).setScale(decimals, RoundingMode.HALF_DOWN).toString(); //to 2 dp
    }

    public static BigInteger gweiToWei(BigDecimal gwei) {
        return Convert.toWei(gwei, Convert.Unit.GWEI).toBigInteger();
    }

    /**
     * Base - taken to mean default unit for a currency e.g. ETH, DOLLARS
     * Subunit - taken to mean subdivision of base e.g. WEI, CENTS
     *
     * @param baseAmountStr - decimal amonut in base unit of a given currency
     * @param decimals - decimal places used to convert to subunits
     * @return amount in subunits
     */
    public static BigInteger baseToSubunit(String baseAmountStr, int decimals) {
        assert(decimals >= 0);
        BigDecimal baseAmount = new BigDecimal(baseAmountStr);
        BigDecimal subunitAmount = baseAmount.multiply(BigDecimal.valueOf(10).pow(decimals));
        try {
            return subunitAmount.toBigIntegerExact();
        } catch (ArithmeticException ex) {
            assert(false);
            return subunitAmount.toBigInteger();
        }
    }

    /**
     * @param subunitAmount - amouunt in subunits
     * @param decimals - decimal places used to convert subunits to base
     * @return amount in base units
     */
    public static BigDecimal subunitToBase(BigInteger subunitAmount, int decimals)
    {
        assert(decimals >= 0);
        return new BigDecimal(subunitAmount).divide(BigDecimal.valueOf(10).pow(decimals));
    }

    public static boolean isDecimalValue(String value)
    {
        for (char ch : value.toCharArray()) if (!(Character.isDigit(ch) || ch == '.')) return false;
        return true;
    }

    public static String getScaledValueWithLimit(BigDecimal value, long decimals)
    {
        String pattern = getDigitalPattern(9, 2);
        return scaledValue(value, pattern, decimals);
    }

    public static String getScaledValueFixed(BigDecimal value, long decimals, int precision)
    {
        //form precision
        String pattern = getDigitalPattern(precision, precision);
        return scaledValue(value, pattern, decimals);
    }

    public static String getScaledValueMinimal(BigDecimal value, long decimals, int max_precision)
    {
        return scaledValue(value, getDigitalPattern(max_precision, 0), decimals);
    }

    public static String getScaledValueScientific(final BigDecimal value, long decimals)
    {
        return getScaledValueScientific(value, decimals, showDecimalPlaces);
    }

    public static String getScaledValueScientific(final BigDecimal value, long decimals, int dPlaces)
    {
        String returnValue;
        BigDecimal correctedValue = value.divide(BigDecimal.valueOf(Math.pow(10, decimals)), 18, RoundingMode.DOWN);
        final NumberFormat formatter = new DecimalFormat(CustomViewSettings.getDecimalFormat());
        formatter.setRoundingMode(RoundingMode.DOWN);
        if (value.equals(BigDecimal.ZERO)) //zero balance
        {
            returnValue = "0";
        }
        else if (correctedValue.compareTo(BigDecimal.valueOf(0.000001)) < 0) //very low balance
        {
            returnValue = formatter.format(correctedValue);
            returnValue = returnValue.replace("E", "e");
        }
        else if (correctedValue.compareTo(BigDecimal.valueOf(Math.pow(10, 14))) > 0) //too big
        {
            returnValue = formatter.format(correctedValue);
            returnValue = returnValue.replace("E", "e+");
        }
        else //otherwise display in standard pattern to dPlaces dp
        {
            DecimalFormat df = new DecimalFormat(getDigitalPattern(dPlaces));
            df.setRoundingMode(RoundingMode.DOWN);
            returnValue = convertToLocale(df.format(correctedValue));
        }

        return returnValue;
    }

    public static String getScaledValue(BigDecimal value, long decimals, int precision)
    {
        return scaledValue(value, getDigitalPattern(precision), decimals);
    }

    private static String scaledValue(BigDecimal value, String pattern, long decimals)
    {
        DecimalFormat df = new DecimalFormat(pattern);
        value = value.divide(BigDecimal.valueOf(Math.pow(10, decimals)), 18, RoundingMode.DOWN);
        df.setRoundingMode(RoundingMode.DOWN);
        return df.format(value);
    }

    /**
     * Default precision method
     *
     * @param valueStr
     * @param decimals
     * @return
     */
    public static String getScaledValue(String valueStr, long decimals)
    {
        return getScaledValue(valueStr, decimals, Token.TOKEN_BALANCE_PRECISION);
    }

    /**
     * Universal scaled value method
     * @param valueStr
     * @param decimals
     * @return
     */
    public static String getScaledValue(String valueStr, long decimals, int precision) {
        // Perform decimal conversion
        if (decimals > 1 && valueStr != null && valueStr.length() > 0 && Character.isDigit(valueStr.charAt(0)))
        {
            BigDecimal value = new BigDecimal(valueStr);
            return getScaledValue(value, decimals, precision); //represent balance transfers according to 'decimals' contract indicator property
        }
        else if (valueStr != null)
        {
            return valueStr;
        }
        else
        {
            return "0";
        }
    }
}
