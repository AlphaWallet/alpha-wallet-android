package com.langitwallet.app.ui.widget.entity;

import com.langitwallet.app.entity.analytics.ActionSheetMode;
import com.langitwallet.app.web3.entity.Web3Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by JB on 23/03/2022.
 *
 * This might be better done using a common class containing these functions which the 1559 and legacy widgets derive from
 * All common code can go into this class.
 */
public interface GasWidgetInterface
{
    boolean isSendingAll(Web3Transaction tx);
    BigInteger getValue();
    BigInteger getGasPrice(BigInteger defaultPrice);
    BigInteger getGasLimit();
    long getNonce();
    BigInteger getGasMax();
    BigInteger getPriorityFee();
    BigInteger getGasPrice();
    void setGasEstimate(BigInteger estimate);
    void setGasEstimateExact(BigInteger estimate);
    void onDestroy();
    boolean checkSufficientGas();
    void setupResendSettings(ActionSheetMode mode, BigInteger gasPrice);
    void setCurrentGasIndex(int gasSelectionIndex, BigInteger maxFeePerGas, BigInteger maxPriorityFee, BigDecimal customGasLimit, long expectedTxTime, long customNonce);
    long getExpectedTransactionTime();
    default boolean gasPriceReady(long gasEstimateTime)
    {
        return gasEstimateTime > (System.currentTimeMillis() - 30 * 1000);
    }

    boolean gasPriceReady();
}
