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
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmERC721Asset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.util.Utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmException;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.app.service.TickerService.TICKER_TIMEOUT;
import static com.alphawallet.app.service.TokensService.EXPIRED_CONTRACT;

public class TokensRealmSource implements TokenLocalSource {

    public static final String TAG = "TLS";
    public static final String IMAGES_DB = "image_urls_db";
    public static final String TICKER_DB = "tickers_db";
    public static final long ACTUAL_BALANCE_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    public static final String ADDRESS_FORMAT = "0x????????????????????????????????????????-*";
    private static final long ACTUAL_TOKEN_TICKER_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

    public static final String EVENT_CARDS = "-eventName";

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
                if (token.tokenInfo != null && token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT) && token.tokenInfo.symbol != null)
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
    public Token updateTokenType(Token token, Wallet wallet, ContractType type)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address);
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", dbKey, Case.INSENSITIVE)
                    .findFirst();

            if (realmToken == null)
            {
                saveToken(wallet, token, new Date());
            }
            else
            {
                realm.executeTransaction(instance -> {
                    realmToken.setInterfaceSpec(type.ordinal());
                    realmToken.setName(token.tokenInfo.name);
                    realmToken.setSymbol(token.tokenInfo.symbol);
                });
            }

            return fetchToken(token.tokenInfo.chainId, wallet, token.getAddress());
        }
    }

    @Override
    public Realm getRealmInstance(Wallet wallet)
    {
        return realmManager.getRealmInstance(wallet);
    }

    @Override
    public Realm getTickerRealmInstance()
    {
        return realmManager.getRealmInstance(TICKER_DB);
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
            case DYNAMIC_CONTRACT:
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
    public Token fetchToken(int chainId, Wallet wallet, String address)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmItem = realm.where(RealmToken.class)
                    .equalTo("address", databaseKey(chainId, address))
                    .equalTo("chainId", chainId)
                    .findFirst();

            Token t = convertSingle(realmItem, realm, null, wallet);
            if (t == null && address.equalsIgnoreCase(wallet.address))
            {
                NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
                if (info == null) return t;
                t = createCurrencyToken(info, wallet);
                realm.beginTransaction();
                saveToken(realm, t);
                realm.commitTransaction();
                return t;
            }
            else
            {
                return t;
            }
        }
    }

    private TokenTicker convertRealmTicker(RealmTokenTicker rawItem)
    {
        TokenTicker tokenTicker = null;
        if (rawItem != null)
        {
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
            }
        }
    }

    @Override
    public boolean getEnabled(Token token)
    {
        boolean isEnabled = false;
        try (Realm realm = realmManager.getRealmInstance(new Wallet(token.getWallet())))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", databaseKey(token))
                    .equalTo("chainId", token.tokenInfo.chainId)
                    .findFirst();

            if (realmToken != null)
            {
                isEnabled = realmToken.isEnabled();
            }
        }

        return isEnabled;
    }

    @Override
    public boolean hasVisibilityBeenChanged(Token token)
    {
        boolean hasBeenChanged = false;
        try (Realm realm = realmManager.getRealmInstance(new Wallet(token.getWallet().toLowerCase())))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", databaseKey(token))
                    .equalTo("chainId", token.tokenInfo.chainId)
                    .findFirst();

            if (realmToken != null)
            {
                hasBeenChanged = realmToken.isVisibilityChanged();
            }
        }

        return hasBeenChanged;
    }

    @Override
    public void setVisibilityChanged(Wallet wallet, Token token)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransactionAsync(r -> {
                RealmToken realmToken = r.where(RealmToken.class)
                        .equalTo("address", databaseKey(token))
                        .equalTo("chainId", token.tokenInfo.chainId)
                        .findFirst();

                if (realmToken != null)
                {
                    realmToken.setVisibilityChanged(true);
                }
            });
        }
        catch (Exception ex)
        {
            //
        }
    }

    public static String databaseKey(int chainId, String address)
    {
        return address.toLowerCase() + "-" + chainId;
    }

    public static String databaseKey(Token token)
    {
        return databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase());
    }

    public static String eventActivityKey(String txHash, String activityName)
    {
        return txHash + "-" + activityName + EVENT_CARDS;
    }

    public static String eventActivityKey(String txHash, String activityName, int extendedId)
    {
        return txHash + "-" + activityName + EVENT_CARDS + "-" + extendedId;
    }

    public static String eventBlockKey(int chainId, String eventAddress, String namedType, String filter)
    {
        return eventAddress.toLowerCase() + "-" + chainId + "-" + namedType + "-" + filter + "-eventBlock";
    }

    @Override
    public void markBalanceChecked(Wallet wallet, int chainId, String tokenAddress)
    {
        if (tokenAddress == null) tokenAddress = wallet.address; //base chain update
        String key = databaseKey(chainId, tokenAddress.toLowerCase());
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransactionAsync(instance -> {
                RealmToken realmToken = instance.where(RealmToken.class)
                        .equalTo("address", key)
                        .equalTo("chainId", chainId)
                        .findFirst();

                if (realmToken != null)
                {
                    realmToken.setUpdateTime(System.currentTimeMillis());
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    @Override
    public boolean updateTokenBalance(Wallet wallet, int chainId, String tokenAddress, BigDecimal balance, List<BigInteger> balanceArray, ContractType type)
    {
        boolean balanceChanged = false;
        if (tokenAddress == null) tokenAddress = wallet.address; //base chain update
        String key = databaseKey(chainId, tokenAddress);
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", key)
                    .equalTo("chainId", chainId)
                    .findFirst();

            if (realmToken != null)
            {
                String currentBalance = realmToken.getBalance();
                String newBalance = balance.toString();
                if (balanceArray != null) newBalance = Utils.bigIntListToString(balanceArray, true);

                if (type == ContractType.ERC721 || type == ContractType.ERC721_LEGACY)
                {
                    //only used for determining if balance is now zero
                    if (balance.equals(BigDecimal.ZERO) && !realmToken.getBalance().equals("0"))
                    {
                        realm.beginTransaction();
                        realmToken.setBalance("0");
                        realmToken.setUpdateTime(System.currentTimeMillis());
                        deleteAssets(realm, key);
                        Log.d(TAG, "Zero out ERC721 balance: " + realmToken.getName() + " :" + tokenAddress);
                        balanceChanged = true;
                    }
                }
                else if (!newBalance.equals(currentBalance))
                {
                    realm.beginTransaction();
                    //updating balance
                    realmToken.setBalance(newBalance);
                    realmToken.setUpdateTime(System.currentTimeMillis());
                    Log.d(TAG, "Update Token Balance: " + realmToken.getName() + " :" + tokenAddress);
                    balanceChanged = true;
                }

                if (realm.isInTransaction())
                {
                    realm.commitTransaction();
                    realm.close();
                }
            }
        }
        catch (Exception e)
        {
            //
        }

        return balanceChanged;
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

            if (realmToken == null && (token.hasPositiveBalance() || token.isEthereum()))
            {
                saveToken(wallet, token, new Date());
            }
            else if (realmToken != null && token.checkRealmBalanceChange(realmToken))
            {
                realm.beginTransaction();
                token.setRealmBalance(realmToken);
                realmToken.updateTokenInfoIfRequired(token.tokenInfo);
                token.setRealmInterfaceSpec(realmToken);
                realm.commitTransaction();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public Disposable storeTokenUrl(int networkId, String address, String imageUrl)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;
                    @Override
                    public void onStart()
                    {
                        String instanceKey = address.toLowerCase() + "-" + networkId;
                        realm = realmManager.getRealmInstance(IMAGES_DB);
                        RealmAuxData instance = realm.where(RealmAuxData.class)
                                .equalTo("instanceKey", instanceKey)
                                .findFirst();

                        realm.beginTransaction();
                        if (instance == null)
                        {
                            instance = realm.createObject(RealmAuxData.class, instanceKey);
                        }

                        instance.setResult(imageUrl);
                        instance.setResultTime(System.currentTimeMillis());
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm.isInTransaction()) realm.commitTransaction();
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

    private void saveToken(Realm realm, Token token) throws RealmException
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
            if (token.isERC721())
            {
                saveERC721Assets(realm, token);
            }
        }
        else
        {
            Log.d(TAG, "Update Token: " + token.getFullName());
            realmToken.updateTokenInfoIfRequired(token.tokenInfo);

            if (token.checkRealmBalanceChange(realmToken))
            {
                //has token changed?
                realmToken.setUpdateTime(token.updateBlancaTime);
                token.setRealmInterfaceSpec(realmToken);
                token.setRealmBalance(realmToken);
                token.setRealmLastBlock(realmToken);
                if (token.isERC721())
                {
                    saveERC721Assets(realm, token);
                }
            }
        }

        //Final check to see if the token should be visible
        if (token.getBalanceRaw().compareTo(BigDecimal.ZERO) > 0 && !realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            token.tokenInfo.isEnabled = true;
            realmToken.setEnabled(true);
        }
    }

    private void saveToken(Wallet wallet, Token token, Date currentTime) {
        if (!Utils.isAddressValid(wallet.address)) return;
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransaction(instance -> {
                saveToken(realm, token);
            });
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
        for (Asset asset : e.getTokenAssets().values())
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

    private void deleteAssets(Realm realm, String dbKey) throws RealmException
    {
        String key = dbKey + "-";

        RealmResults<RealmERC721Asset> realmAssets = realm.where(RealmERC721Asset.class)
                .beginsWith("tokenIdAddr", key)
                .findAll();

        for (RealmERC721Asset asset : realmAssets)
        {
            asset.deleteFromRealm();
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
        AssetContract contract = new AssetContract(token);

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

    public TokenCardMeta[] fetchTokenMetasForUpdate(Wallet wallet, List<Integer> networkFilters)
    {
        List<TokenCardMeta> tokenMetas = new ArrayList<>();
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                    .sort("addedTime", Sort.ASCENDING)
                    .beginGroup().equalTo("isEnabled", true).or().like("address", wallet.address.toLowerCase() + "*").endGroup()
                    .like("address", ADDRESS_FORMAT)
                    .findAll();

            for (RealmToken t : realmItems)
            {
                if (networkFilters.size() > 0 && !networkFilters.contains(t.getChainId()) ||
                        (t.getContractType() != ContractType.ETHEREUM && !t.getEnabled())) continue;

                TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(),
                        convertStringBalance(t.getBalance(), t.getContractType()), t.getUpdateTime(),
                        null, t.getName(), t.getSymbol(), t.getContractType());
                meta.lastTxUpdate = t.getLastTxTime();

                tokenMetas.add(meta);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return tokenMetas.toArray(new TokenCardMeta[0]);
    }

    /**
     * Fetches all enabled TokenMetas in database, adding in chain tokens if required
     * @param wallet
     * @param networkFilters
     * @param svs
     * @return
     */
    public Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Integer> networkFilters, AssetDefinitionService svs)
    {
        List<TokenCardMeta> tokenMetas = new ArrayList<>();
        return Single.fromCallable(() -> {
            //ensure root tokens for filters are in there
            List<Integer> rootChainTokenCards = new ArrayList<>(networkFilters);
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .equalTo("isEnabled", true)
                        .like("address", ADDRESS_FORMAT)
                        .findAll();

                for (RealmToken t : realmItems)
                {
                    if (networkFilters.size() > 0 && !networkFilters.contains(t.getChainId()) || !t.getEnabled()) continue;
                    String balance = convertStringBalance(t.getBalance(), t.getContractType());

                    if (t.getContractType() == ContractType.ETHEREUM && !(t.getTokenAddress().equalsIgnoreCase(wallet.address) || t.getTokenAddress().equals("eth")))
                    {
                        continue;
                    }

                    TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance, t.getUpdateTime(), svs, t.getName(), t.getSymbol(), t.getContractType());
                    meta.lastTxUpdate = t.getLastTxTime();
                    tokenMetas.add(meta);

                    if (t.getContractType() == ContractType.ETHEREUM && rootChainTokenCards.contains(t.getChainId()))
                    {
                        rootChainTokenCards.remove((Integer)t.getChainId());
                    }
                }

                removeLocalTickers(realm); //delete any local tickers, these have all moved into a single realm
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                //create metas for any card not previously saved
                for (Integer chainId : rootChainTokenCards)
                {
                    TokenCardMeta meta = new TokenCardMeta(chainId, wallet.address.toLowerCase(), "0", 0, svs, "", "", ContractType.ETHEREUM);
                    meta.lastTxUpdate = 0;
                    tokenMetas.add(meta);
                }
            }

            return tokenMetas.toArray(new TokenCardMeta[0]);
        });
    }

    private void removeLocalTickers(Realm realm)
    {
        try
        {
            realm.executeTransactionAsync(r -> {
                RealmResults<RealmTokenTicker> realmItems = r.where(RealmTokenTicker.class)
                        .findAll();
                if (realmItems.size() > 0)
                {
                    realmItems.deleteAllFromRealm();
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    /**
     * Resolves all the token names into the unused 'auxdata' column. These will be used later for filtering
     * TODO: perform this action when tokens are written and when new scripts are detected, not every time we start the add/hide
     * @param wallet
     * @param svs
     * @return
     */
    @Override
    public Single<Integer> fixFullNames(Wallet wallet, AssetDefinitionService svs) {
        return Single.fromCallable(() -> {
            int updated = 0;
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .like("address", ADDRESS_FORMAT)
                        .findAll();

                realm.beginTransaction();
                for (RealmToken t : realmItems)
                {
                    String svsName = svs.getTokenName(t.getChainId(), t.getTokenAddress(), 1);
                    final String fullName = svsName != null ? svsName : t.getName();

                    if (!fullName.equals(t.getAuxData()))
                    {
                        t.setAuxData(fullName);
                        updated++;
                    }
                }

                if (updated > 0)
                {
                    realm.commitTransaction();
                }
                else
                {
                    realm.cancelTransaction();
                }
            }

            return updated;
        });
    }

    /**
     * Fetches all TokenMeta currently in the database with search term, without fixing chain tokens if missing
     * @param wallet
     * @param networkFilters
     * @return
     */
    @Override
    public Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Integer> networkFilters, String searchTerm) {
        List<TokenCardMeta> tokenMetas = new ArrayList<>();
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .like("auxData", "*" + searchTerm + "*", Case.INSENSITIVE)
                        .like("address", ADDRESS_FORMAT)
                        .findAll();

                for (RealmToken t : realmItems)
                {
                    if (networkFilters.size() > 0 && !networkFilters.contains(t.getChainId()) || t.getContractType() == ContractType.ETHEREUM) continue;
                    String balance = convertStringBalance(t.getBalance(), t.getContractType());
                    TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance, t.getUpdateTime(), null, t.getAuxData(), t.getSymbol(), t.getContractType());
                    meta.lastTxUpdate = t.getLastTxTime();
                    tokenMetas.add(meta);
                }
            }

            return tokenMetas.toArray(new TokenCardMeta[0]);
        });
    }

    @Override
    public Disposable updateEthTickers(Map<Integer, TokenTicker> ethTickers)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;

                    @Override
                    public void onStart()
                    {
                        realm = realmManager.getRealmInstance(TICKER_DB);
                        realm.beginTransaction();
                        for (int chainId : ethTickers.keySet())
                        {
                            writeTickerToRealm(realm, ethTickers.get(chainId), chainId, "eth");
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm.isInTransaction()) realm.commitTransaction();
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

    @Override
    public Disposable updateERC20Tickers(Map<String, TokenTicker> erc20Tickers)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;

                    @Override
                    public void onStart()
                    {
                        realm = realmManager.getRealmInstance(TICKER_DB);
                        realm.beginTransaction();
                        for (String tokenAddress : erc20Tickers.keySet())
                        {
                            writeTickerToRealm(realm, erc20Tickers.get(tokenAddress), MAINNET_ID, tokenAddress);
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm.isInTransaction()) realm.commitTransaction();
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

    @Override
    public TokenTicker getCurrentTicker(Token token)
    {
        TokenTicker tt = null;
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            String key = databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase());
            RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                    .equalTo("contract", key)
                    .findFirst();

            if (realmItem != null)
            {
                tt = convertRealmTicker(realmItem);
            }
        }

        return tt;
    }

    @Override
    public Disposable removeOutdatedTickers()
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;

                    @Override
                    public void onStart()
                    {
                        realm = realmManager.getRealmInstance(TICKER_DB);
                        realm.beginTransaction();
                        //get all tickers
                        RealmResults<RealmTokenTicker> realmItems = realm.where(RealmTokenTicker.class)
                                .lessThan("updatedTime", System.currentTimeMillis() - TICKER_TIMEOUT)
                                .findAll();

                        for (RealmTokenTicker data : realmItems)
                        {
                            data.deleteFromRealm();
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm.isInTransaction()) realm.commitTransaction();
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

    private void writeTickerToRealm(Realm realm, final TokenTicker ticker, int chainId, String tokenAddress)
    {
        if (ticker == null) return;
        String databaseKey = databaseKey(chainId, tokenAddress.toLowerCase());
        RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                .equalTo("contract", databaseKey)
                .findFirst();

        if (realmItem == null)
        {
            realmItem = realm.createObject(RealmTokenTicker.class, databaseKey);
            realmItem.setCreatedTime(ticker.updateTime);
        }
        else
        {
            //compare old ticker to see if we need an update
            if (realmItem.getCurrencySymbol().equals(ticker.priceSymbol) && realmItem.getPrice().equals(ticker.price)
                && realmItem.getPercentChange24h().equals(ticker.percentChange24h))
            {
                //no update
                return;
            }
        }

        realmItem.setPercentChange24h(ticker.percentChange24h);
        realmItem.setPrice(ticker.price);
        realmItem.setImage(TextUtils.isEmpty(ticker.image)
                ? ""
                : ticker.image);
        realmItem.setUpdatedTime(ticker.updateTime);
        realmItem.setCurrencySymbol(ticker.priceSymbol);
    }

    public static String convertStringBalance(String balance, ContractType type)
    {
        if (TextUtils.isEmpty(balance) || balance.equals("0"))
        {
            return "0";
        }
        else
        {
            switch (type)
            {
                case NOT_SET:
                case ETHEREUM:
                case ERC20:
                case OTHER:
                case CURRENCY:
                case DELETED_ACCOUNT:
                case CREATION:
                default:
                    return balance;

                case ERC721_TICKET:
                case ERC875_LEGACY:
                case ERC875:
                case ERC721_UNDETERMINED:
                case ERC721:
                case ERC721_LEGACY:
                    return zeroOrBalance(balance);
            }
        }
    }

    private static String zeroOrBalance(String balance)
    {
        String[] ids = balance.split(",");

        for (String id : ids)
        {
            //remove whitespace
            String trim = id.trim();
            if (!trim.equals("0")) return balance;
        }

        return "0";
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
            }
        }

        return tokenList.toArray(new Token[0]);
    }

    private Token convertSingle(RealmToken realmItem, Realm realm, TokenFactory tf, Wallet wallet)
    {
        if (realmItem == null) return null;
        if (tf == null) tf   = new TokenFactory();
        TokenInfo    info    = tf.createTokenInfo(realmItem);
        NetworkInfo  network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
        if (network == null) return null;
        Token result = tf.createToken(info, realmItem, realmItem.getUpdateTime(), network.getShortName());
        result.setTokenWallet(wallet.address);

        if (result.isERC721()) //add erc721 assets
        {
            List<String> tokenIdAddrs = realmItem.getTokenIdList();
            List<Asset>  assets       = getERC721Assets(tokenIdAddrs, realm, result);
            for (Asset asset : assets)
                result.addAssetToTokenBalanceAssets(asset);
        }
        return result;
    }

    public Token createCurrencyToken(NetworkInfo network, Wallet wallet)
    {
        TokenInfo tokenInfo = new TokenInfo(wallet.address, network.name, network.symbol, 18, true, network.chainId);
        BigDecimal balance = BigDecimal.ZERO;
        Token eth = new Token(tokenInfo, balance, 0, network.getShortName(), ContractType.ETHEREUM); //create with zero time index to ensure it's updated immediately
        eth.setTokenWallet(wallet.address);
        eth.setIsEthereum();
        eth.pendingBalance = balance;
        return eth;
    }
}
