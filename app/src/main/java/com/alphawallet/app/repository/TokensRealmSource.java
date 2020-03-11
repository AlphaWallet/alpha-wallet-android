package com.alphawallet.app.repository;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.opensea.Trait;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.entity.RealmERC721Asset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.RealmManager;

import org.web3j.crypto.WalletUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmException;

import static com.alphawallet.app.interact.SetupTokensInteract.EXPIRED_CONTRACT;

public class TokensRealmSource implements TokenLocalSource {

    public static final String TAG = "TLS";
    public static final long ACTUAL_BALANCE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final long ACTUAL_TOKEN_TICKER_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

    private final RealmManager realmManager;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    public TokensRealmSource(RealmManager realmManager, EthereumNetworkRepositoryType ethereumNetworkRepository) {
        this.realmManager = realmManager;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    @Override
    public Single<Token[]> saveTokens(Wallet wallet, Token[] items)
    {
        return Single.fromCallable(() -> {
            for (Token token : items) {
                if (token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT) && token.tokenInfo.symbol != null)
                {
                    saveTokenLocal(wallet, token);
                }
            }
            return items;
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

    @Override
    public void updateTokenType(Token token, Wallet wallet, ContractType type)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", dbKey)
                    .findFirst();

            if (realmToken == null)
            {
                saveToken(wallet, token, new Date());
            }
            else
            {
                realm.beginTransaction();
                realmToken.setInterfaceSpec(type.ordinal());
                realm.commitTransaction();
            }
        }
    }

    @Override
    public Single<Token> saveToken(Wallet wallet, Token token)
    {
        return Single.fromCallable(() -> {
            saveTokenLocal(wallet, token);
            return token;
        });
    }

    private void saveTokenLocal(Wallet wallet, Token token)
    {
        Date now = new Date();
        switch (token.getInterfaceSpec())
        {
            case ETHEREUM:
            case ERC20:
            case ERC875_LEGACY:
            case ERC875:
            case CURRENCY:
            case ERC721_TICKET:
            case ERC721:
            case ERC721_LEGACY:
                saveToken(wallet, token, now);
                break;
            //No save
            case NOT_SET:
            case OTHER:
            case CREATION:
                break;
            default:
                System.out.println("Unknown Token Contract");
                break;
        }
    }

    @Override
    public Single<Token> fetchEnabledToken(NetworkInfo networkInfo, Wallet wallet, String address) {
        return Single.fromCallable(() -> {
            Realm realm = null;

            try {
                realm = realmManager.getRealmInstance(wallet);
                RealmToken realmItem = realm.where(RealmToken.class)
                        .equalTo("address", databaseKey(networkInfo.chainId, address))
                        .equalTo("chainId", networkInfo.chainId)
                        .findFirst();

                return convertSingle(realmItem, realm, null, wallet);
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
        });
    }

    @Override
    public Single<Token[]> fetchTokensWithBalance(Wallet wallet) {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .findAll();

                return convertMulti(realmItems, System.currentTimeMillis(), wallet, realm);
            }
            catch (Exception e)
            {
                return new Token[0]; //ensure fetch completes
            }
        }).flatMap(tokens -> attachTickers(tokens, wallet));
    }

    @Override
    public Single<Token> saveTicker(Wallet wallet, final Token token) {
        return Single.fromCallable(() -> {
            if (!WalletUtils.isValidAddress(wallet.address)
                    || token.ticker == null) return token;

            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                TransactionsRealmCache.addRealm();
                realm.beginTransaction();
                writeTickerToRealm(realm, token);
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
    public Single<Token[]> saveTickers(Wallet wallet, Token[] tokens)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                TransactionsRealmCache.addRealm();
                realm.beginTransaction();
                for (Token token : tokens)
                {
                    if (!WalletUtils.isValidAddress(wallet.address)
                            || token.ticker == null) continue;
                    writeTickerToRealm(realm, token);
                }
                realm.commitTransaction();
                TransactionsRealmCache.subRealm();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            return tokens;
        });
    }

    private void writeTickerToRealm(Realm realm, final Token token)
    {
        if (token.ticker == null) return;
        String tickerName = databaseKey(token);
        RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                .equalTo("contract", tickerName)
                .findFirst();
        if (realmItem == null) {
            realmItem = realm.createObject(RealmTokenTicker.class, tickerName);
            realmItem.setCreatedTime(token.ticker.updateTime);
        }
        realmItem.setPercentChange24h(token.ticker.percentChange24h);
        realmItem.setPrice(token.ticker.price);
        realmItem.setImage(TextUtils.isEmpty(token.ticker.image)
                           ? ""
                           : token.ticker.image);
        realmItem.setUpdatedTime(token.ticker.updateTime);
        realmItem.setCurrencySymbol(token.ticker.priceSymbol);
    }

    private Single<Token[]> attachTickers(Token[] tokens, Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                for (Token t : tokens)
                {
                    RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                            .equalTo("contract", databaseKey(t))
                            .findFirst();
                    t.ticker = convertRealmTicker(realmItem);
                }
            }
            return tokens;
        });
    }

    @Override
    public Single<TokenTicker> fetchTicker(Wallet wallet, Token token) {
        return Single.fromCallable(() -> {
            TokenTicker tokenTicker = null;
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmTokenTicker rawItem = realm.where(RealmTokenTicker.class)
                        .equalTo("contract", token.getAddress() + "-" + token.tokenInfo.chainId)
                        .findFirst();

                tokenTicker = convertRealmTicker(rawItem);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return tokenTicker == null
                    ? new TokenTicker()
                    : tokenTicker;
        });
    }

    private TokenTicker convertRealmTicker(RealmTokenTicker rawItem)
    {
        TokenTicker tokenTicker = null;
        if (rawItem != null)
        {
            long minCreatedTime = System.currentTimeMillis() - ACTUAL_TOKEN_TICKER_INTERVAL;
            String currencySymbol = rawItem.getCurrencySymbol();
            if (currencySymbol == null || currencySymbol.length() == 0)
                currencySymbol = "USD";
            tokenTicker = new TokenTicker(rawItem.getPrice(), rawItem.getPercentChange24h(), currencySymbol, rawItem.getImage(), rawItem.getUpdatedTime());
        }

        return tokenTicker;
    }

    @Override
    public void setEnable(NetworkInfo network, Wallet wallet, Token token, boolean isEnabled) {
        Realm realm = null;
        try {
            token.tokenInfo.isEnabled = isEnabled;
            realm = realmManager.getRealmInstance(wallet);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", databaseKey(token))
                    .equalTo("chainId", token.tokenInfo.chainId)
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
                result = tf.createToken(info, realmToken, realmToken.getUpdateTime(), network.getShortName());
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
                    .like("address", address.toLowerCase() + "*")
                    .findAll();

            if (realmTokens != null)
            {
                for (RealmToken rt : realmTokens)
                {
                    TokenFactory tf = new TokenFactory();
                    TokenInfo info = tf.createTokenInfo(rt);
                    NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
                    Token newToken = tf.createToken(info, rt, rt.getUpdateTime(), network.getShortName());//; new Token(info, balance, realmItem.getUpdatedTime());
                    newToken.setTokenWallet(wallet.address);
                    if (address.equals(wallet.address)) newToken.setIsEthereum();

                    RealmTokenTicker rawItem = realm.where(RealmTokenTicker.class)
                            .equalTo("contract", newToken.getAddress() + "-" + newToken.tokenInfo.chainId)
                            .findFirst();

                    newToken.ticker = ethereumNetworkRepository.updateTicker(newToken, convertRealmTicker(rawItem));
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
            realmToken.setUpdateTime(token.updateBlancaTime);
            token.setRealmBalance(realmToken);
            token.setRealmInterfaceSpec(realmToken);
            token.setRealmLastBlock(realmToken);
            realmToken.setEnabled(token.tokenInfo.isEnabled);
            realmToken.setChainId(token.tokenInfo.chainId);
            if (token.ticker != null)
            {
                writeTickerToRealm(realm, token);
            }
            if (token.isERC721())
            {
                saveERC721Assets(realm, token);
            }
        }
        else
        {
            Log.d(TAG, "Update Token: " + token.getFullName());
            if (token.checkRealmBalanceChange(realmToken))
            {
                //has token changed?
                realmToken.setName(token.tokenInfo.name);
                realmToken.setSymbol(token.tokenInfo.symbol);
                realmToken.setUpdateTime(token.updateBlancaTime);
                token.setRealmInterfaceSpec(realmToken);
                token.setRealmBalance(realmToken);
                token.setRealmLastBlock(realmToken);
                if (token.ticker != null)
                {
                    writeTickerToRealm(realm, token);
                }
                if (token.isERC721())
                {
                    saveERC721Assets(realm, token);
                }
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

    private void saveERC721Assets(Realm realm, Token token) throws RealmException
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

        String dbKey = databaseKey(token);

        deleteAssets(realm, dbKey);

        //now create the assets inside this
        for (Asset asset : e.getTokenAssets())
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

    private List<Asset> getERC721Assets(List<String> keys, Realm realm, Token token)
    {
        List<Asset> assets = new ArrayList<>();
        AssetContract contract = new AssetContract(token.tokenInfo.address, token.tokenInfo.name,
                                                   token.tokenInfo.symbol, token.getInterfaceSpec().toString());

        for (String key : keys)
        {
            String dbKey = databaseKey(token.tokenInfo.chainId, contract.getAddress());

            RealmERC721Asset realmAsset = realm.where(RealmERC721Asset.class)
                    .equalTo("tokenIdAddr", RealmERC721Asset.tokenIdAddrName(key, dbKey))
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

    private Token[] convertMulti(RealmResults<RealmToken> realmItems, long now, Wallet wallet, Realm realm)
    {
        TokenFactory tf        = new TokenFactory();
        List<Token>  tokenList = new ArrayList<>();
        for (RealmToken realmItem : realmItems)
        {
            Token t = convertSingle(realmItem, realm, tf, wallet);

            if (t != null)
            {
                tokenList.add(t);
                if (!t.isTerminated() && !t.isBad()) t.balanceUpdateWeight = 2.0f; //on wallet startup give all tokens a high priority so they all refresh then settle down
            }
        }

        return tokenList.toArray(new Token[0]);
    }

    private Token convertSingle(RealmToken realmItem, Realm realm, TokenFactory tf, Wallet wallet)
    {
        if (realmItem == null) return null;
        if (tf == null) tf   = new TokenFactory();
        Token        result  = null;
        TokenInfo    info    = tf.createTokenInfo(realmItem);
        NetworkInfo  network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
        result = tf.createToken(info, realmItem, realmItem.getUpdateTime(), network.getShortName());
        result.setTokenWallet(wallet.address);
        RealmTokenTicker rawItem = realm.where(RealmTokenTicker.class)
                .equalTo("contract", result.getAddress() + "-" + result.tokenInfo.chainId)
                .findFirst();

        result.ticker = convertRealmTicker(rawItem);

        if (result.isERC721()) //add erc721 assets
        {
            List<String> tokenIdAddrs = realmItem.getTokenIdList();
            List<Asset>  assets       = getERC721Assets(tokenIdAddrs, realm, result);
            for (Asset asset : assets)
                result.addAssetToTokenBalanceAssets(asset);
        }
        return result;
    }

    private void createBlankToken(Realm realm, Token token)
    {
        RealmToken realmToken = realm.createObject(RealmToken.class, databaseKey(token));
        realmToken.setName(token.tokenInfo.name);
        realmToken.setSymbol(token.tokenInfo.symbol);
        realmToken.setDecimals(0);
        realmToken.setUpdateTime(-1);
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
