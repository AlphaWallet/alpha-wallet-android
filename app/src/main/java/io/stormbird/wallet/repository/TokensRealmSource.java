package io.stormbird.wallet.repository;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenFactory;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.TokenTicker;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.entity.RealmToken;
import io.stormbird.wallet.repository.entity.RealmTokenTicker;
import io.stormbird.wallet.service.RealmManager;

import static io.stormbird.wallet.interact.SetupTokensInteract.EXPIRED_CONTRACT;

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
    public Single<Token> saveToken(NetworkInfo networkInfo, Wallet wallet, Token token) {
        return Single.fromCallable(() -> {
            Date now = new Date();
            saveToken(networkInfo, wallet, token, now);
            return token;
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
                        .notEqualTo("address", wallet.address)
                        .findAll();

                realmItems = deleteCapsAddresses(realm, realmItems, wallet.address);

                return convert(realmItems, System.currentTimeMillis());
            }
            finally
            {
                if (realm != null)
                {
                    realm.close();
                }
            }
        });
    }

    //TODO: Deprecate this function after 2 months (September). Only purpose is to fix a problem with duplicate address being logged
    //TODO: If the address was specified in the XML file as a mixture of caps and lowercase. This has been fixed within the root token handling
    //TODO: However artifacts from those ancient times may still persist and cause update problems hence this function.
    private RealmResults<RealmToken> deleteCapsAddresses(Realm realm, RealmResults<RealmToken> realmItems, String walletAddress)
    {
        boolean withinRealmTx = false;
        for (int i = 0; i < realmItems.size(); i++)
        {
            RealmToken realmItem = realmItems.get(i);
            if (containsCaps(realmItem)) //remove any entry who's address contains caps. NB this only happens when address is taken from XML
                                         // but, since we don't control XML content we need to deal with problems here
            {
                if (!withinRealmTx)
                {
                    realm.beginTransaction();
                    withinRealmTx = true;
                }
                realmItem.deleteFromRealm();
            }
        }

        if (withinRealmTx)
        {
            realm.commitTransaction();

            realmItems = realm.where(RealmToken.class)
                    .sort("addedTime", Sort.ASCENDING)
                    .equalTo("isEnabled", true)
                    .notEqualTo("address", walletAddress)
                    .findAll();
        }

        return realmItems;
    }

    private boolean containsCaps(RealmToken token)
    {
        String address = token.getAddress();

        for (int n = 0; n < address.length(); n++)
        {
            char ch = address.charAt(n);
            if (ch >= 'A' && ch <= 'F')
            {
                return true;
            }
        }

        return false;
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
                        .notEqualTo("address", wallet.address)
                        .findAll();
                //Log.d("TRS", "Sz: " + realmItems.size());
                return convertBalance(realmItems, System.currentTimeMillis());
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Observable<List<Token>> fetchEnabledTokensSequentialList(NetworkInfo networkInfo, Wallet wallet)
    {
        return fetchEnabledTokens(networkInfo, wallet).toObservable()
                .flatMap(this::transformList);
    }

    private Observable<List<Token>> transformList(Token[] tokens)
    {
        return Observable.just(Arrays.asList(tokens));
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
                TransactionsRealmCache.addRealm();
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
                if (realm != null && realm.isInTransaction()) {
                    realm.cancelTransaction();
                }
            } finally {
                if (realm != null) {
                    realm.close();
                    TransactionsRealmCache.subRealm();
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

            TransactionsRealmCache.addRealm();
            realm.beginTransaction();
            if (realmToken != null) {
                realmToken.setEnabled(isEnabled);
            }
            realm.commitTransaction();
        } catch (Exception ex) {
            if (realm != null && realm.isInTransaction()) {
                realm.cancelTransaction();
            }
        } finally {
            if (realm != null) {
                realm.close();
                TransactionsRealmCache.subRealm();
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
            if (realm != null && realm.isInTransaction()) {
                realm.cancelTransaction();
            }
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
    }

    @Override
    public Token getTokenBalance(NetworkInfo network, Wallet wallet, String address)
    {
        Realm realm = null;
        Token result = null;
        try
        {
            realm = realmManager.getRealmInstance(network, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", address)
                    .findFirst();

            if (realmToken != null)
            {
                TokenFactory tf = new TokenFactory();
                TokenInfo info = tf.createTokenInfo(realmToken);
                result = tf.createTokenBalance(info, realmToken, realmToken.getUpdatedTime());//; new Token(info, balance, realmItem.getUpdatedTime());
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (realm != null)
            {
                realm.close();
            }
        }
        return result;
    }

    @Override
    public void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token)
    {
        Realm realm = null;
        try
        {
            realm = realmManager.getRealmInstance(network, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();

            if (token.hasPositiveBalance() && realmToken == null)
            {
                saveToken(network, wallet, token, new Date());

                realmToken = realm.where(RealmToken.class)
                        .equalTo("address", token.tokenInfo.address)
                        .findFirst();
            }

            //Don't update realm unless we need to.
            if (realmToken != null && token.checkRealmBalanceChange(realmToken))
            {
                TransactionsRealmCache.addRealm();
                realm.beginTransaction();
                token.setRealmBalance(realmToken);
                if (token.tokenInfo.name != null)
                {
                    realmToken.setName(token.tokenInfo.name);
                    realmToken.setSymbol(token.tokenInfo.symbol);
                    realmToken.setDecimals(token.tokenInfo.decimals);
                    realmToken.setStormbird(token.tokenInfo.isStormbird);
                }
                realmToken.setNullCheckCount(0);
                realm.commitTransaction();
                TransactionsRealmCache.subRealm();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            if (realm != null && realm.isInTransaction())
            {
                realm.cancelTransaction();
            }
        }
        finally
        {
            if (realm != null)
            {
                realm.close();
            }
        }
    }

    @Override
    public void updateTokenDestroyed(NetworkInfo network, Wallet wallet, Token token)
    {
        Realm realm = null;
        try
        {
            realm = realmManager.getRealmInstance(network, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();

            TransactionsRealmCache.addRealm();
            realm.beginTransaction();
            realmToken.setName(null);
            realmToken.setSymbol(null);
            realm.commitTransaction();
        }
        catch (Exception ex)
        {
            if (realm != null && realm.isInTransaction())
            {
                realm.cancelTransaction();
            }
        }
        finally
        {
            if (realm != null)
            {
                realm.close();
                TransactionsRealmCache.subRealm();
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
            if (realmToken == null)
            {
                TransactionsRealmCache.addRealm();
                realm.beginTransaction();
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
                realm.commitTransaction();
                TransactionsRealmCache.subRealm();
            }
            else
            {
                Log.d(TAG, "Update Token: " + token.getFullName());
                if (!token.tokenInfo.name.equals(realmToken.getName()) || !token.tokenInfo.symbol.equals(realmToken.getSymbol()))
                {
                    //has token changed?
                    TransactionsRealmCache.addRealm();
                    realm.beginTransaction();
                    realmToken.setName(token.tokenInfo.name);
                    realmToken.setSymbol(token.tokenInfo.symbol);
                    realmToken.setDecimals(token.tokenInfo.decimals);
                    realmToken.setAddedTime(currentTime.getTime());
                    realmToken.setEnabled(true);
                    realm.commitTransaction();
                    TransactionsRealmCache.subRealm();
                }
                //realmToken.setBalance(token.getFullBalance());
            }
        } catch (Exception ex) {
            if (realm != null && realm.isInTransaction()) {
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
        if (realmItems.size() == 0) return null;
        TokenFactory tf = new TokenFactory();
        Token result = null;
        RealmToken realmItem = realmItems.get(0);
            if (realmItem != null) {
                TokenInfo info = tf.createTokenInfo(realmItem);
                result = tf.createToken(info, realmItem, realmItem.getUpdatedTime());//; new Token(info, balance, realmItem.getUpdatedTime());
            }
        return result;
    }

    @Override
    public void setTokenTerminated(NetworkInfo network, Wallet wallet, Token token)
    {
        Realm realm = null;
        try
        {
            realm = realmManager.getRealmInstance(network, wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .findFirst();

            TransactionsRealmCache.addRealm();
            realm.beginTransaction();
            token.setIsTerminated(realmToken);
            realm.commitTransaction();
        }
        catch (Exception ex)
        {
            if (realm != null && realm.isInTransaction())
            {
                realm.cancelTransaction();
            }
        }
        finally
        {
            if (realm != null)
            {
                realm.close();
                TransactionsRealmCache.subRealm();
            }
        }
    }
}
