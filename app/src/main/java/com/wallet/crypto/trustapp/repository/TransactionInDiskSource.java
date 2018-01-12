package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.entity.RealmTokenInfo;
import com.wallet.crypto.trustapp.repository.entity.RealmTransaction;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class TransactionInDiskSource implements TransactionLocalSource {
	@Override
	public Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet) {
		return null;
	}

	@Override
	public void putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions) {
        Completable.fromAction(() -> {
            Realm realm = null;
            try {
                realm = getRealmInstance(networkInfo, wallet);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        for (Transaction transaction : transactions) {
                            RealmTransaction item = realm.createObject(RealmTransaction.class, "hash");
                            fill(item, transaction);
                        }
                    }
                });
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(() -> {}, t -> {});
	}

    private void fill(RealmTransaction item, Transaction transaction) {

    }

    private void putInNeed(NetworkInfo networkInfo, Wallet wallet, TokenInfo tokenInfo) {

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

    private Realm getRealmInstance(NetworkInfo networkInfo, Wallet wallet) {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name(wallet.address + "-" + networkInfo.name + ".realm")
                .schemaVersion(1)
                .build();
        return Realm.getInstance(config);
    }

    private Transaction convert(RealmTransaction rawItem) {
        return null;
    }
}
