package com.alphawallet.app.entity;

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
    private final float extraPriorityFeeRatio = 0.25f;      // extra priority fee offered in case of expected baseFee rise
    private final long fallbackPriorityFee = 2000000000L; // priority fee offered when there are no recent transactions
    private final FeeHistory feeHistory;

    public SuggestEIP1559Fees(FeeHistory feeHistory)
    {
        this.feeHistory = feeHistory;
        
    }
}
