package com.langitwallet.app.entity;

import com.langitwallet.app.entity.tokens.Token;

public interface BuyCryptoInterface {
    void handleBuyFunction(Token token);
    void handleGeneratePaymentRequest(Token token);
}
