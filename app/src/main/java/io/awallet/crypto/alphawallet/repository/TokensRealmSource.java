package io.awallet.crypto.alphawallet.repository;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenFactory;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.TokenTicker;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.entity.RealmToken;
import io.awallet.crypto.alphawallet.repository.entity.RealmTokenTicker;
import io.awallet.crypto.alphawallet.service.RealmManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static io.awallet.crypto.alphawallet.interact.SetupTokensInteract.EXPIRED_CONTRACT;

public class TokensRealmSource implements TokenLocalSource {

    public static final String TAG = "TLS";
    public static final long ACTUAL_BALANCE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final long ACTUAL_TOKEN_TICKER_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final String COINMARKETCAP_IMAGE_URL = "https://files.coinmarketcap.com/static/img/coins/128x128/%s.png";

    private final RealmManager realmManager;

    public TokensRealmSource(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    @Override
    public Completable saveTokens(NetworkInfo networkInfo, Wallet wallet, Token[] items) {
        return Completable.fromAction(() -> {
            Date now = new Date();
            for (Token token : items) {
                if (token.tokenInfo.name == null || token.tokenInfo.name.equals(EXPIRED_CONTRACT) || token.tokenInfo.symbol == null)
                {
                    Log.d(TAG, "Attempting to store invalid contract: " + token.getAddress());
                }
                else
                {
                    saveToken(networkInfo, wallet, token, now);
                }
            }
        });
    }

    @Override
    public Single<Token[]> saveTokensList(NetworkInfo networkInfo, Wallet wallet, Token[] items) {
        return Single.fromCallable(() -> {
            Date now = new Date();
            for (Token token : items) {
                if (token.tokenInfo.name == null || token.tokenInfo.name.equals(EXPIRED_CONTRACT))
                {
                    Log.d(TAG, "Attempting to store invalid contract: " + token.getAddress());
                }
                else
                {
                    saveToken(networkInfo, wallet, token, now);
                }
            }
            return items;
        });
    }

    @Override
    public Single<Token> fetchEnabledToken(NetworkInfo networkInfo, Wallet wallet, String address) {
        return Single.fromCallable(() -> {
            Realm realm = null;

            try {
                realm = realmManager.getRealmInstance(networkInfo, wallet);
                RealmResults<RealmToken> realmItem = realm.where(RealmToken.class)
                        .equalTo("address", address)
                        .findAll();

                return convertSingle(realmItem, System.currentTimeMillis());
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Single<Token[]> fetchEnabledTokens(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm realm = null;
            try {
                realm = realmManager.getRealmInstance(networkInfo, wallet);
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("isEnabled", true)
                        .findAll();
                return convert(realmItems, System.currentTimeMillis());
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Single<Token[]> fetchEnabledTokensWithBalance(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm realm = null;
            try {
                realm = realmManager.getRealmInstance(networkInfo, wallet);
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("isEnabled", true)
                        .findAll();
                Log.d("TRS", "Sz: " + realmItems.size());
                return convertBalance(realmItems, System.currentTimeMillis());
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Single<Token[]> fetchAllTokens(NetworkInfo networkInfo, Wallet wallet) {
        return Single.fromCallable(() -> {
            Realm realm = null;
            try {
                realm = realmManager.getRealmInstance(networkInfo, wallet);
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .findAll();

                return convert(realmItems, System.currentTimeMillis());
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Completable saveTickers(NetworkInfo network, Wallet wallet, TokenTicker[] tokenTickers) {
        return Completable.fromAction(() -> {
            Realm realm = null;
            try {
                realm = realmManager.getRealmInstance(network, wallet);
                realm.beginTransaction();
                long now = System.currentTimeMillis();
                for (TokenTicker tokenTicker : tokenTickers) {
                    RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                            .equalTo("contract", tokenTicker.contract)
                            .findFirst();
                    if (realmItem == null) {
                        realmItem = realm.createObject(RealmTokenTicker.class, tokenTicker.contract);
                        realmItem.setCreatedTime(now);
                    }
                    realmItem.setId(tokenTicker.id);
                    realmItem.setPercentChange24h(tokenTicker.percentChange24h);
                    realmItem.setPrice(tokenTicker.price);
                    realmItem.setImage(TextUtils.isEmpty(tokenTicker.image)
                            ? String.format(COINMARKETCAP_IMAGE_URL, tokenTicker.id)
                            : tokenTicker.image);
                    realmItem.setUpdatedTime(now);
                }
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
        });
    }

    @Override
    public Single<TokenTicker[]> fetchTickers(NetworkInfo network, Wallet wallet, Token[] tokens) {
        return Single.fromCallable(() -> {
            ArrayList<TokenTicker> tokenTickers = new ArrayList<>();
            Realm realm = null;
            try {
                realm = realmManager.getRealmInstance(network, wallet);
                realm.beginTransaction();
                long minCreatedTime = System.currentTimeMillis() - ACTUAL_TOKEN_TICKER_INTERVAL;
                RealmResults<RealmTokenTicker> rawItems = realm.where(RealmTokenTicker.class)
                        .greaterThan("updatedTime", minCreatedTime)
                        .findAll();
                int len = rawItems.size();
                for (int i = 0; i < len; i++) {
                    RealmTokenTicker rawItem = rawItems.get(i);
                    if (rawItem != null) {
                        tokenTickers.add(new TokenTicker(
                                rawItem.getId(),
                                rawItem.getContract(),
                                rawItem.getPrice(),
                                rawItem.getPercentChange24h(),
                                rawItem.getImage(),
                                rawItem.isStormbird()));
                    }
                }
                realm.commitTransaction();
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
            return tokenTickers.size() == 0
                    ? null
                    : tokenTickers.toArray(new TokenTicker[tokenTickers.size()]);
        });
    }

    @Override
    public void setEnable(NetworkInfo network, Wallet wallet, Token token, boolean isEnabled) {
        Realm realm = null;
        try {
            token.tokenInfo.isEnabled = isEnabled;
            realm = realmManager.getRealmInstance(network, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();

            realm.beginTransaction();
            if (realmToken != null) {
                realmToken.setEnabled(isEnabled);
            }
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

    @Override
    public void updateTokenBurn(NetworkInfo network, Wallet wallet, Token token, List<Integer> burn) {
        Realm realm = null;
        try {
            realm = realmManager.getRealmInstance(network, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();
            realm.beginTransaction();
            if (realmToken != null) {
                token.setRealmBurn(realmToken, burn);
            }
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

    @Override
    public void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token) {
        Realm realm = null;
        try {
            realm = realmManager.getRealmInstance(network, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();
            realm.beginTransaction();
            if (realmToken != null) {
                token.setRealmBalance(realmToken);
            }
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

    private void saveToken(NetworkInfo networkInfo, Wallet wallet, Token token, Date currentTime) {
        Realm realm = null;
        try {
            realm = realmManager.getRealmInstance(networkInfo, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();
            realm.beginTransaction();
            if (realmToken == null) {
                Log.d(TAG, "Save New Token: " + token.getFullName() + " :" + token.tokenInfo.address);
                realmToken = realm.createObject(RealmToken.class, token.tokenInfo.address);
                realmToken.setName(token.tokenInfo.name);
                realmToken.setSymbol(token.tokenInfo.symbol);
                realmToken.setDecimals(token.tokenInfo.decimals);
                realmToken.setAddedTime(currentTime.getTime());
                realmToken.setEnabled(true);
                realmToken.setBurnList("");
                if (token instanceof Ticket) {
                    realmToken.setStormbird(true);
                }
            }
            else
            {
                Log.d(TAG, "Update Token: " + token.getFullName());
            }
            realmToken.setBalance(token.getFullBalance());
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

    private Token[] convert(RealmResults<RealmToken> realmItems, long now) {
        int len = realmItems.size();
        TokenFactory tf = new TokenFactory();
        Token[] result = new Token[len];
        for (int i = 0; i < len; i++) {
            RealmToken realmItem = realmItems.get(i);
            if (realmItem != null) {
                TokenInfo info = tf.createTokenInfo(realmItem);
                result[i] = tf.createToken(info, realmItem, realmItem.getUpdatedTime());//; new Token(info, balance, realmItem.getUpdatedTime());
            }
        }
        return result;
    }

    private Token[] convertBalance(RealmResults<RealmToken> realmItems, long now) {
        int len = realmItems.size();
        TokenFactory tf = new TokenFactory();
        Token[] result = new Token[len];
        for (int i = 0; i < len; i++) {
            RealmToken realmItem = realmItems.get(i);
            if (realmItem != null) {
                TokenInfo info = tf.createTokenInfo(realmItem);
                result[i] = tf.createTokenBalance(info, realmItem, realmItem.getUpdatedTime());//; new Token(info, balance, realmItem.getUpdatedTime());
            }
        }
        return result;
    }

    private Token convertSingle(RealmResults<RealmToken> realmItems, long now) {
        int len = realmItems.size();
        TokenFactory tf = new TokenFactory();
        Token result = null;
        RealmToken realmItem = realmItems.get(0);
            if (realmItem != null) {
                TokenInfo info = tf.createTokenInfo(realmItem);
                result = tf.createToken(info, realmItem, realmItem.getUpdatedTime());//; new Token(info, balance, realmItem.getUpdatedTime());
            }
        return result;
    }
}
