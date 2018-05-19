package io.stormbird.wallet.entity;

/**
 * Created by James on 4/03/2018.
 */

public class TokenTransaction {
    public Token token;
    public Transaction transaction;

    public TokenTransaction(Token t, Transaction tx)
    {
        token = t;
        transaction = tx;
    }
}
