package io.stormbird.wallet.entity;

import java.math.BigInteger;
import java.util.List;

import io.stormbird.token.entity.TSAction;

public interface StandardFunctionInterface
{
    default void selectRedeemTokens(List<BigInteger> selection) { };
    default void sellTicketRouter(List<BigInteger> selection) { };
    default void showTransferToken(List<BigInteger> selection) { };
    default void showSend() { };
    default void showReceive() { };
    default void displayTokenSelectionError(TSAction action) { };
    void handleTokenScriptFunction(String function, List<BigInteger> selection);
}
