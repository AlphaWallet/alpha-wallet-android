package io.stormbird.wallet.repository;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.stormbird.wallet.entity.ERC875ContractTransaction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionContract;
import io.stormbird.wallet.entity.TransactionOperation;
import io.stormbird.wallet.entity.TransactionType;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.entity.RealmTransaction;
import io.stormbird.wallet.repository.entity.RealmTransactionContract;
import io.stormbird.wallet.repository.entity.RealmTransactionOperation;
import io.stormbird.wallet.service.RealmManager;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;

import static io.stormbird.wallet.entity.TransactionOperation.ERC875_CONTRACT_TYPE;
import static io.stormbird.wallet.entity.TransactionOperation.NORMAL_CONTRACT_TYPE;

public class TransactionsRealmCache implements TransactionLocalSource {

    private final RealmManager realmManager;
    private static final String TAG = "TRC";
    public static int realmCount = 0;

    public TransactionsRealmCache(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

	@Override
	public Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm instance = null;
            try {
                instance = realmManager.getRealmInstance(networkInfo, wallet);
                RealmResults<RealmTransaction> txs = instance.where(RealmTransaction.class).findAll();
                Log.d(TAG, "Found " + txs.size() + " TX Results");
                return convert(txs);
            } finally {
                if (instance != null) {
                    instance.close();
                }
            }
        });
	}

    @Override
    public Transaction fetchTransaction(NetworkInfo networkInfo, Wallet wallet, String hash)
    {
        try (Realm instance = realmManager.getRealmInstance(networkInfo, wallet))
        {
            RealmTransaction realmTx = instance.where(RealmTransaction.class)
                    .equalTo("hash", hash)
                    .findFirst();

            if (realmTx != null)
            {
                return convert(realmTx);
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
	public Completable putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions) {
        return Completable.fromAction(() -> {
            Realm instance = null;
            try {
                instance = realmManager.getRealmInstance(networkInfo, wallet);
                addRealm();
                instance.beginTransaction();
                for (Transaction transaction : transactions) {
                    RealmTransaction item = instance.createObject(RealmTransaction.class, transaction.hash);
                    fill(instance, item, transaction);
                }
                instance.commitTransaction();
            } catch (Exception ex) {
                if (instance != null && instance.isInTransaction()) {
                    instance.cancelTransaction();
                }
            } finally {
                if (instance != null) {
                    subRealm();
                    instance.close();
                }
            }
        })
        .subscribeOn(Schedulers.io());
	}

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
                List<String> deleteList = new ArrayList<>();
                for (Transaction tx : transactions) txMap.put(tx.hash, tx);

                addRealm();
                instance.beginTransaction();
                RealmResults<RealmTransaction> rTx = instance.where(RealmTransaction.class).findAll();
                for (RealmTransaction realmTx : rTx) {
                    Transaction t = convert(realmTx);
                    Transaction replacement = txMap.get(t.hash);

                    if (t.hash.equals("0x127652a07b7c514b5ce853a2fba7f70f1244d211a7bd9e4ed94afa1863e67f3e"))
                    {
                        System.out.println("yoless");
                    }

                    //replace transaction but don't store invalid operations
                    if (replacement != null)
                    {
                        if (isBadTransaction(replacement))
                        {
                            deleteList.add(t.hash);
                        }
                        else
                        {
                            replace(instance, realmTx, replacement);
                        }
                        txMap.remove(t.hash);
                    }
                }
                instance.commitTransaction();

                if (deleteList.size() > 0)
                {
                    instance.beginTransaction();
                    for (String hash : deleteList)
                    {
                        RealmResults<RealmTransaction> result = instance.where(RealmTransaction.class).equalTo("hash", hash).findAll();
                        result.deleteAllFromRealm();
                    }
                    instance.commitTransaction();
                    deleteList.clear();
                }

                for (Transaction transaction : txMap.values()) {
                    //Log.d(TAG, "Attempting to store: " + transaction.hash);
                    //don't store any transaction that
                    if (transaction.hash.equals("0x127652a07b7c514b5ce853a2fba7f70f1244d211a7bd9e4ed94afa1863e67f3e"))
                    {
                        System.out.println("yoless");
                    }
                    if (isBadTransaction(transaction))
                    {
                        //Log.d(TAG, "No Store");
                        continue;
                    }
                    try
                    {
                        instance.beginTransaction();
                        RealmTransaction item = instance.createObject(RealmTransaction.class, transaction.hash);
                        if (transaction.hash.equals("0x127652a07b7c514b5ce853a2fba7f70f1244d211a7bd9e4ed94afa1863e67f3e"))
                        {
                            System.out.println("yoless");
                        }
                        fill(instance, item, transaction);
                        instance.commitTransaction();
                    }
                    catch (io.realm.exceptions.RealmPrimaryKeyConstraintException e)
                    {
                        //already exists
                        //Log.d(TAG, "Already exists: " + transaction.hash);
                        instance.cancelTransaction(); // it can only fail within a transaction, no need to check
                    }
                }
            } catch (Exception ex) {
                if (instance != null && instance.isInTransaction()) {
                    ex.printStackTrace();
                    instance.cancelTransaction();
                }
            } finally {
                if (instance != null) {
                    instance.close();
                    subRealm();
                }
            }
            return transactions;
        });
    }

    private boolean isBadTransaction(Transaction transaction)
    {
        if ((transaction.operations.length > 0 && transaction.operations[0].contract instanceof ERC875ContractTransaction))
        {
            return transaction.operations[0].contract.badTransaction;
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

    private void replace(Realm realm, RealmTransaction item, Transaction transaction) {
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
        item.setChainId(transaction.chainId);

        if (transaction.hash.equals("0x127652a07b7c514b5ce853a2fba7f70f1244d211a7bd9e4ed94afa1863e67f3e"))
        {
            System.out.println("yoless");
        }

        int len = item.getOperations().size();

        //Log.d(TAG, "Replace Tx " + transaction.hash + " : Sz: " + transaction.operations.length);
        for (int i = 0; i < transaction.operations.length; i++)
        {
            boolean addedOperation = false;
            boolean addedContract = false;

            TransactionOperation operation = transaction.operations[i];
            RealmTransactionOperation realmOperation;
            if (i < len)
            {
                realmOperation = item.getOperations().get(i);
            }
            else
            {
                addedOperation = true;
                realmOperation = realm.createObject(RealmTransactionOperation.class);
            }

            realmOperation.setTransactionId(operation.transactionId);
            realmOperation.setViewType(operation.viewType);
            realmOperation.setFrom(operation.from);
            realmOperation.setTo(operation.to);
            realmOperation.setValue(operation.value);

            RealmTransactionContract realmContract;
            if (realmOperation.getContract() != null)
            {
                realmContract = realmOperation.getContract();
            }
            else
            {
                addedContract = true;
                realmContract = realm.createObject(RealmTransactionContract.class);
            }

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
                    realmContract.setOperation(ct.operation.ordinal());
                    realmContract.setOtherParty(ct.otherParty);
                    break;
                case NORMAL_CONTRACT_TYPE:
                    realmContract.setTotalSupply(operation.contract.totalSupply);
                    realmContract.setDecimals(operation.contract.decimals);
                    realmContract.setContractType(NORMAL_CONTRACT_TYPE);
                    break;
            }

            if (addedContract)
            {
                realmOperation.setContract(realmContract);
            }
            if (addedOperation)
            {
                item.getOperations().add(realmOperation);
            }
        }
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
        item.setChainId(transaction.chainId);

        if (transaction.hash.equals("0x127652a07b7c514b5ce853a2fba7f70f1244d211a7bd9e4ed94afa1863e67f3e"))
        {
            System.out.println("yoless");
        }

        //Log.d(TAG, "Write Tx " + transaction.hash + " : Sz: " + transaction.operations.length);

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
                    realmContract.setOperation(ct.operation.ordinal());
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
	    System.gc();
	    Transaction[] result = new Transaction[len];
	    for (int i = 0; i < len; i++) {
	        result[i] = convert(items.get(i));
        }
        return result;
    }

    private Transaction convert(RealmTransaction rawItem) {
        int len = rawItem.getOperations().size();
        TransactionOperation[] operations = new TransactionOperation[len];
        //Log.d(TAG, "Read Tx " + rawItem.getHash() + " : Sz: " + len);
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
                    int operationId = rawOperation.getContract().getOperation();
                    if (operationId >= TransactionType.ILLEGAL_VALUE.ordinal()) operationId = TransactionType.ILLEGAL_VALUE.ordinal();
                    ((ERC875ContractTransaction)operation.contract).operation = TransactionType.values()[operationId];
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
                rawItem.getChainId(),
                operations
                );
    }



    /**
     * This pair of functions can be used for checking we don't have problems with
     * opening too many realm instances.
     */
    public static void addRealm()
    {
        realmCount++;
        //Log.d(TAG, "REALM COUNT: " + realmCount);
    }
    public static void subRealm()
    {
        realmCount--;
        //Log.d(TAG, "REALM COUNT: " + realmCount);
    }
}
