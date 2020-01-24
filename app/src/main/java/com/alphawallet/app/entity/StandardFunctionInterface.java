package com.alphawallet.app.entity;

import com.alphawallet.token.entity.TSAction;

import java.math.BigInteger;
import java.util.List;

public interface StandardFunctionInterface
{
    default void selectRedeemTokens(List<BigInteger> selection) { }

    default void sellTicketRouter(List<BigInteger> selection) { }

    default void showTransferToken(List<BigInteger> selection) { }

    default void showSend() { }

    default void showReceive() { }

    default void displayTokenSelectionError(TSAction action) { }

    default void handleClick(int view) { }

    default void handleTokenScriptFunction(String function, List<BigInteger> selection) { }
}
