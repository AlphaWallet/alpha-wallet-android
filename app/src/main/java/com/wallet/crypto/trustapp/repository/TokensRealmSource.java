package com.wallet.crypto.trustapp.repository;

import android.text.TextUtils;
import android.text.format.DateUtils;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.entity.RealmToken;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

public class TokensRealmSource implements TokenLocalSource {

    private static final long ACTUAL_BALANCE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    private final Map<String, RealmConfiguration> realmConfigurations = new HashMap<>();

    @Override
    public Completable saveTokens(NetworkInfo networkInfo, Wallet wallet, Token[] items) {
        return Completable.fromAction(() -> {
            Date now = new Date();
            for (Token token : items) {
                saveToken(networkInfo, wallet, token, now);
            }
        });
    }

    @Override
    public Single<Token[]> fetchTokens(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            long now = System.currentTimeMillis();
            Realm realm = null;
            try {
                realm = getRealmInstance(networkInfo, wallet);
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("isEnabled", true)
                        .findAll();
                int len = realmItems.size();
                Token[] result = new Token[len];
                for (int i = 0; i < len; i++) {
                    RealmToken realmItem = realmItems.get(i);
                    if (realmItem != null) {
                        TokenInfo info = new TokenInfo(
                                realmItem.getAddress(),
                                realmItem.getName(),
                                realmItem.getSymbol(),
                                realmItem.getDecimals());
                        BigDecimal balance = TextUtils.isEmpty(realmItem.getBalance()) || realmItem.getUpdatedTime() + ACTUAL_BALANCE_INTERVAL < now
                                ? null : new BigDecimal(realmItem.getBalance());
                        result[i] = new Token(info, balance);
                    }
                }
                return result;
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    private void saveToken(NetworkInfo networkInfo, Wallet wallet, Token token, Date currentTime) {
        Realm realm = null;
        try {
            realm = getRealmInstance(networkInfo, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();
            realm.beginTransaction();
            if (realmToken == null) {
                realmToken = realm.createObject(RealmToken.class, token.tokenInfo.address);
                realmToken.setName(token.tokenInfo.name);
                realmToken.setSymbol(token.tokenInfo.symbol);
                realmToken.setDecimals(token.tokenInfo.decimals);
                realmToken.setAddedTime(currentTime.getTime());
                realmToken.setEnabled(true);
            }
            realmToken.setBalance(token.balance.toString());
            realmToken.setUpdatedTime(currentTime.getTime());
            realm.commitTransaction();
        } catch (Exception ex) {
            if (realm != null) {
                realm.cancelTransaction();
            }
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
    }

    private Realm getRealmInstance(NetworkInfo networkInfo, Wallet wallet) {
        String name = wallet.address + "_" + networkInfo.name + "_tkns.realm";
        RealmConfiguration config = realmConfigurations.get(name);
        if (config == null) {
            config = new RealmConfiguration.Builder()
                    .name(name)
                    .schemaVersion(1)
                    .build();
            realmConfigurations.put(name, config);
        }
        return Realm.getInstance(config);
    }
}
