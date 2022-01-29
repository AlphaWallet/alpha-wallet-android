package com.alphawallet.app.util;

import com.alphawallet.app.entity.tokens.Token;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class BalanceUtils
{
    private static final String weiInEth  = "1000000000000000000";
    private static final int showDecimalPlaces = 5;

    public static final String MACRO_PATTERN = "###,###,###,###,##0";
    public static final String CURRENCY_PATTERN = MACRO_PATTERN + ".00";
    private static final double ONE_BILLION = 1000000000.0;

    private static String getDigitalPattern(int precision)
    {
        return getDigitalPattern(precision, 0);
    }

    // Use this to format display strings however you like, eg for French, Spanish style
    // If you change this, it would also be advisable to change NumericInput so user has expected input format
    private static DecimalFormat getFormat(String pattern)
    {
        DecimalFormatSymbols standardisedNumericFormat = new DecimalFormatSymbols(Locale.ENGLISH);
        standardisedNumericFormat.setDecimalSeparator('.');
        standardisedNumericFormat.setGroupingSeparator(',');

        return new DecimalFormat(pattern, standardisedNumericFormat);
    }

    private static String getDigitalPattern(int precision, int fixed)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(MACRO_PATTERN);
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
        return scaledValue(value, getDigitalPattern(precision), 0, 0);
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
        return scaledValue(value, pattern, decimals, 0);
    }

    public static String getScaledValueFixed(BigDecimal value, long decimals, int precision)
    {
        String pattern = getDigitalPattern(precision, precision);
        return scaledValue(value, pattern, decimals, precision);
    }

    public static String getScaledValueMinimal(BigInteger value, long decimals)
    {
        return getScaledValueMinimal(new BigDecimal(value), decimals, Token.TOKEN_BALANCE_FOCUS_PRECISION); //scaledValue(new BigDecimal(value), getDigitalPattern(Token.TOKEN_BALANCE_FOCUS_PRECISION, 0), decimals);
    }

    public static String getScaledValueMinimal(BigDecimal value, long decimals, int max_precision)
    {
        return scaledValue(value, getDigitalPattern(max_precision, 0), decimals, 0);
    }

    public static String getScaledValueScientific(final BigDecimal value, long decimals)
    {
        return getScaledValueScientific(value, decimals, showDecimalPlaces);
    }

    //TODO: write 'suffix' generator: https://www.nist.gov/pml/weights-and-measures/metric-si-prefixes
    // Tera to pico (T to p) Anything below p show as 0.000
    public static String getScaledValueScientific(final BigDecimal value, long decimals, int dPlaces)
    {
        String returnValue;
        BigDecimal correctedValue = value.divide(BigDecimal.valueOf(Math.pow(10, decimals)), 18, RoundingMode.DOWN);

        final BigDecimal displayThreshold = BigDecimal.ONE.divide(BigDecimal.valueOf(Math.pow(10, dPlaces)), 18, RoundingMode.DOWN);
        if (value.equals(BigDecimal.ZERO)) //zero balance
        {
            returnValue = "0";
        }
        else if (correctedValue.compareTo(displayThreshold) < 0) //very low balance //TODO: Fold into getSuffixedValue below
        {
            returnValue = "0.000~";
        }
        else if (requiresSuffix(correctedValue, dPlaces))
        {
            returnValue = getSuffixedValue(correctedValue, dPlaces);
        }
        else //otherwise display in standard pattern to dPlaces dp
        {
            DecimalFormat df = getFormat(getDigitalPattern(dPlaces));
            //DecimalFormat df = new DecimalFormat(getDigitalPattern(dPlaces));
            df.setRoundingMode(RoundingMode.DOWN);
            returnValue = convertToLocale(df.format(correctedValue));
        }

        return returnValue;
    }

    private static boolean requiresSuffix(BigDecimal correctedValue, int dPlaces)
    {
        final BigDecimal displayThreshold = BigDecimal.ONE.divide(BigDecimal.valueOf(Math.pow(10, dPlaces)), 18, RoundingMode.DOWN);
        return correctedValue.compareTo(displayThreshold) < 0
                || correctedValue.compareTo(BigDecimal.valueOf(Math.pow(10, 6 + dPlaces))) > 0;
    }

    private static String getSuffixedValue(BigDecimal correctedValue, int dPlaces)
    {
        DecimalFormat df = getFormat(getDigitalPattern(0));
        df.setRoundingMode(RoundingMode.DOWN);
        int reductionValue = 0;
        String suffix = "";

        if (correctedValue.compareTo(BigDecimal.valueOf(Math.pow(10, 12 + dPlaces))) > 0) //T
        {
            reductionValue = 12;
            suffix = "T";
        }
        else if (correctedValue.compareTo(BigDecimal.valueOf(Math.pow(10, 9 + dPlaces))) > 0) //G
        {
            reductionValue = 9;
            suffix = "G";
        }
        else if (correctedValue.compareTo(BigDecimal.valueOf(Math.pow(10, 6 + dPlaces))) > 0) //M
        {
            reductionValue = 6;
            suffix = "M";
        }

        correctedValue = correctedValue.divideToIntegralValue(BigDecimal.valueOf(Math.pow(10, reductionValue)));
        return convertToLocale(df.format(correctedValue)) + suffix;
    }

    public static String getScaledValue(BigDecimal value, long decimals, int precision)
    {
        try
        {
            return scaledValue(value, getDigitalPattern(precision), decimals, precision);
        }
        catch (NumberFormatException e)
        {
            return "~";
        }
    }

    private static String scaledValue(BigDecimal value, String pattern, long decimals, int macroPrecision)
    {
        DecimalFormat df = getFormat(pattern);

        value = value.divide(BigDecimal.valueOf(Math.pow(10, decimals)), 18, RoundingMode.DOWN);
        if (macroPrecision > 0)
        {
            final BigDecimal displayThreshold = BigDecimal.ONE.multiply(BigDecimal.valueOf(Math.pow(10, macroPrecision)));
            if (value.compareTo(displayThreshold) > 0)
            {
                //strip decimals
                df = getFormat(MACRO_PATTERN);
            }
        }
        df.setRoundingMode(RoundingMode.DOWN);
        return convertToLocale(df.format(value));
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

    //Currency conversion
    public static String genCurrencyString(double price, String currencySymbol)
    {
        String suffix = "";
        String format = CURRENCY_PATTERN;
        if (price > ONE_BILLION)
        {
            format += "0";
            price /= ONE_BILLION;
            suffix = "B";
        }

        DecimalFormat df = getFormat(format);
        df.setRoundingMode(RoundingMode.CEILING);
        if (price >= 0) {
            return currencySymbol + df.format(price) + suffix;
        } else {
            return "-" + currencySymbol + df.format(Math.abs(price));
        }
    }
}
