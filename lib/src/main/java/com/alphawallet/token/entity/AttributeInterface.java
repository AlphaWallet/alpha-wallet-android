package com.alphawallet.token.entity;

import java.math.BigInteger;

/**
 * Created by James on 22/05/2019.
 * Stormbird in Sydney
 */
public interface AttributeInterface
{
    TransactionResult getFunctionResult(ContractAddress contract, AttributeType attr, BigInteger tokenId);
    TransactionResult storeAuxData(TransactionResult tResult);
    boolean resolveOptimisedAttr(ContractAddress contract, AttributeType attr, TransactionResult transactionResult);

    String getWalletAddr();
}
