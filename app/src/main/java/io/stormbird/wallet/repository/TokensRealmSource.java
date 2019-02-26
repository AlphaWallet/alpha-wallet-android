package io.stormbird.wallet.repository;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenFactory;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.TokenTicker;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.entity.opensea.AssetContract;
import io.stormbird.wallet.entity.opensea.Trait;
import io.stormbird.wallet.repository.entity.RealmERC721Asset;
import io.stormbird.wallet.repository.entity.RealmERC721Token;
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
                if (token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT) && token.tokenInfo.symbol != null)
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
                if (token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT)) //only store a contract with valid name
                {
                    saveToken(networkInfo, wallet, token, now);
                }
            }
            return items;
        });
    }

    @Override
    public Single<Token[]> saveERC721Tokens(NetworkInfo networkInfo, Wallet wallet, Token[] tokens)
    {
        return Single.fromCallable(() -> {
            Date now = new Date();
            try (Realm realm = realmManager.getERC721RealmInstance(networkInfo, wallet))
            {
                realm.beginTransaction();
                for (Token token : tokens)
                {
                    saveERC721Token(realm, wallet, token, now);
                }
                realm.commitTransaction();
            }
            checkTokenRealm(networkInfo, wallet, tokens);
            return tokens;
        });
    }

    private void deleteRealmToken(Realm realm, String address)
    {
        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", address)
                .findFirst();

        if (realmToken != null)
        {
            realmToken.deleteFromRealm();
        }
    }

    private void checkTokenRealm(NetworkInfo networkInfo, Wallet wallet, Token[] tokens)
    {
        Map<String, Token> erc721Map = new HashMap<>();
        for (Token t : tokens) erc721Map.put(t.getAddress(), t);
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.beginTransaction();
            RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                    .findAll();

            for (RealmToken rt : realmItems)
            {
                if (erc721Map.containsKey(rt.getAddress()))
                {
                    rt.deleteFromRealm();
                }
            }

            realm.commitTransaction();
        }
    }

    @Override
    public Single<Token> saveToken(NetworkInfo networkInfo, Wallet wallet, Token token) {
        return Single.fromCallable(() -> {
            Date now = new Date();
            if (token instanceof ERC721Token)
            {
                saveERC721Token(networkInfo, wallet, token, now);
            }
            else
            {
                saveToken(networkInfo, wallet, token, now);
            }
            return token;
        });
    }

    @Override
    public Single<Token> fetchEnabledToken(NetworkInfo networkInfo, Wallet wallet, String address) {
        return Single.fromCallable(() -> {
            Realm realm = null;

            try {
                realm = realmManager.getRealmInstance(wallet);
                RealmResults<RealmToken> realmItem = realm.where(RealmToken.class)
                        .equalTo("address", address)
                        .equalTo("chainId", networkInfo.chainId)
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
                realm = realmManager.getRealmInstance(wallet);
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("isEnabled", true)
                        .notEqualTo("address", wallet.address)
                        .equalTo("chainId", networkInfo.chainId)
                        .findAll();

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
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("isEnabled", true)
                        .notEqualTo("address", wallet.address)
                        .equalTo("chainId", networkInfo.chainId)
                        .findAll();

                return convertBalance(realmItems, System.currentTimeMillis(), wallet, networkInfo.chainId);
            }
            catch (Exception e)
            {
                return new Token[0]; //ensure fetch completes
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
                realm = realmManager.getRealmInstance(wallet);
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("chainId", networkInfo.chainId)
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
    public Single<Token[]> fetchERC721Tokens(NetworkInfo network, Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getERC721RealmInstance(network, wallet))
            {
                RealmResults<RealmERC721Token> realmItems = realm.where(RealmERC721Token.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .findAll();

                return convertERC721(realmItems, realm, network, wallet);
            }
            catch (Exception e)
            {
                return new Token[0]; // ensure fetch always completes
            }
        });
    }

    @Override
    public Completable saveTickers(NetworkInfo network, Wallet wallet, TokenTicker[] tokenTickers) {
        return Completable.fromAction(() -> {
            Realm realm = null;
            try {
                realm = realmManager.getRealmInstance(wallet);
                TransactionsRealmCache.addRealm();
                realm.beginTransaction();
                long now = System.currentTimeMillis();
                for (TokenTicker tokenTicker : tokenTickers) {
                    RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                            .equalTo("contract", tokenTicker.contract)
                            .equalTo("chainId", network.chainId)
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
                realm = realmManager.getRealmInstance(wallet);
                realm.beginTransaction();
                long minCreatedTime = System.currentTimeMillis() - ACTUAL_TOKEN_TICKER_INTERVAL;
                RealmResults<RealmTokenTicker> rawItems = realm.where(RealmTokenTicker.class)
                        .greaterThan("updatedTime", minCreatedTime)
                        .equalTo("chainId", network.chainId)
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
                                rawItem.getImage()));
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
            realm = realmManager.getRealmInstance(wallet);
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
            realm = realmManager.getRealmInstance(wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .equalTo("chainId", network.chainId)
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
        Token result = null;
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", address)
                    .equalTo("chainId", network.chainId)
                    .findFirst();

            if (realmToken != null)
            {
                TokenFactory tf = new TokenFactory();
                TokenInfo info = tf.createTokenInfo(realmToken);
                result = tf.createToken(info, realmToken, realmToken.getUpdatedTime());//; new Token(info, balance, realmItem.getUpdatedTime());
                result.setTokenWallet(wallet.address);
                result.setTokenNetwork(network.chainId);
                result.lastBlockCheck = realmToken.getLastBlock();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return result;
    }

    @Override
    public void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .equalTo("chainId", network.chainId)
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
                }
                token.setRealmInterfaceSpec(realmToken);
                token.setRealmAuxData(realmToken);
                realmToken.setNullCheckCount(0);
                realm.commitTransaction();
                TransactionsRealmCache.subRealm();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void updateTokenDestroyed(NetworkInfo network, Wallet wallet, Token token)
    {
        Realm realm = null;
        try
        {
            realm = realmManager.getRealmInstance(wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .equalTo("chainId", network.chainId)
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

    @Override
    public Disposable storeBlockRead(Token token, NetworkInfo network, Wallet wallet)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;
                    @Override
                    public void onStart()
                    {
                        realm = realmManager.getRealmInstance(wallet);
                        RealmToken realmToken = realm.where(RealmToken.class)
                                .equalTo("address", token.tokenInfo.address)
                                .equalTo("chainId", network.chainId)
                                .findFirst();

                        if (realmToken != null)
                        {
                            TransactionsRealmCache.addRealm();
                            realm.beginTransaction();
                            token.setRealmLastBlock(realmToken);
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        realm.commitTransaction();
                        TransactionsRealmCache.subRealm();
                        realm.close();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        if (realm != null && !realm.isClosed())
                        {
                            realm.close();
                        }
                    }
                });
    }

    private void saveToken(NetworkInfo networkInfo, Wallet wallet, Token token, Date currentTime) {
        Realm realm = null;
        try {
            realm = realmManager.getRealmInstance(wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", token.tokenInfo.address)
                    .equalTo("chainId", networkInfo.chainId)
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
                token.setRealmInterfaceSpec(realmToken);
                token.setRealmAuxData(realmToken);
                realmToken.setEnabled(true);
                realmToken.setBurnList("");
                realmToken.setChainId(token.tokenInfo.chainId);
                realm.commitTransaction();
                TransactionsRealmCache.subRealm();
            }
            else
            {
                Log.d(TAG, "Update Token: " + token.getFullName());
                if (token.checkRealmBalanceChange(realmToken))
                {
                    //has token changed?
                    TransactionsRealmCache.addRealm();
                    realm.beginTransaction();
                    realmToken.setName(token.tokenInfo.name);
                    realmToken.setSymbol(token.tokenInfo.symbol);
                    realmToken.setDecimals(token.tokenInfo.decimals);
                    realmToken.setAddedTime(currentTime.getTime());
                    token.setRealmInterfaceSpec(realmToken);
                    token.setRealmAuxData(realmToken);
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

    /**
     * Store single ERC721 token
     * @param wallet
     * @param token
     * @param now
     */
    private void saveERC721Token(NetworkInfo network, Wallet wallet, Token token, Date now)
    {
        try (Realm realm = realmManager.getERC721RealmInstance(network, wallet))
        {
            realm.beginTransaction();
            saveERC721Token(realm, wallet, token, now);
            realm.commitTransaction();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void saveERC721Token(Realm realm, Wallet wallet, Token token, Date currentTime) {
        ERC721Token e;
        if (token instanceof ERC721Token)
        {
            e = (ERC721Token) token;
        }
        else
        {
            return; //no storage here
        }

        //initial check to ensure all assets in this class have the same contract
        AssetContract contract = null;
        String address = token.tokenInfo.address;

        String contractName = null;
        String contractSymbol = null;
        String schemaName = null;

        for (Asset asset : e.tokenBalance)
        {
            if (address != null)
            {
                contract = asset.getAssetContract();
                if (!address.equals(asset.getAssetContract().getAddress()))
                {
                    Log.d("TRS", "Addresses Don't match recording ERC721");
                    return;
                }
            }
            else
            {
                address = asset.getAssetContract().getAddress();
                contract = asset.getAssetContract();

                contractName = contract.getName();
                contractSymbol = contract.getSymbol();
                schemaName = contract.getSchemaName();
            }
        }

        if (contractName == null)
        {
            contractName = token.tokenInfo.name;
            contractSymbol = token.tokenInfo.symbol;
            schemaName = "ERC721";
        }

        deleteRealmToken(realm, address); //in case it was recorded as normal token

        RealmERC721Token realmToken = realm.where(RealmERC721Token.class)
                .equalTo("address", address)
                .findFirst();

        if (realmToken == null)
        {
            //create new storage
            Log.d(TAG, "Save New ERC721 Token: " + token.tokenInfo.name + " :" + address);
            realmToken = realm.createObject(RealmERC721Token.class, address);
            realmToken.setName(contractName);
            realmToken.setSymbol(contractSymbol);
            realmToken.setAddedTime(currentTime.getTime());
            realmToken.setUpdatedTime(currentTime.getTime());
            realmToken.setSchemaName(schemaName);
            realmToken.setTokenIdList(e.tokenBalance);
        }
        else
        {
            //update balance if changed
            List<String> tokenBalance = realmToken.getTokenIdList();
            boolean needsUpdate = false;
            if (tokenBalance.size() != e.tokenBalance.size())
            {
                needsUpdate = true;
            }
            else
            {
                for (int i = 0; i < tokenBalance.size(); i++)
                {
                    if (!tokenBalance.get(i).equals(e.tokenBalance.get(i).getTokenId()))
                        needsUpdate = true;
                }
            }

            if (needsUpdate)
            {
                //balance changed, remove old assets
                deleteAssets(realm, address);

                //update balance
                realmToken.setUpdatedTime(currentTime.getTime());
                realmToken.setTokenIdList(e.tokenBalance);
            }
        }

        //now create the assets inside this
        for (Asset asset : e.tokenBalance)
        {
            RealmERC721Asset realmAsset = realm.where(RealmERC721Asset.class)
                    .equalTo("tokenIdAddr", RealmERC721Asset.tokenIdAddrName(asset.getTokenId(), contract.getAddress()))
                    .findFirst();

            if (realmAsset == null)
            {
                realmAsset = realm.createObject(RealmERC721Asset.class,
                                                RealmERC721Asset.tokenIdAddrName(asset.getTokenId(), contract.getAddress()));

                realmAsset.setName(asset.getName());
                realmAsset.setDescription(asset.getDescription());
                realmAsset.setExternalLink(asset.getExternalLink());
                realmAsset.setImagePreviewUrl(asset.getImagePreviewUrl());
                realmAsset.setBackgroundColor(asset.getBackgroundColor());
                realmAsset.setTraits(asset.getTraits());
            }
            else
            {
                //see if traits have changed
                List<Trait> traits = realmAsset.getTraits();
                if (traits.size() != asset.getTraits().size() || traitsDifferent(traits, asset.getTraits()))
                {
                    realmAsset.setImagePreviewUrl(asset.getImagePreviewUrl());
                    realmAsset.setTraits(asset.getTraits());
                }
            }
        }
    }

    private void deleteAssets(Realm realm, String address)
    {
        String key = address + "-";

        try
        {
            RealmResults<RealmERC721Asset> realmAssets = realm.where(RealmERC721Asset.class)
                    .beginsWith("tokenIdAddr", key)
                    .findAll();

            for (RealmERC721Asset asset : realmAssets)
            {
                asset.deleteFromRealm();
            }
        }
        catch (Exception e)
        {
            //silent
        }
    }

    private boolean traitsDifferent(List<Trait> traits, List<Trait> traits1)
    {
        for (int i = 0; i < traits.size(); i++)
        {
            if (!traits.get(i).getTraitType().equals(traits1.get(i).getTraitType())
                || !traits.get(i).getValue().equals(traits1.get(i).getValue()))
            {
                return true;
            }
        }

        return false;
    }

    private Token[] convertERC721(RealmResults<RealmERC721Token> realmItems, Realm realm, NetworkInfo network, Wallet wallet) {
        int len = realmItems.size();
        Token[] result = new Token[len];
        for (int i = 0; i < len; i++)
        {
            TokenFactory tf = new TokenFactory();
            RealmERC721Token realmItem = realmItems.get(i);
            if (realmItem != null)
            {
                //get all the assets for this ERC first
                List<String> tokenIdAddrs = realmItem.getTokenIdList();
                List<Asset> assets = getERC721Assets(tokenIdAddrs, realm, realmItem);

                result[i] = tf.createERC721Token(realmItem, assets, realmItem.getUpdatedTime());
                result[i].setTokenWallet(wallet.address);
                result[i].setTokenNetwork(network.chainId);
            }
        }
        return result;
    }

    private List<Asset> getERC721Assets(List<String> keys, Realm realm, RealmERC721Token realmItem)
    {
        List<Asset> assets = new ArrayList<>();
        //String address, String name, String symbol, String schemaName)
        AssetContract contract = new AssetContract(realmItem.getAddress(), realmItem.getName(),
                                                   realmItem.getSymbol(), realmItem.getSchemaName());
        for (String key : keys)
        {
            RealmERC721Asset realmAsset = realm.where(RealmERC721Asset.class)
                    .equalTo("tokenIdAddr", RealmERC721Asset.tokenIdAddrName(key, contract.getAddress()))
                    .findFirst();

            if (realmAsset != null)
            {
                Asset asset = new Asset(realmAsset.getTokenId(), contract);
                asset.setBackgroundColor(realmAsset.getBackgroundColor());
                asset.setDescription(realmAsset.getDescription());
                asset.setExternalLink(realmAsset.getExternalLink());
                asset.setImagePreviewUrl(realmAsset.getImagePreviewUrl());
                asset.setTraits(realmAsset.getTraits());
                asset.setName(realmAsset.getName());

                assets.add(asset);
            }
        }

        return assets;
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

    private Token[] convertBalance(RealmResults<RealmToken> realmItems, long now, Wallet wallet, int network) {
        TokenFactory tf = new TokenFactory();
        List<Token> tokenList = new ArrayList<>();
        for (RealmToken realmItem : realmItems)
        {
            if (realmItem != null)
            {
                TokenInfo info = tf.createTokenInfo(realmItem);
                Token token = tf.createToken(info, realmItem, realmItem.getUpdatedTime());//; new Token(info, balance, realmItem.getUpdatedTime());
                if (token != null)
                {
                    token.setTokenWallet(wallet.address);
                    token.setTokenNetwork(network);
                    tokenList.add(token);
                }
            }
        }

        return tokenList.toArray(new Token[0]);
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

    private void createBlankToken(Realm realm, Token token)
    {
        RealmToken realmToken = realm.createObject(RealmToken.class, token.tokenInfo.address);
        realmToken.setName(token.tokenInfo.name);
        realmToken.setSymbol(token.tokenInfo.symbol);
        realmToken.setDecimals(0);
        realmToken.setUpdatedTime(-1);
        realmToken.setAddedTime(-1);
        realmToken.setEnabled(true);
        realmToken.setBurnList("");
        realmToken.setInterfaceSpec(0);
        realmToken.setChainId(token.tokenInfo.chainId);
    }

    @Override
    public Disposable setTokenTerminated(Token token, NetworkInfo network, Wallet wallet)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;

                    @Override
                    public void onStart()
                    {
                        realm = realmManager.getRealmInstance(wallet);
                        RealmToken realmToken = realm.where(RealmToken.class)
                                .equalTo("address", token.tokenInfo.address)
                                .equalTo("chainId", network.chainId)
                                .findFirst();

                        TransactionsRealmCache.addRealm();
                        realm.beginTransaction();
                        if (realmToken == null)
                        {
                            createBlankToken(realm, token);
                        }
                        else
                        {
                            token.setIsTerminated(realmToken);
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        realm.commitTransaction();
                        TransactionsRealmCache.subRealm();
                        realm.close();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        if (realm != null && !realm.isClosed())
                        {
                            realm.close();
                        }
                    }
                });
    }
}
