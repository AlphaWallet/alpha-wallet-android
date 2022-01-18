package com.alphawallet.app.entity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by JB on 4/01/2022.
 *
 * Based on equivalent in KEthereum: https://github.com/komputing/KEthereum,
 *  in turn based on https://github.com/zsfelfoldi/feehistory
 */
public class SuggestEIP1559Fees
{
    private final int sampleMinPercentile = 10;          // sampled percentile range of exponentially weighted baseFee history
    private final int sampleMaxPercentile = 30;

    private final int rewardPercentile = 10;             // effective reward value to be selected from each individual block
    private final int rewardBlockPercentile = 40;        // suggested priority fee to be selected from sorted individual block reward percentiles

    private final int maxTimeFactor = 15;                // highest timeFactor index in the returned list of suggestion
    private final float extraPriorityFeeRatio = 0.25f;    // extra priority fee offered in case of expected baseFee rise
    private final long fallbackPriorityFee = 2000000000L; // priority fee offered when there are no recent transactions
    private final FeeHistory feeHistory;

    public SuggestEIP1559Fees(FeeHistory feeHistory)
    {
        this.feeHistory = feeHistory;

        List<BigInteger> baseFee = new ArrayList<>();

        for (String fee : feeHistory.baseFeePerGas)
        {
            baseFee.add(new BigInteger(fee));
        }

        //baseFee[baseFee.size - 1] = (baseFee[baseFee.size - 1].toBigDecimal() * BigDecimal(9 / 8.0)).toBigInteger()

        //sort
        //List<BigInteger> order = new ArrayList<>(baseFeeList);
        //Collections.sort(order);





        
    }
}
