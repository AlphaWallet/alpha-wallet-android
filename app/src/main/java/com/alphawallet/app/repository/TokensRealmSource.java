package com.alphawallet.app.repository;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.ERC721Token;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenFactory;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.TokenTicker;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmERC721Asset;
import com.alphawallet.app.repository.entity.RealmERC721Token;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenTicker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmException;

import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.opensea.Trait;
import com.alphawallet.app.service.RealmManager;
import org.web3j.crypto.WalletUtils;

import static com.alphawallet.app.interact.SetupTokensInteract.EXPIRED_CONTRACT;

public class TokensRealmSource implements TokenLocalSource {

    public static final String TAG = "TLS";
    public static final long ACTUAL_BALANCE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final long ACTUAL_TOKEN_TICKER_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final String COINMARKETCAP_IMAGE_URL = "https://files.coinmarketcap.com/static/img/coins/128x128/%s.png";

    private final RealmManager realmManager;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    public TokensRealmSource(RealmManager realmManager, EthereumNetworkRepositoryType ethereumNetworkRepository) {
        this.realmManager = realmManager;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    @Override
    public Completable saveTokens(NetworkInfo networkInfo, Wallet wallet, Token[] items) {
        return Completable.fromAction(() -> {
            Date now = new Date();
            for (Token token : items) {
                if (token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT) && token.tokenInfo.symbol != null)
                {
                    saveToken(wallet, token, now);
                }
            }
        });
    }

    @Override
    public Single<Token[]> saveERC721Tokens(Wallet wallet, Token[] tokens)
    {
        return Single.fromCallable(() -> {
            Date now = new Date();
            try (Realm realm = realmManager.getERC721RealmInstance(wallet))
            {
                realm.beginTransaction();
                for (Token token : tokens)
                {
                    saveERC721Token(realm, wallet, token, now);
                }
                realm.commitTransaction();
            }
            checkTokenRealm(wallet, tokens);
            return tokens;
        });
    }

    @Override
    public Single<Token[]> saveERC20Tokens(Wallet wallet, Token[] tokens)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                realm.beginTransaction();
                for (Token token : tokens)
                {
                    saveToken(realm, token);
                }
                realm.commitTransaction();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            return tokens;
        });
    }

    @Override
    public void deleteRealmToken(int chainId, Wallet wallet, String address)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            String dbKey = databaseKey(chainId, address);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", dbKey)
                    .findFirst();

            if (realmToken != null)
            {
                realm.beginTransaction();
                realmToken.deleteFromRealm();
                realm.commitTransaction();
            }
        }
    }

    private void checkTokenRealm(Wallet wallet, Token[] tokens)
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
    public Single<Token> saveToken(Wallet wallet, Token token) {
        return Single.fromCallable(() -> {
            Date now = new Date();
            if (token instanceof ERC721Token)
            {
                saveERC721Token(wallet, token, now);
            }
            else
            {
                saveToken(wallet, token, now);
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
                        .equalTo("address", databaseKey(networkInfo.chainId, address))
                        .equalTo("chainId", networkInfo.chainId)
                        .findAll();

                return convertSingle(realmItem);
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Single<Token[]> fetchEnabledTokensWithBalance(Wallet wallet) {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("isEnabled", true)
                        .findAll();

                return convertBalance(realmItems, System.currentTimeMillis(), wallet);
            }
            catch (Exception e)
            {
                return new Token[0]; //ensure fetch completes
            }
        });
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

                return convert(realmItems);
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Single<Token[]> fetchERC721Tokens(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getERC721RealmInstance(wallet))
            {
                RealmResults<RealmERC721Token> realmItems = realm.where(RealmERC721Token.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .findAll();

                return convertERC721(realmItems, realm, wallet);
            }
            catch (Exception e)
            {
                return new Token[0]; // ensure fetch always completes
            }
        });
    }

    @Override
    public Single<Token> saveTicker(NetworkInfo network, Wallet wallet, final Token token) {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                if (!WalletUtils.isValidAddress(wallet.address)) return token;
                TransactionsRealmCache.addRealm();
                realm.beginTransaction();
                long now = System.currentTimeMillis();
                String tickerName = token.getAddress() + "-" + token.tokenInfo.chainId;
                RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                        .equalTo("contract", tickerName)
                        .findFirst();
                if (realmItem == null) {
                    realmItem = realm.createObject(RealmTokenTicker.class, tickerName);
                    realmItem.setCreatedTime(now);
                }
                realmItem.setId(token.ticker.id);
                realmItem.setPercentChange24h(token.ticker.percentChange24h);
                realmItem.setPrice(token.ticker.price);
                realmItem.setImage(TextUtils.isEmpty(token.ticker.image)
                        ? String.format(COINMARKETCAP_IMAGE_URL, token.ticker.id)
                        : token.ticker.image);
                realmItem.setUpdatedTime(now);
                realmItem.setCurrencySymbol(token.ticker.priceSymbol);
                realm.commitTransaction();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return token;
        });
    }

    @Override
    public Single<TokenTicker> fetchTicker(NetworkInfo network, Wallet wallet, Token token) {
        return Single.fromCallable(() -> {
            ArrayList<TokenTicker> tokenTickers = new ArrayList<>();
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                long minCreatedTime = System.currentTimeMillis() - ACTUAL_TOKEN_TICKER_INTERVAL;
                RealmResults<RealmTokenTicker> rawItems = realm.where(RealmTokenTicker.class)
                        .equalTo("contract", token.getAddress() + "-" + token.tokenInfo.chainId)
                        .greaterThan("updatedTime", minCreatedTime)
                        .findAll();
                int len = rawItems.size();
                for (int i = 0; i < len; i++)
                {
                    RealmTokenTicker rawItem = rawItems.get(i);
                    if (rawItem != null)
                    {
                        String currencySymbol = rawItem.getCurrencySymbol();
                        if (currencySymbol == null || currencySymbol.length() == 0) currencySymbol = "USD";
                        tokenTickers.add(new TokenTicker(rawItem.getId(), rawItem.getContract(), rawItem.getPrice(), rawItem.getPercentChange24h(), rawItem.getCurrencySymbol(), rawItem.getImage()));
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return tokenTickers.size() == 0
                    ? new TokenTicker("0", "0", "0", "0", "USD", null)
                    : tokenTickers.get(0);
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

    /**
     * Loading stored token with balance from database
     * @param network
     * @param wallet
     * @param address
     * @return
     */
    @Override
    public Token getTokenBalance(NetworkInfo network, Wallet wallet, String address)
    {
        Token result = null;
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .like("address", address + "*")
                    .equalTo("chainId", network.chainId)
                    .findFirst();

            if (realmToken != null)
            {
                TokenFactory tf = new TokenFactory();
                TokenInfo info = tf.createTokenInfo(realmToken);
                result = tf.createToken(info, realmToken, realmToken.getAddedTime(), network.getShortName());
                result.setTokenWallet(wallet.address);
                result.setupRealmToken(realmToken);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return result;
    }

    @Override
    public Map<Integer, Token> getTokenBalances(Wallet wallet, String address)
    {
        Map<Integer, Token> result = new HashMap<>();
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmResults<RealmToken> realmTokens = realm.where(RealmToken.class)
                    .like("address", address + "*")
                    .findAll();

            if (realmTokens != null)
            {
                for (RealmToken rt : realmTokens)
                {
                    TokenFactory tf = new TokenFactory();
                    TokenInfo info = tf.createTokenInfo(rt);
                    NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
                    Token newToken = tf.createToken(info, rt, rt.getAddedTime(), network.getShortName());//; new Token(info, balance, realmItem.getUpdatedTime());
                    newToken.setTokenWallet(wallet.address);
                    newToken.setupRealmToken(rt);
                    if (address.equals(wallet.address)) newToken.setIsEthereum();
                    result.put(info.chainId, newToken);
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return result;
    }

    private String databaseKey(int chainId, String address)
    {
        return address + "-" + chainId;
    }

    private String databaseKey(Token token)
    {
        return databaseKey(token.tokenInfo.chainId, token.tokenInfo.address);
    }

    @Override
    public void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", databaseKey(token))
                    .equalTo("chainId", network.chainId)
                    .findFirst();

            if (token.hasPositiveBalance() && realmToken == null)
            {
                saveToken(wallet, token, new Date());
            }
            else if (realmToken != null && token.checkRealmBalanceChange(realmToken))
            {
                TransactionsRealmCache.addRealm();
                realm.beginTransaction();
                token.setRealmBalance(realmToken);
                if (token.tokenInfo.name.length() > 0)
                {
                    realmToken.setName(token.tokenInfo.name);
                    realmToken.setSymbol(token.tokenInfo.symbol);
                    realmToken.setDecimals(token.tokenInfo.decimals);
                }
                token.setRealmInterfaceSpec(realmToken);
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
    public Disposable storeBlockRead(Token token, Wallet wallet)
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
                                .equalTo("address", databaseKey(token))
                                .equalTo("chainId", token.tokenInfo.chainId)
                                .findFirst();

                        if (realmToken != null)
                        {
                            TransactionsRealmCache.addRealm();
                            realm.beginTransaction();
                            token.setRealmLastBlock(realmToken);
                        }
                        else
                        {
                            saveToken(wallet, token, new Date());
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm.isInTransaction()) realm.commitTransaction();
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

    private void saveToken(Realm realm, Token token) throws Exception
    {
        String databaseKey = databaseKey(token);

        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", databaseKey)
                .equalTo("chainId", token.tokenInfo.chainId)
                .findFirst();
        if (realmToken == null)
        {
            Log.d(TAG, "Save New Token: " + token.getFullName() + " :" + token.tokenInfo.address);

            realmToken = realm.createObject(RealmToken.class, databaseKey);
            realmToken.setName(token.tokenInfo.name);
            realmToken.setSymbol(token.tokenInfo.symbol);
            realmToken.setDecimals(token.tokenInfo.decimals);
            realmToken.setAddedTime(token.updateBlancaTime);
            token.setRealmBalance(realmToken);
            token.setRealmInterfaceSpec(realmToken);
            token.setRealmLastBlock(realmToken);
            realmToken.setEnabled(true);
            realmToken.setChainId(token.tokenInfo.chainId);
        }
        else
        {
            Log.d(TAG, "Update Token: " + token.getFullName());
            if (token.checkRealmBalanceChange(realmToken))
            {
                //has token changed?
                realmToken.setAddedTime(token.updateBlancaTime);
                token.setRealmInterfaceSpec(realmToken);
                realmToken.setEnabled(true);
                token.setRealmBalance(realmToken);
            }
        }
    }

    private void saveToken(Wallet wallet, Token token, Date currentTime) {
        if (!WalletUtils.isValidAddress(wallet.address)) return;
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            TransactionsRealmCache.addRealm();
            realm.beginTransaction();
            saveToken(realm, token);
            realm.commitTransaction();
            TransactionsRealmCache.subRealm();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Store single ERC721 token
     * @param wallet
     * @param token
     * @param now
     */
    private void saveERC721Token(Wallet wallet, Token token, Date now)
    {
        if (!WalletUtils.isValidAddress(wallet.address)) return;
        try (Realm realm = realmManager.getERC721RealmInstance(wallet))
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

    private void saveERC721Token(Realm realm, Wallet wallet, Token token, Date currentTime) throws RealmException
    {
        ERC721Token e;
        if (token instanceof ERC721Token)
        {
            e = (ERC721Token) token;
        }
        else
        {
            return; //no storage here
        }

        if (!WalletUtils.isValidAddress(wallet.address)) return;

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

        deleteRealmToken(token.tokenInfo.chainId, wallet, address); //in case it was recorded as normal token
        String dbKey = databaseKey(token);

        RealmERC721Token realmToken = realm.where(RealmERC721Token.class)
                .equalTo("address", dbKey)
                .findFirst();

        if (realmToken == null)
        {
            //create new storage
            Log.d(TAG, "Save New ERC721 Token: " + token.tokenInfo.name + " :" + address);
            realmToken = realm.createObject(RealmERC721Token.class, dbKey);
            realmToken.setName(contractName);
            realmToken.setSymbol(contractSymbol);
            realmToken.setAddedTime(currentTime.getTime());
            realmToken.setSchemaName(schemaName);
            realmToken.setTokenIdList(e.tokenBalance);
            realmToken.setChainId(token.tokenInfo.chainId);
            realmToken.setContractType(token.getInterfaceSpec().ordinal());
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
                deleteAssets(realm, dbKey);

                //update balance
                realmToken.setAddedTime(currentTime.getTime());
                realmToken.setTokenIdList(e.tokenBalance);
            }
        }

        //now create the assets inside this
        for (Asset asset : e.tokenBalance)
        {
            RealmERC721Asset realmAsset = realm.where(RealmERC721Asset.class)
                    .equalTo("tokenIdAddr", RealmERC721Asset.tokenIdAddrName(asset.getTokenId(), dbKey))
                    .findFirst();

            if (realmAsset == null)
            {
                realmAsset = realm.createObject(RealmERC721Asset.class,
                                                RealmERC721Asset.tokenIdAddrName(asset.getTokenId(), dbKey));

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

    private void deleteAssets(Realm realm, String dbKey)
    {
        String key = dbKey + "-";

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

    private Token[] convertERC721(RealmResults<RealmERC721Token> realmItems, Realm realm, Wallet wallet) {
        int len = realmItems.size();
        Token[] result = new Token[len];
        for (int i = 0; i < len; i++)
        {
            TokenFactory tf = new TokenFactory();
            RealmERC721Token realmItem = realmItems.get(i);
            if (realmItem.isTokenId()) continue;
            if (realmItem != null)
            {
                //get all the assets for this ERC first
                List<String> tokenIdAddrs = realmItem.getTokenIdList();
                List<Asset> assets = getERC721Assets(tokenIdAddrs, realm, realmItem);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(realmItem.getChainId());
                result[i] = tf.createERC721Token(realmItem, assets, realmItem.getUpdatedTime(), network.getShortName());
                result[i].setTokenWallet(wallet.address);
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

    private Token[] convert(RealmResults<RealmToken> realmItems) {
        int len = realmItems.size();
        TokenFactory tf = new TokenFactory();
        Token[] result = new Token[len];
        for (int i = 0; i < len; i++) {
            RealmToken realmItem = realmItems.get(i);
            if (realmItem != null) {
                TokenInfo info = tf.createTokenInfo(realmItem);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
                result[i] = tf.createToken(info, realmItem, realmItem.getAddedTime(), network.getShortName());//; new Token(info, balance, realmItem.getUpdatedTime());
            }
        }
        return result;
    }

    private Token[] convertBalance(RealmResults<RealmToken> realmItems, long now, Wallet wallet) {
        TokenFactory tf = new TokenFactory();
        List<Token> tokenList = new ArrayList<>();
        for (RealmToken realmItem : realmItems)
        {
            if (realmItem != null)
            {
                TokenInfo info = tf.createTokenInfo(realmItem);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
                Token token = tf.createToken(info, realmItem, now, network.getShortName());//; new Token(info, balance, realmItem.getUpdatedTime());
                if (token != null)
                {
                    if (info.address.equalsIgnoreCase(wallet.address) && realmItem.getInterfaceSpec() != ContractType.ETHEREUM.ordinal())
                    {
                        token.setInterfaceSpec(ContractType.ETHEREUM);
                    }
                    token.setTokenWallet(wallet.address);
                    token.setupRealmToken(realmItem);
                    tokenList.add(token);
                }
            }
        }

        return tokenList.toArray(new Token[0]);
    }

    private Token convertSingle(RealmResults<RealmToken> realmItems) {
        if (realmItems.size() == 0) return null;
        TokenFactory tf = new TokenFactory();
        Token result = null;
        RealmToken realmItem = realmItems.get(0);
            if (realmItem != null) {
                TokenInfo info = tf.createTokenInfo(realmItem);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
                result = tf.createToken(info, realmItem, realmItem.getAddedTime(), network.getShortName());
                result.lastBlockCheck = realmItem.getLastBlock();
                result.lastTxCheck = realmItem.getUpdatedTime();
            }
        return result;
    }

    private void createBlankToken(Realm realm, Token token)
    {
        RealmToken realmToken = realm.createObject(RealmToken.class, databaseKey(token));
        realmToken.setName(token.tokenInfo.name);
        realmToken.setSymbol(token.tokenInfo.symbol);
        realmToken.setDecimals(0);
        realmToken.setAddedTime(-1);
        realmToken.setAddedTime(-1);
        realmToken.setEnabled(false);
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
                        if (!WalletUtils.isValidAddress(wallet.address)) return;
                        RealmToken realmToken = realm.where(RealmToken.class)
                                .equalTo("address", databaseKey(token))
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
                        if (realm.isInTransaction()) realm.commitTransaction();
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
