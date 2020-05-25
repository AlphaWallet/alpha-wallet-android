package com.alphawallet.token.entity;

import java.math.BigInteger;

/**
 * Created by James on 22/05/2019.
 * Stormbird in Sydney
 */
public interface AttributeInterface
{
    TransactionResult getFunctionResult(ContractAddress contract, Attribute attr, BigInteger tokenId);
    TransactionResult storeAuxData(TransactionResult tResult);
    boolean resolveOptimisedAttr(ContractAddress contract, Attribute attr, TransactionResult transactionResult);

    String getWalletAddr();

    default long getLastTokenUpdate(int chainId, String address) { return 0; };
}
