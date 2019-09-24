package com.alphawallet.app.repository;

import android.util.Log;

import com.alphawallet.app.entity.ERC875ContractTransaction;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionContract;
import com.alphawallet.app.entity.TransactionOperation;
import com.alphawallet.app.entity.TransactionType;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.repository.entity.RealmTransactionContract;
import com.alphawallet.app.repository.entity.RealmTransactionOperation;

import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import com.alphawallet.app.service.RealmManager;

import java.util.ArrayList;
import java.util.List;

import static com.alphawallet.app.entity.TransactionOperation.ERC875_CONTRACT_TYPE;
import static com.alphawallet.app.entity.TransactionOperation.NORMAL_CONTRACT_TYPE;

public class TransactionsRealmCache implements TransactionLocalSource {

    private final RealmManager realmManager;
    private static final String TAG = "TRC";
    public static int realmCount = 0;

    public TransactionsRealmCache(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

	@Override
	public Single<Transaction[]> fetchTransaction(Wallet wallet, int maxTransactions) {
        return Single.fromCallable(() -> {
            try (Realm instance = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmTransaction> txs = instance.where(RealmTransaction.class)
                        .sort("timeStamp", Sort.DESCENDING)
                        .findAll();
                Log.d(TAG, "Found " + txs.size() + " TX Results");
                return convertCount(txs, maxTransactions);
            }
            catch (Exception e)
            {
                return new Transaction[0];
            }
        });
	}

    @Override
    public Single<Transaction[]> fetchTransactions(Wallet wallet, Token token, int count)
    {
        return Single.fromCallable(() -> {
            try (Realm instance = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmTransaction> txs;
                txs = instance.where(RealmTransaction.class)
                        .equalTo("chainId", token.tokenInfo.chainId)
                        .sort("timeStamp", Sort.DESCENDING)
                        .findAll();

                List<Transaction> result = new ArrayList<>();

                for (RealmTransaction rtx : txs)
                {
                    Transaction tx = convert(rtx);
                    if (tx.isRelated(token.getAddress(), wallet.address))
                    {
                        result.add(tx);
                        if (result.size() >= 3) break;
                    }
                }
                return result.toArray(new Transaction[0]);
            }
            catch (Exception e)
            {
                return new Transaction[0];
            }
        });
    }

    @Override
    public Transaction fetchTransaction(Wallet wallet, String hash)
    {
        try (Realm instance = realmManager.getRealmInstance(wallet))
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

    /**
     * Single thread that also returns the transactions so we can use it in as an invisible member in an obserable stream
     * @param wallet
     * @param transactions
     * @return
     */
    @Override
    public Single<Transaction[]> putAndReturnTransactions(Wallet wallet, Transaction[] transactions) {
        return Single.fromCallable(() -> {
            try (Realm instance = realmManager.getRealmInstance(wallet))
            {
                addRealm();
                instance.beginTransaction();
                for (Transaction transaction : transactions)
                {
                    if (isBadTransaction(transaction)) continue;
                    RealmTransaction realmTx = instance.where(RealmTransaction.class)
                            .equalTo("hash", transaction.hash)
                            .findFirst();

                    if (realmTx != null && !realmTx.getBlockNumber().equals("0")) continue;
                    if (realmTx == null) realmTx = instance.createObject(RealmTransaction.class, transaction.hash);

                    try
                    {
                        fill(instance, realmTx, transaction);
                    }
                    catch (io.realm.exceptions.RealmPrimaryKeyConstraintException e)
                    {
                        e.printStackTrace(); //sometimes there's a tx clash, same tx may be recorded from different tokens
                    }
                }
                instance.commitTransaction();
                subRealm();
            }

            return transactions;
        });
    }

    @Override
    public void putTransaction(Wallet wallet, Transaction tx)
    {
        try (Realm instance = realmManager.getRealmInstance(wallet))
        {
            if (!alreadyRecorded(instance, tx.hash))
            {
                instance.beginTransaction();

                RealmTransaction item = instance.createObject(RealmTransaction.class, tx.hash);
                fill(instance, item, tx);

                instance.commitTransaction();
            }
        }
        catch (Exception e)
        {
            //do not record
            e.printStackTrace();
        }
    }

    private boolean alreadyRecorded(Realm instance, String hash)
    {
        RealmTransaction realmTx = instance.where(RealmTransaction.class)
                .equalTo("hash", hash)
                .findFirst();

        return realmTx != null && !realmTx.getBlockNumber().equals("0");
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
	    //System.gc();
	    Transaction[] result = new Transaction[len];
	    for (int i = 0; i < len; i++) {
	        result[i] = convert(items.get(i));
        }
        return result;
    }

    private Transaction[] convertCount(RealmResults<RealmTransaction> items, int maxTransactions) {
        int len = items.size() > maxTransactions ? maxTransactions : items.size();
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
