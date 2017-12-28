package com.wallet.crypto.trustapp.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class BallanceUtils {
    private static String weiInEth  = "1000000000000000000";

    public static String weiToEth(BigInteger wei, int sigFig) throws Exception {
        BigDecimal eth = new BigDecimal(wei.toString()).divide(new BigDecimal(weiInEth));
        int scale = sigFig - eth.precision() + eth.scale();
        BigDecimal eth_scaled = eth.setScale(scale, RoundingMode.HALF_UP);
        return eth_scaled.toString();
    }

    public static String ethToUsd(String priceUsd, String ethBalance) {
        BigDecimal usd = new BigDecimal(ethBalance).multiply(new BigDecimal(priceUsd));
        usd = usd.setScale(2, RoundingMode.CEILING);
        return usd.toString();
    }
}
