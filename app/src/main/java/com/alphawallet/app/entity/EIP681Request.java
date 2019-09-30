package com.alphawallet.app.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by James on 25/02/2019.
 * Stormbird in Singapore
 */
public class EIP681Request
{
    private final String PROTOCOL = "ethereum:";
    private String address;
    private BigDecimal weiAmount;
    private int chainId;

    public EIP681Request(String displayAddress, int chainId, BigDecimal weiAmount)
    {
        this.address = displayAddress;
        this.chainId = chainId;
        this.weiAmount = weiAmount;
    }

    public String generateRequest()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(PROTOCOL);
        sb.append(address);
        sb.append("@");
        sb.append(chainId);
        sb.append("?value=");
        sb.append(format(weiAmount));

        return sb.toString();
    }

    private String format(BigDecimal x)
    {
        NumberFormat formatter = new DecimalFormat("0.#E0");
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        formatter.setMaximumFractionDigits(6);
        return formatter.format(x).replace(",", ".");
    }
}
