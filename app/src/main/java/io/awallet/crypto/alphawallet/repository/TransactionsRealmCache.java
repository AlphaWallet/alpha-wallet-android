package io.awallet.crypto.alphawallet.repository;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.awallet.crypto.alphawallet.entity.ERC875ContractTransaction;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionContract;
import io.awallet.crypto.alphawallet.entity.TransactionOperation;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.entity.RealmTransaction;
import io.awallet.crypto.alphawallet.repository.entity.RealmTransactionContract;
import io.awallet.crypto.alphawallet.repository.entity.RealmTransactionOperation;
import io.awallet.crypto.alphawallet.service.RealmManager;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;

import static io.awallet.crypto.alphawallet.entity.TransactionOperation.ERC875_CONTRACT_TYPE;
import static io.awallet.crypto.alphawallet.entity.TransactionOperation.NORMAL_CONTRACT_TYPE;
import static io.awallet.crypto.alphawallet.interact.SetupTokensInteract.UNKNOWN_CONTRACT;

public class TransactionsRealmCache implements TransactionLocalSource {

    private final RealmManager realmManager;
    private static final String TAG = "TRC";

    public TransactionsRealmCache(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

	@Override
	public Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm instance = null;
            try {
                instance = realmManager.getRealmInstance(networkInfo, wallet);
                //Log.d("TRC", "TX retrieved " + instance.)
                return convert(instance.where(RealmTransaction.class).findAll());
            } finally {
                if (instance != null) {
                    instance.close();
                }
            }
        });
	}

    @Override
	public Completable putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions) {
        return Completable.fromAction(() -> {
            Realm instance = null;
            try {
                instance = realmManager.getRealmInstance(networkInfo, wallet);
                instance.beginTransaction();
                for (Transaction transaction : transactions) {
                    RealmTransaction item = instance.createObject(RealmTransaction.class, transaction.hash);
                    fill(instance, item, transaction);
                }
                instance.commitTransaction();
            } catch (Exception ex) {
                if (instance != null) {
                    instance.cancelTransaction();
                }
            } finally {
                if (instance != null) {
                    instance.close();
                }
            }
        })
        .subscribeOn(Schedulers.io());
	}


	/*
	updateNewCard(Realm realm, VisitingCardPOJO card) {
            VisitingCardPOJO toEdit = realm.where(VisitingCardPOJO.class)
                    .equalTo("no", card.getNo()).findFirst();
            realm.beginTransaction();
            toEdit.setName(card.getName());
            toEdit.setAddress(card.getAddress());
            realm.commitTransaction();
        }
	 */

    /**
     * Single thread that also returns the transactions so we can use it in as an invisible member in an obserable stream
     * @param networkInfo
     * @param wallet
     * @param transactions
     * @return
     */
    @Override
    public Single<Transaction[]> putAndReturnTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions) {
        return Single.fromCallable(() -> {
            Realm instance = null;
            try {
                instance = realmManager.getRealmInstance(networkInfo, wallet);
                //1. Update existing transactions
                Map<String, Transaction> txMap = new HashMap<>();
                for (Transaction tx : transactions) txMap.put(tx.hash, tx);

                instance.beginTransaction();
                RealmResults<RealmTransaction> rTx = instance.where(RealmTransaction.class).findAll();
                for (RealmTransaction realmTx : rTx) {
                    Transaction t = convert(realmTx);
                    Transaction replacement = txMap.get(t.hash);

                    //replace transaction
                    if (replacement != null)
                    {
                        Log.d(TAG, "Replacing: " + t.hash);
                        fill(instance, realmTx, replacement);
                        txMap.remove(t.hash);
                    }
                }
                instance.commitTransaction();

                for (Transaction transaction : txMap.values()) {
                    Log.d(TAG, "Attempting to store: " + transaction.hash);
                    //don't store any transaction that
                    if (isUnknownContract(transaction))
                    {
                        Log.d(TAG, "No Store");
                        continue;
                    }
                    try
                    {
                        instance.beginTransaction();
                        Log.d(TAG, "Storing: " + transaction.hash);
                        RealmTransaction item = instance.createObject(RealmTransaction.class, transaction.hash);
                        fill(instance, item, transaction);
                        instance.commitTransaction();
                    }
                    catch (io.realm.exceptions.RealmPrimaryKeyConstraintException e)
                    {
                        //already exists
                        Log.d(TAG, "Already exists: " + transaction.hash);
                        instance.cancelTransaction();
                    }
                }
            } catch (Exception ex) {
                if (instance != null) {
                    ex.printStackTrace();
                    instance.cancelTransaction();
                }
            } finally {
                if (instance != null) {
                    instance.close();
                }
            }
            return transactions;
        });
    }

    private boolean isUnknownContract(Transaction transaction)
    {
        if ((transaction.operations.length > 0 && transaction.operations[0].contract instanceof ERC875ContractTransaction)
            && (transaction.operations[0].contract.name.contains(UNKNOWN_CONTRACT)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public Single<Transaction> findLast(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm realm = null;
            try {
                realm = realmManager.getRealmInstance(networkInfo, wallet);
                return convert(realm.where(RealmTransaction.class).findFirst());
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        })
        .observeOn(Schedulers.io());
    }

    private void fill(Realm realm, RealmTransaction item, Transaction transaction) {
        item.setError(transaction.error);
        item.setBlockNumber(transaction.blockNumber);
        item.setTimeStamp(transaction.timeStamp);
        item.setNonce(transaction.nonce);
        item.setFrom(transaction.from);
        item.setTo(transaction.to);
        item.setValue(transaction.value);
        item.setGas(transaction.gas);
        item.setGasPrice(transaction.gasPrice);
        item.setInput(transaction.input);
        item.setGasUsed(transaction.gasUsed);

        for (TransactionOperation operation : transaction.operations) {
            RealmTransactionOperation realmOperation = realm.createObject(RealmTransactionOperation.class);
            realmOperation.setTransactionId(operation.transactionId);
            realmOperation.setViewType(operation.viewType);
            realmOperation.setFrom(operation.from);
            realmOperation.setTo(operation.to);
            realmOperation.setValue(operation.value);

            RealmTransactionContract realmContract = realm.createObject(RealmTransactionContract.class);
            realmContract.setAddress(operation.contract.address);
            realmContract.setName(operation.contract.name);
            realmContract.setSymbol(operation.contract.symbol);

            switch (operation.contract.contractType())
            {
                case ERC875_CONTRACT_TYPE:
                    ERC875ContractTransaction ct = (ERC875ContractTransaction) operation.contract;
                    realmContract.setBalance(ct.balance);
                    realmContract.setType(ct.type);
                    realmContract.setIndices(ct.getIndicesString());
                    realmContract.setContractType(ERC875_CONTRACT_TYPE);
                    realmContract.setOperation(ct.operation);
                    realmContract.setOtherParty(ct.otherParty);
                    break;
                case NORMAL_CONTRACT_TYPE:
                    realmContract.setTotalSupply(operation.contract.totalSupply);
                    realmContract.setDecimals(operation.contract.decimals);
                    realmContract.setContractType(NORMAL_CONTRACT_TYPE);
                    break;
            }

            realmOperation.setContract(realmContract);
            item.getOperations().add(realmOperation);
        }
    }

    private Transaction[] convert(RealmResults<RealmTransaction> items) {
	    int len = items.size();
	    Transaction[] result = new Transaction[len];
	    for (int i = 0; i < len; i++) {
	        result[i] = convert(items.get(i));
        }
        return result;
    }

    private Transaction convert(RealmTransaction rawItem) {
        int len = rawItem.getOperations().size();
        TransactionOperation[] operations = new TransactionOperation[len];
        for (int i = 0; i < len; i++) {
            RealmTransactionOperation rawOperation = rawItem.getOperations().get(i);
            if (rawOperation == null) {
                continue;
            }
            TransactionOperation operation = new TransactionOperation();
            operation.transactionId = rawOperation.getTransactionId();
            operation.viewType = rawOperation.getViewType();
            operation.from = rawOperation.getFrom();
            operation.to = rawOperation.getTo();
            operation.value = rawOperation.getValue();

            int type = rawOperation.getContract().getContractType();

            switch (type)
            {
                case ERC875_CONTRACT_TYPE:
                    operation.contract = new ERC875ContractTransaction();
                    ((ERC875ContractTransaction)operation.contract).balance = rawOperation.getContract().getBalance();
                    ((ERC875ContractTransaction)operation.contract).setIndicesFromString(rawOperation.getContract().getIndices());
                    ((ERC875ContractTransaction)operation.contract).operation = rawOperation.getContract().getOperation();
                    ((ERC875ContractTransaction)operation.contract).otherParty = rawOperation.getContract().getOtherParty();
                    ((ERC875ContractTransaction)operation.contract).type = rawOperation.getContract().getType();
                    break;
                case NORMAL_CONTRACT_TYPE:
                    operation.contract = new TransactionContract();
                    operation.contract.totalSupply = rawOperation.getContract().getTotalSupply();
                    operation.contract.decimals = rawOperation.getContract().getDecimals();
                    operation.contract.symbol = rawOperation.getContract().getSymbol();
                    break;
            }


            operation.contract.address = rawOperation.getContract().getAddress();
            operation.contract.name = rawOperation.getContract().getName();

            operations[i] = operation;
        }
	    return new Transaction(
	            rawItem.getHash(),
                rawItem.getError(),
                rawItem.getBlockNumber(),
                rawItem.getTimeStamp(),
                rawItem.getNonce(),
                rawItem.getFrom(),
                rawItem.getTo(),
                rawItem.getValue(),
                rawItem.getGas(),
                rawItem.getGasPrice(),
                rawItem.getInput(),
                rawItem.getGasUsed(),
                operations
                );
    }
}
