package com.alphawallet.app.repository;

import android.util.Log;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.ERC875ContractTransaction;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionContract;
import com.alphawallet.app.entity.TransactionOperation;
import com.alphawallet.app.entity.TransactionType;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.repository.entity.RealmTransactionContract;
import com.alphawallet.app.repository.entity.RealmTransactionOperation;

import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import com.alphawallet.app.service.RealmManager;

import org.web3j.protocol.core.methods.response.EthTransaction;

import java.util.ArrayList;
import java.util.List;

import static com.alphawallet.app.entity.TransactionOperation.ERC875_CONTRACT_TYPE;
import static com.alphawallet.app.entity.TransactionOperation.NORMAL_CONTRACT_TYPE;
import static com.alphawallet.app.repository.TokensRealmSource.EVENT_CARDS;

public class TransactionsRealmCache implements TransactionLocalSource {

    private final RealmManager realmManager;
    private static final String TAG = "TRC";
    public static int realmCount = 0;

    public TransactionsRealmCache(RealmManager realmManager) {
        this.realmManager = realmManager;
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

    @Override
    public Transaction[] fetchPendingTransactions(String currentAddress)
    {
        Transaction[] pendingTransactions;
        try (Realm instance = realmManager.getRealmInstance(new Wallet(currentAddress)))
        {
            RealmResults<RealmTransaction> pendingTxs = instance.where(RealmTransaction.class)
                    .equalTo("blockNumber", "0")
                    .findAll();

            pendingTransactions = new Transaction[pendingTxs.size()];
            for (int i = 0; i < pendingTxs.size(); i++)
            {
                pendingTransactions[i] = convert(pendingTxs.get(i));
            }
        }
        catch (Exception e)
        {
            pendingTransactions = new Transaction[0];
        }

        return pendingTransactions;
    }

    @Override
    public Single<ActivityMeta[]> fetchActivityMetas(Wallet wallet, int chainId, String tokenAddress, int historyCount)
    {
        return Single.fromCallable(() -> {
            List<ActivityMeta> metas = new ArrayList<>();
            try (Realm instance = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmTransaction> txs = instance.where(RealmTransaction.class)
                        .sort("timeStamp", Sort.DESCENDING)
                        .equalTo("chainId", chainId)
                        .findAll();
                Log.d(TAG, "Found " + txs.size() + " TX Results");

                for (RealmTransaction item : txs)
                {
                    Transaction tx = convert(item);
                    if (tx.isRelated(tokenAddress, wallet.address))
                    {
                        TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber().equals("0"));
                        metas.add(tm);
                        if (metas.size() >= historyCount) break;
                    }
                }
            }
            catch (Exception e)
            {
                //
            }

            return metas.toArray(new ActivityMeta[0]);
        });
    }

    @Override
    public Single<ActivityMeta[]> fetchEventMetas(Wallet wallet, List<Integer> networkFilters)
    {
        return Single.fromCallable(() -> {
            List<ActivityMeta> metas = new ArrayList<>();
            try (Realm instance = realmManager.getRealmInstance(wallet.address))
            {
                RealmResults<RealmAuxData> evs = instance.where(RealmAuxData.class)
                        .endsWith("instanceKey", EVENT_CARDS)
                        .findAll();
                Log.d(TAG, "Found " + evs.size() + " TX Results");
                for (RealmAuxData item : evs)
                {
                    if (!networkFilters.contains(item.getChainId())) continue;
                    EventMeta newMeta = new EventMeta(item.getTransactionHash(), item.getEventName(), item.getFunctionId(), item.getResultTime(), item.getChainId());
                    metas.add(newMeta);
                }
            }
            catch (Exception e)
            {
                //
            }

            return metas.toArray(new ActivityMeta[0]);
        });
    }

    @Override
    public Single<ActivityMeta[]> fetchActivityMetas(Wallet wallet, List<Integer> networkFilters, long fetchTime, int fetchLimit)
    {
        return Single.fromCallable(() -> {
            List<ActivityMeta> metas = new ArrayList<>();
            try (Realm instance = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmTransaction> txs;
                if (fetchTime > 0)
                {
                    txs = instance.where(RealmTransaction.class)
                            .sort("timeStamp", Sort.DESCENDING)
                            .lessThan("timeStamp", fetchTime)
                            .limit(fetchLimit)
                            .findAll();
                }
                else
                {
                    txs = instance.where(RealmTransaction.class)
                            .sort("timeStamp", Sort.DESCENDING)
                            .limit(fetchLimit)
                            .findAll();
                }

                Log.d(TAG, "Found " + txs.size() + " TX Results");

                for (RealmTransaction item : txs)
                {
                    if (networkFilters.contains(item.getChainId()))
                    {
                        TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber().equals("0"));
                        metas.add(tm);
                    }
                }
            }
            catch (Exception e)
            {
                //
            }

            return metas.toArray(new ActivityMeta[0]);
        });
    }

    @Override
    public RealmAuxData fetchEvent(String walletAddress, String eventKey)
    {
        try (Realm instance = realmManager.getRealmInstance(walletAddress))
        {
            return instance.where(RealmAuxData.class)
                    .equalTo("instanceKey", eventKey)
                    .findFirst();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public void putTransaction(Wallet wallet, Transaction tx)
    {
        try (Realm instance = realmManager.getRealmInstance(wallet))
        {
            instance.executeTransaction(realm -> {
                RealmTransaction realmTx = instance.where(RealmTransaction.class)
                        .equalTo("hash", tx.hash)
                        .findFirst();

                if (realmTx != null)
                {
                    deleteOperations(realmTx);
                }
                else
                {
                    realmTx = instance.createObject(RealmTransaction.class, tx.hash);
                }

                fill(instance, realmTx, tx);
                realm.insertOrUpdate(realmTx);
            });
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

    @Override
    public Transaction storeRawTx(Wallet wallet, EthTransaction rawTx, long timeStamp)
    {
        if (rawTx.getResult() == null) return null;
        org.web3j.protocol.core.methods.response.Transaction ethTx = rawTx.getTransaction().get();
        final Transaction tx = new Transaction(ethTx.getHash(), "0", ethTx.getBlockNumber().toString(), timeStamp, ethTx.getNonce().intValue(), ethTx.getFrom(),
                ethTx.getTo(), ethTx.getValue().toString(), ethTx.getGas().toString(), ethTx.getGasPrice().toString(), ethTx.getInput(), ethTx.getGas().toString(), ethTx.getChainId().intValue(), "");
        tx.completeSetup(wallet.address);

        deleteTransaction(wallet, ethTx.getHash());
        try (Realm instance = realmManager.getRealmInstance(wallet))
        {
            instance.executeTransaction(realm -> {
                RealmTransaction item = instance.createObject(RealmTransaction.class, ethTx.getHash());
                fill(instance, item, tx);
                realm.insertOrUpdate(item);
            });
        }
        catch (Exception e)
        {
            //
        }

        return tx;
    }

    @Override
    public void deleteTransaction(Wallet wallet, String oldTxHash)
    {
        try (Realm instance = realmManager.getRealmInstance(wallet))
        {
            RealmTransaction realmTx = instance.where(RealmTransaction.class)
                    .equalTo("hash", oldTxHash)
                    .findFirst();

            if (realmTx != null)
            {
                instance.executeTransaction(realm -> {
                    deleteOperations(realmTx);
                    realmTx.deleteFromRealm();
                });
            }
        }
        catch (Exception e)
        {
            //do not record
            e.printStackTrace();
        }
    }

    public static void fill(Realm realm, RealmTransaction item, Transaction transaction) {
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

    private Transaction[] convertCount(RealmResults<RealmTransaction> items, int maxTransactions, List<Integer> networkFilters) {
        List<Transaction> retrievedTransactions = new ArrayList<>();
        int len = items.size() > maxTransactions ? maxTransactions : items.size();
        for (RealmTransaction item : items)
        {
            Transaction tx = convert(item);
            if (networkFilters.contains(tx.chainId))
            {
                retrievedTransactions.add(tx);
            }

            if (retrievedTransactions.size() >= len) break;
        }

        return retrievedTransactions.toArray(new Transaction[0]);
    }

    private void deleteOperations(RealmTransaction rawItem)
    {
        int len = rawItem.getOperations().size();
        for (int i = 0; i < len; i++)
        {
            RealmTransactionOperation rawOperation = rawItem.getOperations().get(i);
            if (rawOperation == null)
            {
                continue;
            }
            rawOperation.deleteFromRealm();
        }
    }

    public static Transaction convert(RealmTransaction rawItem) {
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

    @Override
    public void markTransactionDropped(String walletAddress, String hash)
    {
        try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
        {
            RealmTransaction realmTx = instance.where(RealmTransaction.class)
                    .equalTo("hash", hash)
                    .findFirst();

            if (realmTx != null)
            {
                instance.executeTransaction(realm -> {
                    realmTx.setBlockNumber("-1");
                    realmTx.setTimeStamp(System.currentTimeMillis()); //update timestamp so it's updated on the UI
                });
            }
        }
        catch (Exception e)
        {
            //
        }
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

    @Override
    public Realm getRealmInstance(Wallet wallet)
    {
        return realmManager.getRealmInstance(wallet);
    }
}
