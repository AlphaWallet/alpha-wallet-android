package io.stormbird.wallet.web3;

import io.stormbird.wallet.web3.entity.Transaction;

public interface OnSignTransactionListener {
    void onSignTransaction(Transaction transaction);
}
