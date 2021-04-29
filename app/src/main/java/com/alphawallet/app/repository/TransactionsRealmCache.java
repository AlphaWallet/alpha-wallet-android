package com.alphawallet.app.repository;

import android.util.Log;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.RealmManager;

import org.jetbrains.annotations.NotNull;
import org.web3j.protocol.core.methods.response.EthTransaction;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.alphawallet.app.repository.TokensRealmSource.EVENT_CARDS;

public class TransactionsRealmCache implements TransactionLocalSource {

    private final RealmManager realmManager;
    private static final String TAG = "TRC";

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
    public long fetchTxCompletionTime(Wallet wallet, String hash)
    {
        try (Realm instance = realmManager.getRealmInstance(wallet))
        {
            RealmTransaction realmTx = instance.where(RealmTransaction.class)
                    .equalTo("hash", hash)
                    .findFirst();

            if (realmTx != null)
            {
                return realmTx.getExpectedCompletion();
            }
        }
        catch (Exception e)
        {
            //
        }

        return System.currentTimeMillis() + 60*1000;
    }

    @Override
    public Transaction[] fetchPendingTransactions(String currentAddress)
    {
        Transaction[] pendingTransactions;
        try (Realm instance = realmManager.getRealmInstance(new Wallet(currentAddress)))
        {
            RealmResults<RealmTransaction> pendingTxs = instance.where(RealmTransaction.class)
                    .equalTo("blockNumber", "-2")
                    .or().equalTo("blockNumber", "0")
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
                        TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber());
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
                final RealmResults<RealmTransaction> txs = generateRealmQuery(instance, fetchTime, fetchLimit).findAll();
                Log.d(TAG, "Found " + txs.size() + " TX Results");
                fixBadTXValues(instance, fetchTime, fetchLimit);

                for (RealmTransaction item : txs)
                {
                    if (networkFilters.contains(item.getChainId()))
                    {
                        TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber());
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

    private RealmQuery<RealmTransaction> generateRealmQuery(Realm instance, long fetchTime, int fetchLimit)
    {
        if (fetchTime > 0)
        {
            return instance.where(RealmTransaction.class)
                    .sort("timeStamp", Sort.DESCENDING)
                    .lessThan("timeStamp", fetchTime)
                    .limit(fetchLimit);
        }
        else
        {
            return instance.where(RealmTransaction.class)
                    .sort("timeStamp", Sort.DESCENDING)
                    .limit(fetchLimit);
        }
    }

    //Correct any bad value that was previously recorded in a way that wrote the wrong time value
    //TODO: Remove this after a couple more releases, but it's harmless
    private void fixBadTXValues(@NotNull Realm instance, final long fetchTime, final int fetchLimit)
    {
        long currentTime = System.currentTimeMillis() / 1000L;
        instance.executeTransactionAsync(r -> {
            final RealmResults<RealmTransaction> txs = generateRealmQuery(r, fetchTime, fetchLimit).findAll();
            for (RealmTransaction item : txs)
            {
                if ((currentTime - item.getTimeStamp()) < -3000 * 24 * 60 * 60 && (item.getBlockNumber().equals("-1") || item.getBlockNumber().equals("0")))
                {
                    // potentially incorrectly recorded tx, change to pending to re-scan
                    item.setBlockNumber("0");
                    item.setError("0");
                    item.setTimeStamp(currentTime);
                }
            }
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
    public void putTransaction(Wallet wallet, final Transaction tx)
    {
        try (Realm instance = realmManager.getRealmInstance(wallet))
        {
            instance.executeTransactionAsync(r -> {
                RealmTransaction realmTx = r.where(RealmTransaction.class)
                        .equalTo("hash", tx.hash)
                        .findFirst();

                if (realmTx == null)
                {
                    realmTx = r.createObject(RealmTransaction.class, tx.hash);
                }

                fill(r, realmTx, tx);
                r.insertOrUpdate(realmTx);
            });
        }
        catch (Exception e)
        {
            //do not record
            e.printStackTrace();
        }
    }

    @Override
    public Transaction storeRawTx(Wallet wallet, int chainId, EthTransaction rawTx, long timeStamp, boolean isSuccessful)
    {
        if (rawTx.getResult() == null) return null;
        org.web3j.protocol.core.methods.response.Transaction ethTx = rawTx.getTransaction().get();

        final Transaction tx = new Transaction(ethTx, chainId, isSuccessful, timeStamp);

        deleteTransaction(wallet, ethTx.getHash());
        try (Realm instance = realmManager.getRealmInstance(wallet))
        {
            instance.executeTransactionAsync(r -> {
                RealmTransaction item = r.createObject(RealmTransaction.class, ethTx.getHash());
                fill(r, item, tx);
                r.insertOrUpdate(item);
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
            instance.executeTransactionAsync(r -> {
                RealmTransaction realmTx = r.where(RealmTransaction.class)
                        .equalTo("hash", oldTxHash)
                        .findFirst();

                if (realmTx != null)
                {
                    //deleteOperations(realmTx);
                    realmTx.deleteFromRealm();
                }
            });
        }
        catch (Exception e)
        {
            //do not record
            e.printStackTrace();
        }
    }

    public static void fill(Realm realm, RealmTransaction item, Transaction transaction)
    {
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
    }

    public static Transaction convert(RealmTransaction rawItem) {
        boolean isConstructor = rawItem.getInput() != null && rawItem.getInput().equals(Transaction.CONSTRUCTOR);

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
                isConstructor
                );
    }

    @Override
    public void markTransactionBlock(String walletAddress, String hash, long blockValue)
    {
        try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
        {
            instance.executeTransactionAsync(r -> {
                RealmTransaction realmTx = r.where(RealmTransaction.class)
                        .equalTo("hash", hash)
                        .findFirst();

                if (realmTx != null)
                {
                    realmTx.setBlockNumber(String.valueOf(blockValue));
                    realmTx.setTimeStamp(System.currentTimeMillis() / 1000); //update timestamp so it's updated on the UI
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    @Override
    public Realm getRealmInstance(Wallet wallet)
    {
        return realmManager.getRealmInstance(wallet);
    }
}
