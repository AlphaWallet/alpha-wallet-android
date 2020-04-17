package com.alphawallet.token.tools;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Ethereum unit conversion functions.
 */
public final class Convert {
    private Convert() { }

    public static BigDecimal fromWei(String number, Unit unit) {
        return fromWei(new BigDecimal(number), unit);
    }

    public static BigDecimal fromWei(BigDecimal number, Unit unit) {
        return number.divide(unit.getWeiFactor());
    }

    public static BigDecimal toWei(String number, Unit unit) {
        return toWei(new BigDecimal(number), unit);
    }

    public static BigDecimal toWei(BigDecimal number, Unit unit) {
        return number.multiply(unit.getWeiFactor());
    }

    public enum Unit {
        WEI("wei", 0),
        KWEI("kwei", 3),
        MWEI("mwei", 6),
        GWEI("gwei", 9),
        SZABO("szabo", 12),
        FINNEY("finney", 15),
        ETHER("ether", 18),
        KETHER("kether", 21),
        METHER("mether", 24),
        GETHER("gether", 27);

        private final String name;
        private final BigDecimal weiFactor;
        private final int factor;

        Unit(String name, int factor) {
            this.name = name;
            this.weiFactor = BigDecimal.TEN.pow(factor);
            this.factor = factor;
        }

        public BigDecimal getWeiFactor() {
            return weiFactor;
        }
        public int getFactor() { return factor; }

        @Override
        public String toString() {
            return name;
        }

        public static Unit fromString(String name) {
            if (name != null) {
                for (Unit unit : Unit.values()) {
                    if (name.equalsIgnoreCase(unit.name)) {
                        return unit;
                    }
                }
            }
            return Unit.valueOf(name);
        }
    }

    public static String getEthString(double ethPrice)
    {
        DecimalFormat df = new DecimalFormat("0.#####");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(ethPrice);
    }

    public static String getEthString(double ethFiatValue, int decimals)
    {
        DecimalFormat df = new DecimalFormat("0.#####");
        df.setRoundingMode(RoundingMode.CEILING);
        df.setMaximumFractionDigits(decimals);
        return df.format(ethFiatValue);
    }

    public static String getConvertedValue(BigDecimal rawValue, int divisor)
    {
        BigDecimal convertedValue = rawValue.divide(new BigDecimal(Math.pow(10, divisor)));
        DecimalFormat df = new DecimalFormat("0.#####");
        df.setRoundingMode(RoundingMode.HALF_DOWN);
        return df.format(convertedValue);
    }

    public static String getEthStringSzabo(BigInteger szabo)
    {
        BigDecimal ethPrice = fromWei(toWei(new BigDecimal(szabo), Unit.SZABO), Unit.ETHER);
        DecimalFormat df = new DecimalFormat("0.#####");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(ethPrice);
    }
}
