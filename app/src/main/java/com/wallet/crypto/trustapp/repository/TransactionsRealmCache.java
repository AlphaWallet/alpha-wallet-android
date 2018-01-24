package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.BuildConfig;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.TransactionContract;
import com.wallet.crypto.trustapp.entity.TransactionOperation;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.entity.RealmTransaction;
import com.wallet.crypto.trustapp.repository.entity.RealmTransactionContract;
import com.wallet.crypto.trustapp.repository.entity.RealmTransactionOperation;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class TransactionsRealmCache implements TransactionLocalSource {

    private final Map<String, RealmConfiguration> realmConfigurations = new HashMap<>();

	@Override
	public Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm instance = null;
            try {
                instance = getRealmInstance(networkInfo, wallet);
                return convert(instance.where(RealmTransaction.class).findAll());
            } finally {
                if (instance != null) {
                    instance.close();
                }
            }
        });
	}

    @Override
	public void putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions) {
        Completable.fromAction(() -> {
            Realm instance = null;
            try {
                instance = getRealmInstance(networkInfo, wallet);
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
        .subscribeOn(Schedulers.io())
        .subscribe(() -> {}, t -> {});
	}

    @Override
    public Single<Transaction> findLast(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm realm = null;
            try {
                realm = getRealmInstance(networkInfo, wallet);
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
            realmContract.setTotalSupply(operation.contract.totalSupply);
            realmContract.setDecimals(operation.contract.decimals);
            realmContract.setSymbol(operation.contract.symbol);

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
            operation.contract = new TransactionContract();
            operation.contract.address = rawOperation.getContract().getAddress();
            operation.contract.name = rawOperation.getContract().getName();
            operation.contract.totalSupply = rawOperation.getContract().getTotalSupply();
            operation.contract.decimals = rawOperation.getContract().getDecimals();
            operation.contract.symbol = rawOperation.getContract().getSymbol();
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

    private Realm getRealmInstance(NetworkInfo networkInfo, Wallet wallet) {
        String name = wallet.address + "-" + networkInfo.name + "-transactions.realm";
        RealmConfiguration config = realmConfigurations.get(name);
        if (config == null) {
            config = new RealmConfiguration.Builder()
                    .name(name)
                    .schemaVersion(BuildConfig.DB_VERSION)
                    .deleteRealmIfMigrationNeeded()
                    .build();
            realmConfigurations.put(name, config);
        }
        return Realm.getInstance(config);
    }
}
