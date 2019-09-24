package io.stormbird.wallet.entity;

import java.math.BigInteger;
import java.util.List;

import io.stormbird.token.entity.TSAction;

public interface StandardFunctionInterface
{
    void selectRedeemTokens(List<BigInteger> selection);
    void sellTicketRouter(List<BigInteger> selection);
    void showTransferToken(List<BigInteger> selection);
    void showSend();
    void showReceive();
    void displayTokenSelectionError(TSAction action);
    void handleTokenScriptFunction(String function, List<BigInteger> selection);
}
