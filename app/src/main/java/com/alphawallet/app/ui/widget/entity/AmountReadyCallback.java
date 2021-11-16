package com.alphawallet.app.ui.widget.entity;

import java.math.BigDecimal;

/**
 * Created by JB on 13/11/2020.
 */
public interface AmountReadyCallback
{
    void amountReady(BigDecimal value, BigDecimal gasFee);
    default void updateCryptoAmount(BigDecimal value) { }
}
