package com.alphawallet.app.repository;

import android.text.TextUtils;
import android.util.Log;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.SingleSource;
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
                realm.executeTransaction(r -> {
                    for (Token token : items) {
                        if (token.tokenInfo != null && token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT) && token.tokenInfo.symbol != null)
                        {
                            saveTokenLocal(r, token);
                        }
                    }
                });
            }
            catch (Exception e)
            {
                //
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
    public void deleteRealmToken(long chainId, Wallet wallet, String address)
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
                realm.executeTransaction(r -> saveTokenLocal(r, token));
            }
            return token;
        });
    }

    @Override
    public String getTokenImageUrl(long networkId, String address)
    {
        String url = "";
        String instanceKey = address.toLowerCase() + "-" + networkId;
        try (Realm realm = realmManager.getRealmInstance(IMAGES_DB))
        {
            RealmAuxData instance = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", instanceKey)
                    .findFirst();

            if (instance != null)
            {
                url = instance.getResult();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return url;
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
            case ERC1155:
                saveToken(r, token);
                break;
            //No save
            case NOT_SET:
            case OTHER:
            case CREATION:
                break;
            default:
                if (BuildConfig.DEBUG) System.out.println("Unknown Token Contract");
                break;
        }
    }

    @Override
    public Token fetchToken(long chainId, Wallet wallet, String address)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmItem = realm.where(RealmToken.class)
                    .equalTo("address", databaseKey(chainId, address))
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
            realm.executeTransaction(r -> {
                NetworkInfo[] allMainNetworks = ethereumNetworkRepository.getAllActiveNetworks();
                for (NetworkInfo info : allMainNetworks)
                {
                    RealmToken realmItem = r.where(RealmToken.class)
                            .equalTo("address", databaseKey(info.chainId, walletAddress))
                            .findFirst();

                    if (realmItem == null)
                    {
                        saveToken(r, createCurrencyToken(info, new Wallet(walletAddress)));
                    }
                }
            });
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
                    .findFirst();

            if (realmToken != null)
            {
                isEnabled = realmToken.isEnabled();
            }
        }

        return isEnabled;
    }

    @Override
    public void storeAsset(String wallet, Token token, BigInteger tokenId, NFTAsset asset)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransactionAsync(r -> {
                writeAsset(r, token, tokenId, asset);
            });
        }
    }

    @Override
    public void updateNFTAssets(String wallet, Token token, List<BigInteger> additions, List<BigInteger> removals)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransaction(r -> {
                createTokenIfRequired(r, token);
                deleteAssets(r, token, removals);
                int assetCount = updateNFTAssets(r, token, additions);
                //now re-do the balance
                assetCount = token.getBalanceRaw().intValue();

                setTokenUpdateTime(r, token, assetCount);
            });
        }
    }

    private void createTokenIfRequired(Realm realm, Token token)
    {
        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", databaseKey(token))
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

            if (realmToken.getBalance() == null || !realmToken.getBalance().equals(String.valueOf(assetCount)))
            {
                realmToken.setLastTxTime(System.currentTimeMillis());
                realmToken.setAssetUpdateTime(System.currentTimeMillis());
                realmToken.setBalance(String.valueOf(assetCount));
            }
        }
    }

    private int updateNFTAssets(Realm realm, Token token, List<BigInteger> additions) throws RealmException
    {
        if (!token.isNonFungible()) return 0;

        //load all the old assets
        Map<BigInteger, NFTAsset> assetMap = getNFTAssets(realm, token);
        int assetCount = assetMap.size();

        for (BigInteger updatedTokenId : additions)
        {
            NFTAsset asset = assetMap.get(updatedTokenId);
            if (asset == null || asset.requiresReplacement())
            {
                writeAsset(realm, token, updatedTokenId, new NFTAsset());
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

    public static String databaseKey(long chainId, String address)
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

    public static String eventBlockKey(long chainId, String eventAddress, String namedType, String filter)
    {
        return eventAddress.toLowerCase() + "-" + chainId + "-" + namedType + "-" + filter + "-eventBlock";
    }

    @Override
    public boolean updateTokenBalance(Wallet wallet, Token token, BigDecimal balance, List<BigInteger> balanceArray)
    {
        boolean balanceChanged = false;
        String key = databaseKey(token);
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmToken realmToken = realm.where(RealmToken.class)
                    .equalTo("address", key)
                    .findFirst();

            if (realmToken != null)
            {
                final String currentBalance = realmToken.getBalance();
                final String newBalance = (balanceArray == null) ? balance.toString() : Utils.bigIntListToString(balanceArray, true);

                if ((token.isERC721()) && balance.equals(BigDecimal.ZERO) && !currentBalance.equals("0"))
                {
                    //only used for determining if balance is now zero
                    realm.executeTransaction(r -> {
                        realmToken.setBalance(newBalance);
                        deleteAllAssets(r, key);
                    });
                    if (BuildConfig.DEBUG) Log.d(TAG, "Zero out ERC721 balance: " + realmToken.getName() + " :" + token.getAddress());
                    balanceChanged = true;
                }
                else if (!newBalance.equals(currentBalance) || !checkEthToken(realm, token))
                {
                    realm.executeTransaction(r -> {
                        realmToken.setBalance(newBalance);
                        if (token.isEthereum()) updateEthToken(r, token, newBalance);
                    });
                    if (BuildConfig.DEBUG) Log.d(TAG, "Update Token Balance: " + realmToken.getName() + " :" + token.getAddress());
                    balanceChanged = true;
                }

                if (!realmToken.isVisibilityChanged() && realmToken.isEnabled() && newBalance != null && newBalance.equals("0"))
                {
                    realm.executeTransaction(r -> realmToken.setEnabled(false));
                }
            }
            else
            {
                //write token
                realm.executeTransaction(r -> {
                    token.balance = balance;
                    saveTokenLocal(r, token);
                });
            }
        }
        catch (Exception e)
        {
            //
        }

        return balanceChanged;
    }

    private boolean checkEthToken(Realm realm, Token token)
    {
        if (!token.isEthereum()) return true;
        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", databaseKey(token.tokenInfo.chainId, "eth"))
                .findFirst();

        return realmToken != null;
    }

    private void updateEthToken(Realm realm, Token token, String newBalance)
    {
        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", databaseKey(token.tokenInfo.chainId, "eth"))
                .findFirst();

        if (realmToken == null)
        {
            TokenFactory tf = new TokenFactory();
            TokenInfo tInfo = new TokenInfo("eth", token.tokenInfo.name, token.tokenInfo.symbol, token.tokenInfo.decimals,
                    true, token.tokenInfo.chainId);
            saveToken(realm, tf.createToken(tInfo, new BigDecimal(newBalance), null, System.currentTimeMillis(), ContractType.ETHEREUM,
                    token.getNetworkName(), System.currentTimeMillis()));
        }
        else if (!realmToken.getBalance().equals(newBalance))
        {
            realmToken.setBalance(newBalance);
        }
    }

    @Override
    public void storeTokenUrl(long networkId, String address, String imageUrl)
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
                .findFirst();

        boolean wasNew = false;

        if (realmToken == null)
        {
            if (BuildConfig.DEBUG) Log.d(TAG, "Save New Token: " + token.getFullName() + " :" + token.tokenInfo.address + " : " + token.balance.toString());
            realmToken = realm.createObject(RealmToken.class, databaseKey);
            realmToken.setName(token.tokenInfo.name);
            realmToken.setSymbol(token.tokenInfo.symbol);
            realmToken.setDecimals(token.tokenInfo.decimals);
            token.setRealmBalance(realmToken);
            token.setRealmInterfaceSpec(realmToken);
            token.setRealmLastBlock(realmToken);
            realmToken.setEnabled(token.tokenInfo.isEnabled);
            realmToken.setChainId(token.tokenInfo.chainId);
            wasNew = true;
        }
        else
        {
            if (BuildConfig.DEBUG) Log.d(TAG, "Update Token: " + token.getFullName());
            realmToken.updateTokenInfoIfRequired(token.tokenInfo);
            Token oldToken = convertSingle(realmToken, realm, null, new Wallet(token.getWallet()));

            if (token.checkBalanceChange(oldToken))
            {
                //has token changed?
                token.setRealmInterfaceSpec(realmToken);
                token.setRealmBalance(realmToken);
                token.setRealmLastBlock(realmToken);
                writeAssetContract(realm, token);
            }
        }

        if (wasNew && BuildConfig.DEBUG && realmToken.isEnabled())
        {
            Log.d(TAG, "Save New Token already enabled");
        }

        //Final check to see if the token should be visible
        if ((token.balance.compareTo(BigDecimal.ZERO) > 0 || token.getBalanceRaw().compareTo(BigDecimal.ZERO) > 0)
                && !realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            if (wasNew && BuildConfig.DEBUG) Log.d(TAG, "Save New Token set enable");
            token.tokenInfo.isEnabled = true;
            realmToken.setEnabled(true);
        }
        else if (!token.isEthereum() && (token.balance.compareTo(BigDecimal.ZERO) <= 0 && token.getBalanceRaw().compareTo(BigDecimal.ZERO) <= 0)
                && realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            token.tokenInfo.isEnabled = false;
            realmToken.setEnabled(false);
        }
    }

    private void writeAssetContract(final Realm realm, Token token)
    {
        if (token == null || token.getAssetContract() == null) return;

        String databaseKey = databaseKey(token);
        RealmNFTAsset realmNFT = realm.where(RealmNFTAsset.class)
                .equalTo("tokenIdAddr", databaseKey)
                .findFirst();

        if (realmNFT == null)
        {
            realmNFT = realm.createObject(RealmNFTAsset.class, databaseKey);
            realmNFT.setMetaData(token.getAssetContract().getJSON());
        }
        else
        {
            realmNFT.setMetaData(token.getAssetContract().getJSON());
        }
    }

    @Override
    public Token[] initNFTAssets(Wallet wallet, Token[] tokens)
    {
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            realm.executeTransaction(r -> {
                for (Token token : tokens)
                {
                    if (!token.isNonFungible()) continue;

                    //load all the assets from the database
                    Map<BigInteger, NFTAsset> assetMap = getNFTAssets(r, token);

                    //construct live list
                    //for erc1155 need to check each potential 'removal'.
                    //erc721 gets removed by noting token transfer
                    Map<BigInteger, NFTAsset> liveMap = token.queryAssets(assetMap);
                    HashSet<BigInteger> deleteList = new HashSet<>();

                    for (BigInteger tokenId : liveMap.keySet())
                    {
                        NFTAsset oldAsset = assetMap.get(tokenId); //may be null
                        NFTAsset newAsset = liveMap.get(tokenId); //never null

                        if (newAsset.getBalance().compareTo(BigDecimal.ZERO) == 0)
                        {
                            deleteAssets(r, token, Collections.singletonList(tokenId));
                            deleteList.add(tokenId);
                        }
                        else
                        {
                            //token updated or new
                            if (oldAsset != null) { newAsset.updateAsset(oldAsset); }
                            writeAsset(r, token, tokenId, newAsset);
                        }
                    }

                    for (BigInteger tokenId : deleteList)
                    {
                        liveMap.remove(tokenId);
                    }

                    //update token balance & visibility
                    setTokenUpdateTime(r, token, liveMap.keySet().size());
                    token.balance = new BigDecimal(liveMap.keySet().size());
                    if (token.getTokenAssets().hashCode() != liveMap.hashCode()) //replace asset map if different
                    {
                        token.getTokenAssets().clear();
                        token.getTokenAssets().putAll(liveMap);
                    }
                }
            });
        }

        return tokens;
    }

    private void writeAsset(Realm realm, Token token, BigInteger tokenId, NFTAsset asset)
    {
        String key = RealmNFTAsset.databaseKey(token, tokenId);
        RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                .equalTo("tokenIdAddr", key)
                .findFirst();

        if (realmAsset == null)
        {
            realmAsset = realm.createObject(RealmNFTAsset.class, key);
        }
        else if (asset.equals(realmAsset))
        {
            return;
        }

        realmAsset.setMetaData(asset.jsonMetaData());
        realmAsset.setBalance(asset.getBalance());
    }

    private void deleteAllAssets(Realm realm, String dbKey) throws RealmException
    {
        String key = dbKey + "-";

        RealmResults<RealmNFTAsset> realmAssets = realm.where(RealmNFTAsset.class)
                .beginsWith("tokenIdAddr", key, Case.INSENSITIVE)
                .findAll();

        realmAssets.deleteAllFromRealm();
    }

    private void deleteAssets(Realm realm, Token token, List<BigInteger> assetIds)
    {
        for (BigInteger tokenId : assetIds)
        {
            RealmNFTAsset realmAsset = realm.where(RealmNFTAsset.class)
                    .equalTo("tokenIdAddr",  RealmNFTAsset.databaseKey(token, tokenId))
                    .findFirst();

            if (realmAsset != null) realmAsset.deleteFromRealm();
            token.getTokenAssets().remove(tokenId);
        }
    }

    private Map<BigInteger, NFTAsset> getNFTAssets(Realm realm, Token token)
    {
        Map<BigInteger, NFTAsset> assets = new HashMap<>();

        RealmResults<RealmNFTAsset> results = realm.where(RealmNFTAsset.class)
                .like("tokenIdAddr", databaseKey(token) + "-*", Case.INSENSITIVE)
                .findAll();

        for (RealmNFTAsset realmAsset : results)
        {
            try
            {
                //grab all assets for this tokenId
                BigInteger tokenId = new BigInteger(realmAsset.getTokenId());
                NFTAsset asset = new NFTAsset(realmAsset);
                assets.put(tokenId, asset);
            }
            catch (NumberFormatException e)
            {
                // Just in case tokenId got corrupted
            }
        }

        return assets;
    }

    public TokenCardMeta[] fetchTokenMetasForUpdate(Wallet wallet, List<Long> networkFilters)
    {
        List<TokenCardMeta> tokenMetas = new ArrayList<>();
        List<Long> rootChainTokenCards = new ArrayList<>(networkFilters);
        try (Realm realm = realmManager.getRealmInstance(wallet))
        {
            RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                    .sort("addedTime", Sort.ASCENDING)
                    .beginGroup().equalTo("isEnabled", true).or().equalTo("visibilityChanged", false)
                        .or().like("address", wallet.address + "*", Case.INSENSITIVE).endGroup()
                    .findAll();

            for (RealmToken t : realmItems)
            {
                if (networkFilters.size() > 0 && !networkFilters.contains(t.getChainId()) ||
                        (!t.getEnabled() && t.isVisibilityChanged()) || // Don't update tokens hidden by user
                        (ethereumNetworkRepository.isChainContract(t.getChainId(), t.getTokenAddress()))) continue;

                if (t.getContractType() == ContractType.ETHEREUM)
                {
                    if (rootChainTokenCards.contains(t.getChainId()))
                    {
                        rootChainTokenCards.remove((Long) t.getChainId());
                    }
                    else
                    {
                        continue;
                    }
                }

                TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(),
                        convertStringBalance(t.getBalance(), t.getContractType()), t.getUpdateTime(),
                        null, t.getName(), t.getSymbol(), t.getContractType());
                meta.lastTxUpdate = t.getLastTxTime();
                meta.isEnabled = t.isEnabled();

                tokenMetas.add(meta);
            }
        }
        catch (Exception e)
        {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
        finally
        {
            //create metas for any card not previously saved
            for (Long chainId : rootChainTokenCards)
            {
                TokenCardMeta meta = new TokenCardMeta(chainId, wallet.address.toLowerCase(), "0", 0, null, "", "", ContractType.ETHEREUM);
                meta.lastTxUpdate = 0;
                meta.isEnabled = true;
                tokenMetas.add(meta);
            }
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
    public Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Long> networkFilters, AssetDefinitionService svs)
    {
        List<Long> rootChainTokenCards = new ArrayList<>(networkFilters);
        return Single.fromCallable(() -> {
            List<TokenCardMeta> tokenMetas = new ArrayList<>();
            //ensure root tokens for filters are in there
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                RealmResults<RealmToken> realmItems = realm.where(RealmToken.class)
                        .sort("addedTime", Sort.ASCENDING)
                        .beginGroup().equalTo("isEnabled", true).or().like("address", wallet.address + "*", Case.INSENSITIVE).endGroup()
                        .like("address", ADDRESS_FORMAT)
                        .findAll();

                for (RealmToken t : realmItems)
                {
                    if (networkFilters.size() > 0 && !networkFilters.contains(t.getChainId())) continue;
                    if (t.getContractType() == ContractType.ETHEREUM && !(t.getTokenAddress().equalsIgnoreCase(wallet.address))) continue;
                    if (ethereumNetworkRepository.isChainContract(t.getChainId(), t.getTokenAddress())) continue;
                    String balance = convertStringBalance(t.getBalance(), t.getContractType());

                    if (t.getContractType() == ContractType.ETHEREUM) //only allow 1 base per chain
                    {
                        if (rootChainTokenCards.contains(t.getChainId()))
                        {
                            rootChainTokenCards.remove((Long)t.getChainId());
                        }
                        else
                        {
                            continue;
                        }
                    }

                    TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance, t.getUpdateTime(), svs, t.getName(), t.getSymbol(), t.getContractType());
                    meta.lastTxUpdate = t.getLastTxTime();
                    tokenMetas.add(meta);
                    meta.isEnabled = t.isEnabled();
                }

                removeLocalTickers(realm); //delete any local tickers, these have all moved into a single realm
            }
            catch (Exception e)
            {
                if (BuildConfig.DEBUG) e.printStackTrace();
            }

            return tokenMetas;
        }).flatMap(loadedMetas -> populateBaseCards(wallet, rootChainTokenCards, loadedMetas));
    }

    private Single<TokenCardMeta[]> populateBaseCards(Wallet wallet, List<Long> rootChainTokenCards, List<TokenCardMeta> loadedMetas)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getRealmInstance(wallet))
            {
                realm.executeTransaction(r -> {
                    for (long requiredNetwork : rootChainTokenCards)
                    {
                        RealmToken realmItem = r.where(RealmToken.class)
                                .equalTo("address", databaseKey(requiredNetwork, wallet.address))
                                .findFirst();

                        if (realmItem == null)
                        {
                            Token token = createCurrencyToken(ethereumNetworkRepository.getNetworkByChain(requiredNetwork), wallet);
                            saveToken(r, token);
                            loadedMetas.add(new TokenCardMeta(token));
                        }
                    }
                });
            }
            return loadedMetas.toArray(new TokenCardMeta[0]);
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
    public Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Long> networkFilters, String searchTerm) {
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
                    meta.isEnabled = t.isEnabled();
                    tokenMetas.add(meta);
                }
            }

            return tokenMetas.toArray(new TokenCardMeta[0]);
        });
    }

    @Override
    public Single<Token[]> fetchAllTokensWithNameIssue(String walletAddress, List<Long> networkFilters) {
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
    public Single<ContractAddress[]> fetchAllTokensWithBlankName(String walletAddress, List<Long> networkFilters) {
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
    public void updateEthTickers(Map<Long, TokenTicker> ethTickers)
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransactionAsync(r -> {
                for (long chainId : ethTickers.keySet())
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
    public void updateERC20Tickers(long chainId, final Map<String, TokenTicker> erc20Tickers)
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                for (String tokenAddress : erc20Tickers.keySet())
                {
                    writeTickerToRealm(r, erc20Tickers.get(tokenAddress), chainId, tokenAddress);
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
        return getCurrentTicker(databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()));
    }

    @Override
    public TokenTicker getCurrentTicker(String key)
    {
        TokenTicker tt = null;
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
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

    private void writeTickerToRealm(Realm realm, final TokenTicker ticker, long chainId, String tokenAddress)
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
                case ERC1155:
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

        if (realmItem.getTokenAddress().equals("eth"))
        {
            info = new TokenInfo(wallet.address, info.name, info.symbol, info.decimals, info.isEnabled, info.chainId);
        }

        Token result = tf.createToken(info, realmItem, realmItem.getUpdateTime(), network.getShortName());
        result.setTokenWallet(wallet.address);

        if (result.isNonFungible())
        {
            Map<BigInteger, NFTAsset> assets = getNFTAssets(realm, result);
            for (BigInteger tokenId : assets.keySet())
            {
                result.addAssetToTokenBalanceAssets(tokenId, assets.get(tokenId));
            }

            loadAssetContract(realm, result);
        }
        return result;
    }

    private void loadAssetContract(Realm realm, Token token)
    {
        String databaseKey = databaseKey(token);
        RealmNFTAsset realmNFT = realm.where(RealmNFTAsset.class)
                .equalTo("tokenIdAddr", databaseKey)
                .findFirst();

        try
        {
            if (realmNFT != null)
            {
                AssetContract assetContract = new Gson().fromJson(realmNFT.getMetaData(), AssetContract.class);
                token.setAssetContract(assetContract);
            }
        }
        catch (JsonSyntaxException e)
        {
            //
        }
    }

    private Token createCurrencyToken(NetworkInfo network, Wallet wallet)
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
