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
    private final String address;
    private final BigDecimal weiAmount;
    private final long chainId;
    private String contractAddress;

    public EIP681Request(String displayAddress, long chainId, BigDecimal weiAmount)
    {
        this.address = displayAddress;
        this.chainId = chainId;
        this.weiAmount = weiAmount;
    }

    public EIP681Request(String userAddress, String contractAddress, long chainId, BigDecimal weiAmount)
    {
        this.address = userAddress;
        this.chainId = chainId;
        this.weiAmount = weiAmount;
        this.contractAddress = contractAddress;
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

    public String generateERC20Request()
    {
        //ethereum:0x744d70fdbe2ba4cf95131626614a1763df805b9e/transfer?address=0x3d597789ea16054a084ac84ce87f50df9198f415&uint256=314e17
        StringBuilder sb = new StringBuilder();
        sb.append(PROTOCOL);
        sb.append(contractAddress);
        sb.append("@");
        sb.append(chainId);
        sb.append("/transfer?address=");
        sb.append(address);
        sb.append("?uint256=");
        sb.append(format(weiAmount));

        return sb.toString();
    }
}
