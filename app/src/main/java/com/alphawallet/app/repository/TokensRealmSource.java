package com.alphawallet.app.repository;

import android.text.TextUtils;
import android.util.Log;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.opensea.AssetContract;
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
import com.alphawallet.token.entity.ContractAddress;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmException;

import static com.alphawallet.app.service.TickerService.TICKER_TIMEOUT;
import static com.alphawallet.app.service.TokensService.EXPIRED_CONTRACT;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class TokensRealmSource implements TokenLocalSource {

    public static final String TAG = "TLS";
    public static final String IMAGES_DB = "image_urls_db";
    public static final String TICKER_DB = "tickers_db";
    public static final String ADDRESS_FORMAT = "0x????????????????????????????????????????-*";

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
        if (!Utils.isAddressValid(wallet.address)) { return Single.fromCallable(() -> items); }
        else return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                realm.executeTransactionAsync(r -> {
                    for (Token token : items) {
                        if (token.tokenInfo != null && token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT) && token.tokenInfo.symbol != null)
                        {
                            saveTokenLocal(r, token);
                        }
                    }
                });
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
                realm.executeTransactionAsync(r -> {
                    for (Token token : tokens)
                    {
                        saveToken(r, token);
                    }
                });
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
            realm.executeTransactionAsync(r -> {
                String dbKey = databaseKey(chainId, address);
                RealmToken realmToken = r.where(RealmToken.class)
                        .equalTo("address", dbKey)
                        .findFirst();

                if (realmToken != null)
                {
                    realmToken.deleteFromRealm();
                }
            });
        }
    }

    @Override
    public Token updateTokenType(Token token, Wallet wallet, ContractType type)
    {
        token.setInterfaceSpec(type);
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address);
            realm.executeTransactionAsync(r -> {
                RealmToken realmToken = r.where(RealmToken.class)
                        .equalTo("address", dbKey, Case.INSENSITIVE)
                        .findFirst();

                if (realmToken == null)
                {
                    saveToken(r, token);
                }
                else
                {
                    realmToken.setInterfaceSpec(type.ordinal());
                    realmToken.setName(token.tokenInfo.name);
                    realmToken.setSymbol(token.tokenInfo.symbol);
                }
            });

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
        if (!Utils.isAddressValid(wallet.address)) { return Single.fromCallable(() -> token); }
        else return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                realm.executeTransactionAsync(r -> saveTokenLocal(r, token));
            }
            return token;
        });
    }

    private void saveTokenLocal(Realm r, Token token)
    {
        switch (token.getInterfaceSpec())
        {
            case ETHEREUM:
            case ERC20:
            case DYNAMIC_CONTRACT:
            case ERC875_LEGACY:
            case ERC875:
            case CURRENCY:
            case ERC721_TICKET:
            case MAYBE_ERC20:
            case ERC721:
            case ERC721_LEGACY:
                saveToken(r, token);
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
                if (info != null) { t = createCurrencyToken(info, wallet); }
            }

            return t;
        }
    }

    @Override
    public void createBaseNetworkTokens(String walletAddress)
    {
        try (Realm realm = realmManager.getRealmInstance(walletAddress))
        {
            realm.executeTransactionAsync(r -> {
                NetworkInfo[] allMainNetworks = ethereumNetworkRepository.getAllActiveNetworks();
                for (NetworkInfo info : allMainNetworks)
                {
                    RealmToken realmItem = r.where(RealmToken.class)
                            .equalTo("address", databaseKey(info.chainId, walletAddress))
                            .equalTo("chainId", info.chainId)
                            .findFirst();

                    if (realmItem == null)
                    {
                        saveToken(r, createCurrencyToken(info, new Wallet(walletAddress)));
                    }
                }
            });

            realm.refresh();
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
    public void setEnable(Wallet wallet, Token token, boolean isEnabled) {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransactionAsync(r -> {
                RealmToken realmToken = r.where(RealmToken.class)
                        .equalTo("address", databaseKey(token))
                        .equalTo("chainId", token.tokenInfo.chainId)
                        .findFirst();

                if (realmToken != null)
                {
                    realmToken.setEnabled(isEnabled);
                }
            });
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
    public void storeAsset(String wallet, Token token, Asset asset)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransactionAsync(r -> {
                writeAsset(r, asset, token);
            });
        }
    }

    @Override
    public void updateERC721Assets(String wallet, Token token, List<BigInteger> additions, List<BigInteger> removals)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransaction(r -> {
                createTokenIfRequired(r, token);
                deleteAssets(r, token, removals);
                int assetCount = updateERC721Assets(r, token, additions);
                setTokenUpdateTime(r, token, assetCount);
            });
        }
    }

    private void createTokenIfRequired(Realm realm, Token token)
    {
        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", databaseKey(token))
                .equalTo("chainId", token.tokenInfo.chainId)
                .findFirst();

        if (realmToken == null)
        {
            saveToken(realm, token);
        }
    }

    private void setTokenUpdateTime(Realm realm, Token token, int assetCount)
    {
        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", databaseKey(token))
                .equalTo("chainId", token.tokenInfo.chainId)
                .findFirst();

        if (realmToken != null)
        {
            if (!realmToken.isEnabled() && !realmToken.isVisibilityChanged() && assetCount > 0)
            {
                token.tokenInfo.isEnabled = true;
                realmToken.setEnabled(true);
            }
            else if (!realmToken.isVisibilityChanged() && assetCount == 0)
            {
                token.tokenInfo.isEnabled = false;
                realmToken.setEnabled(false);
            }

            realmToken.setLastTxTime(System.currentTimeMillis());
        }
    }

    private int updateERC721Assets(Realm realm, Token token, List<BigInteger> additions) throws RealmException
    {
        if (!token.isERC721()) return 0;

        //load all the old assets
        Map<BigInteger, Asset> assetMap = getERC721Assets(realm, token);
        int assetCount = assetMap.size();

        for (BigInteger updatedTokenId : additions)
        {
            Asset asset = assetMap.get(updatedTokenId);
            if (asset == null || asset.requiresReplacement())
            {
                writeAsset(realm, Asset.blankLoading(updatedTokenId), token);
                if (asset == null) assetCount++;
            }
        }

        return assetCount;
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
            realm.executeTransactionAsync(r -> {
                RealmToken realmToken = r.where(RealmToken.class)
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
                final String currentBalance = realmToken.getBalance();
                final String newBalance = (balanceArray == null) ? balance.toString() : Utils.bigIntListToString(balanceArray, true);

                if ((type == ContractType.ERC721 || type == ContractType.ERC721_LEGACY) && balance.equals(BigDecimal.ZERO) && !currentBalance.equals("0"))
                {
                    //only used for determining if balance is now zero
                    realm.executeTransaction(r -> {
                        realmToken.setBalance(newBalance);
                        realmToken.setUpdateTime(System.currentTimeMillis());
                        deleteAllAssets(r, key);
                    });
                    Log.d(TAG, "Zero out ERC721 balance: " + realmToken.getName() + " :" + tokenAddress);
                    balanceChanged = true;
                }
                else if (!newBalance.equals(currentBalance))
                {
                    realm.executeTransaction(r -> {
                        realmToken.setBalance(newBalance);
                        realmToken.setUpdateTime(System.currentTimeMillis());
                    });
                    Log.d(TAG, "Update Token Balance: " + realmToken.getName() + " :" + tokenAddress);
                    balanceChanged = true;
                }

                if (!realmToken.isVisibilityChanged() && realmToken.isEnabled() && newBalance != null && newBalance.equals("0"))
                {
                    realm.executeTransaction(r -> realmToken.setEnabled(false));
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
                realm.executeTransaction(r -> saveToken(r, token));
            }
            else if (realmToken != null && token.checkRealmBalanceChange(realmToken))
            {
                realm.executeTransaction(r -> {
                    token.setRealmBalance(realmToken);
                    realmToken.updateTokenInfoIfRequired(token.tokenInfo);
                    token.setRealmInterfaceSpec(realmToken);
                });
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void storeTokenUrl(int networkId, String address, String imageUrl)
    {
        try (Realm realm = realmManager.getRealmInstance(IMAGES_DB))
        {
            realm.executeTransactionAsync(r -> {
                String instanceKey = address.toLowerCase() + "-" + networkId;

                RealmAuxData instance = r.where(RealmAuxData.class)
                        .equalTo("instanceKey", instanceKey)
                        .findFirst();

                if (instance == null)
                {
                    instance = r.createObject(RealmAuxData.class, instanceKey);
                }

                instance.setResult(imageUrl);
                instance.setResultTime(System.currentTimeMillis());
            });
        }
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
            if (BuildConfig.DEBUG) Log.d(TAG, "Save New Token: " + token.getFullName() + " :" + token.tokenInfo.address + " : " + token.balance.toString());
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
        }
        else
        {
            if (BuildConfig.DEBUG) Log.d(TAG, "Update Token: " + token.getFullName());
            realmToken.updateTokenInfoIfRequired(token.tokenInfo);

            if (token.checkRealmBalanceChange(realmToken))
            {
                //has token changed?
                realmToken.setUpdateTime(token.updateBlancaTime);
                token.setRealmInterfaceSpec(realmToken);
                token.setRealmBalance(realmToken);
                token.setRealmLastBlock(realmToken);
            }
        }

        //Final check to see if the token should be visible
        if (token.getBalanceRaw().compareTo(BigDecimal.ZERO) > 0 && !realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            token.tokenInfo.isEnabled = true;
            realmToken.setEnabled(true);
        }
        else if (!token.isEthereum() && token.getBalanceRaw().compareTo(BigDecimal.ZERO) <= 0 && realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            token.tokenInfo.isEnabled = false;
            realmToken.setEnabled(false);
        }
    }

    @Override
    public Token[] initERC721Assets(Wallet wallet, Token[] tokens)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransaction(r -> {
                for (Token token : tokens)
                {
                    if (!token.isERC721()) continue;

                    String dbKey = databaseKey(token);

                    //load all the old assets
                    Map<BigInteger, Asset> assetMap = getERC721Assets(r, token);

                    deleteAllAssets(r, dbKey);

                    setTokenUpdateTime(r, token, token.getTokenAssets().size());

                    //now create the assets inside this
                    for (Asset asset : token.getTokenAssets().values())
                    {
                        //check against existing assets; did the existing asset have better details?
                        asset.updateAsset(assetMap);
                        writeAsset(r, asset, token);
                    }
                }
            });
        }

        return tokens;
    }

    private void writeAsset(Realm realm, Asset asset, Token token)
    {
        String key = RealmERC721Asset.tokenIdAddrName(asset.getTokenId(), databaseKey(token));
        RealmERC721Asset realmAsset = realm.where(RealmERC721Asset.class)
                .equalTo("tokenIdAddr", key)
                .findFirst();

        if (realmAsset == null)
        {
            realmAsset = realm.createObject(RealmERC721Asset.class, key);
        }

        realmAsset.setName(asset.getName());
        realmAsset.setDescription(asset.getDescription());
        realmAsset.setExternalLink(asset.getExternalLink());
        realmAsset.setImagePreviewUrl(asset.getImagePreviewUrl());
        realmAsset.setImageOriginalUrl(asset.getImageOriginalUrl());
        realmAsset.setImageThumbnailUrl(asset.getThumbnailUrl());
        realmAsset.setBackgroundColor(asset.getBackgroundColor());
        realmAsset.setTraits(asset.getTraits());
    }

    private void deleteAllAssets(Realm realm, String dbKey) throws RealmException
    {
        String key = dbKey + "-";

        RealmResults<RealmERC721Asset> realmAssets = realm.where(RealmERC721Asset.class)
                .beginsWith("tokenIdAddr", key, Case.INSENSITIVE)
                .findAll();

        realmAssets.deleteAllFromRealm();
    }

    private void deleteAssets(Realm realm, Token token, List<BigInteger> assetIds)
    {
        for (BigInteger tokenId : assetIds)
        {
            RealmERC721Asset realmAsset = realm.where(RealmERC721Asset.class)
                    .equalTo("tokenIdAddr", RealmERC721Asset.tokenIdAddrName(tokenId.toString(), databaseKey(token)))
                    .findFirst();
            if (realmAsset != null) realmAsset.deleteFromRealm();
        }
    }

    private Map<BigInteger, Asset> getERC721Assets(Realm realm, Token token)
    {
        Map<BigInteger, Asset> assets = new HashMap<>();
        AssetContract contract = new AssetContract(token);

        RealmResults<RealmERC721Asset> results = realm.where(RealmERC721Asset.class)
                .like("tokenIdAddr", databaseKey(token) + "-*", Case.INSENSITIVE)
                .findAll();

        for (RealmERC721Asset realmAsset : results)
        {
            try
            {
                //grab all assets for this tokenId
                BigInteger tokenId = new BigInteger(realmAsset.getTokenId());
                Asset asset = new Asset(tokenId, contract);
                asset.setBackgroundColor(realmAsset.getBackgroundColor());
                asset.setDescription(realmAsset.getDescription());
                asset.setExternalLink(realmAsset.getExternalLink());
                asset.setImagePreviewUrl(realmAsset.getImagePreviewUrl());
                asset.setImageOriginalUrl(realmAsset.getImageOriginalUrl());
                asset.setImageThumbnailUrl(realmAsset.getImageThumbnailUrl());
                asset.setTraits(realmAsset.getTraits());
                asset.setName(realmAsset.getName());
                assets.put(tokenId, asset);
            }
            catch (NumberFormatException e)
            {
                // Just in case tokenId got corrupted
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
                    .beginGroup().equalTo("isEnabled", true).or().like("address", wallet.address + "*", Case.INSENSITIVE).endGroup()
                    .like("address", ADDRESS_FORMAT)
                    .findAll();

            for (RealmToken t : realmItems)
            {
                if (networkFilters.size() > 0 && !networkFilters.contains(t.getChainId()) ||
                        (t.getContractType() != ContractType.ETHEREUM && !t.getEnabled()) ||
                        (ethereumNetworkRepository.isChainContract(t.getChainId(), t.getTokenAddress()))) continue;

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
                    if (ethereumNetworkRepository.isChainContract(t.getChainId(), t.getTokenAddress())) continue;
                    String balance = convertStringBalance(t.getBalance(), t.getContractType());

                    if (t.getContractType() == ContractType.ETHEREUM && !(t.getTokenAddress().equalsIgnoreCase(wallet.address)
                            || t.getTokenAddress().equals("eth")))
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
    public Single<Token[]> fetchAllTokensWithNameIssue(String walletAddress, List<Integer> networkFilters) {
        List<Token> tokens = new ArrayList<>();
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(walletAddress))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class) //TODO: Work out how to specify '?' in a Realm filter
                        .findAll();

                TokenFactory tf = new TokenFactory();
                for (RealmToken realmItem : realmItems)
                {
                    if (networkFilters.size() > 0 && !networkFilters.contains(realmItem.getChainId())) continue;
                    if ((!TextUtils.isEmpty(realmItem.getName()) && realmItem.getName().contains("??"))
                        || (!TextUtils.isEmpty(realmItem.getSymbol()) && realmItem.getSymbol().contains("??")))
                    {
                        tokens.add(convertSingle(realmItem, realm, tf, new Wallet(walletAddress)));
                    }
                }
            }

            return tokens.toArray(new Token[0]);
        });
    }

    @Override
    public Single<ContractAddress[]> fetchAllTokensWithBlankName(String walletAddress, List<Integer> networkFilters) {
        List<ContractAddress> tokens = new ArrayList<>();
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(walletAddress))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .like("address", ADDRESS_FORMAT)
                        .like("name", "")
                        .findAll();

                for (RealmToken realmItem : realmItems)
                {
                    if (networkFilters.size() > 0 && !networkFilters.contains(realmItem.getChainId())) continue;
                    if (TextUtils.isEmpty(realmItem.getName()))
                    {
                        tokens.add(new ContractAddress(realmItem.getChainId(), realmItem.getTokenAddress()));
                    }
                }
            }

            return tokens.toArray(new ContractAddress[0]);
        });
    }

    @Override
    public void updateEthTickers(Map<Integer, TokenTicker> ethTickers)
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransactionAsync(r -> {
                for (int chainId : ethTickers.keySet())
                {
                    writeTickerToRealm(r, ethTickers.get(chainId), chainId, "eth");
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    @Override
    public void updateERC20Tickers(final Map<String, TokenTicker> erc20Tickers)
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransactionAsync(r -> {
                for (String tokenAddress : erc20Tickers.keySet())
                {
                    writeTickerToRealm(r, erc20Tickers.get(tokenAddress), MAINNET_ID, tokenAddress);
                }
            });
        }
        catch (Exception e)
        {
            //
        }
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
    public void removeOutdatedTickers()
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransactionAsync(r -> {
                RealmResults<RealmTokenTicker> realmItems = r.where(RealmTokenTicker.class)
                        .lessThan("updatedTime", System.currentTimeMillis() - TICKER_TIMEOUT)
                        .findAll();

                for (RealmTokenTicker data : realmItems)
                {
                    data.deleteFromRealm();
                }
            });
        }
        catch (Exception e)
        {
            //
        }
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
                case ERC721_UNDETERMINED:
                case ERC721:
                case ERC721_LEGACY:
                default:
                    return balance;

                case ERC721_TICKET:
                case ERC875_LEGACY:
                case ERC875:
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
            Map<BigInteger, Asset>  assets = getERC721Assets(realm, result);
            for (Asset asset : assets.values())
            {
                result.addAssetToTokenBalanceAssets(asset);
            }
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
