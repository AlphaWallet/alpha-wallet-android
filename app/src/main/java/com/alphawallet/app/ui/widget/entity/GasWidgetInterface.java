package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.entity.analytics.ActionSheetMode;

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
    void onDestroy();
    boolean checkSufficientGas();
    void setupResendSettings(ActionSheetMode mode, BigInteger gasPrice);
    void setCurrentGasIndex(int gasSelectionIndex, BigInteger maxFeePerGas, BigInteger maxPriorityFee, BigDecimal customGasLimit, long expectedTxTime, long customNonce);
    long getExpectedTransactionTime();
}
