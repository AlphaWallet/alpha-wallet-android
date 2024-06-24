package com.langitwallet.app.repository;

import com.langitwallet.app.entity.OnRampContract;
import com.langitwallet.app.entity.tokens.Token;

public interface OnRampRepositoryType {
    String getUri(String address, Token token);

    OnRampContract getContract(Token token);
}
