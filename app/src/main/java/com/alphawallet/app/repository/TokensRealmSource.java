package com.alphawallet.app.repository;

import static com.alphawallet.app.service.TickerService.TICKER_TIMEOUT;
import static com.alphawallet.app.service.TokensService.EXPIRED_CONTRACT;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmNFTAsset;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenMapping;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmException;
import timber.log.Timber;

public class TokensRealmSource implements TokenLocalSource {

    public static final String TAG = "TLS";
    public static final String IMAGES_DB = "image_urls_db";
    public static final String ATOKENS_DB = "a_tokens_db";
    public static final String TOKENS_MAPPING_DB = "tokens_mapping_db";
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
            case ERC721_ENUMERABLE:
            case ERC1155:
                saveToken(r, token);
                break;
            //No save
            case NOT_SET:
            case OTHER:
            case CREATION:
                break;
            default:
                Timber.d("Unknown Token Contract");
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

    private TokenTicker convertRealmTicker(RealmTokenTicker rawItem)
    {
        TokenTicker tokenTicker = null;
        if (rawItem != null)
        {
            String currencySymbol = rawItem.getCurrencySymbol();
            String price = rawItem.getPrice();
            String percentChange = rawItem.getPercentChange24h();
            if ((price.equals("0") || TextUtils.isEmpty(price))
                    && (percentChange.equals("0") ||  TextUtils.isEmpty(percentChange))) return null; // blank placeholder ticker to stop spamming the API

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
        catch (Exception e)
        {
            Timber.e(e);
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

            realmToken.setLastTxTime(System.currentTimeMillis());
            realmToken.setAssetUpdateTime(System.currentTimeMillis());

            if (realmToken.getBalance() == null || !realmToken.getBalance().equals(String.valueOf(assetCount)))
            {
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

                //does the token need updating?
                if (token.checkInfoRequiresUpdate(realmToken))
                {
                    realm.executeTransaction(r -> {
                        realmToken.setName(token.tokenInfo.name);
                        realmToken.setSymbol(token.tokenInfo.symbol);
                        realmToken.setDecimals(token.tokenInfo.decimals);
                        realmToken.setInterfaceSpec(token.getInterfaceSpec().ordinal());
                    });
                }

                if ((token.isERC721()) && balance.equals(BigDecimal.ZERO) && !currentBalance.equals("0"))
                {
                    //only used for determining if balance is now zero
                    realm.executeTransaction(r -> {
                        realmToken.setBalance(newBalance);
                        deleteAllAssets(r, key);
                    });
                    Timber.tag(TAG).d("Zero out ERC721 balance: %s :%s", realmToken.getName(), token.getAddress());
                    balanceChanged = true;
                }
                else if (!newBalance.equals(currentBalance) || !checkEthToken(realm, token))
                {
                    realm.executeTransaction(r -> {
                        realmToken.setBalance(newBalance);
                        if (token.isEthereum()) updateEthToken(r, token, newBalance);
                    });
                    Timber.tag(TAG).d("Update Token Balance: %s :%s",realmToken.getName(), token.getAddress());
                    balanceChanged = true;
                }

                if (!realmToken.isVisibilityChanged() && realmToken.isEnabled() && newBalance != null && newBalance.equals("0")
                    && !(token.isEthereum() && CustomViewSettings.alwaysShow(token.tokenInfo.chainId)))
                {
                    realm.executeTransaction(r -> {
                        realmToken.setEnabled(false);
                        realmToken.setBalance("0");
                    });
                }
                else if ((!realmToken.isVisibilityChanged() && !realmToken.isEnabled()) &&
                        (token.balance.compareTo(BigDecimal.ZERO) > 0 ||
                                (token.isEthereum() && CustomViewSettings.alwaysShow(token.tokenInfo.chainId) && !realmToken.isEnabled()))) // enable if base token should be showing
                {
                    realm.executeTransaction(r -> {
                        realmToken.setEnabled(true);
                        realmToken.setUpdateTime(System.currentTimeMillis());
                    });
                }
            }
            else
            {
                balanceChanged = true;
                if (token.isEthereum() && CustomViewSettings.alwaysShow(token.tokenInfo.chainId)) token.tokenInfo.isEnabled = true;
                //write token
                realm.executeTransaction(r -> {
                    token.balance = balance;
                    saveTokenLocal(r, token);
                });
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
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
            final String instanceKey = address.toLowerCase() + "-" + networkId;
            final RealmAuxData instance = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", instanceKey)
                    .findFirst();

            if (instance == null || !instance.getResult().equals(imageUrl))
            {
                realm.executeTransactionAsync(r -> {
                    RealmAuxData aux;
                    if (instance == null)
                    {
                        aux = r.createObject(RealmAuxData.class, instanceKey);
                    }
                    else
                    {
                        aux = instance;
                    }

                    aux.setResult(imageUrl);
                    aux.setResultTime(System.currentTimeMillis());
                    r.insertOrUpdate(aux);
                });
            }
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
            Timber.tag(TAG).d("Save New Token: " + token.getFullName() + " :" + token.tokenInfo.address + " : " + token.balance.toString());
            realmToken = realm.createObject(RealmToken.class, databaseKey);
            realmToken.setName(token.tokenInfo.name);
            realmToken.setSymbol(token.tokenInfo.symbol);
            realmToken.setDecimals(token.tokenInfo.decimals);
            token.setRealmBalance(realmToken);
            token.setRealmInterfaceSpec(realmToken);
            token.setRealmLastBlock(realmToken);
            realmToken.setEnabled(token.tokenInfo.isEnabled);
            realmToken.setChainId(token.tokenInfo.chainId);
            realm.insertOrUpdate(realmToken);
            wasNew = true;
        }
        else
        {
            Timber.tag(TAG).d("Update Token: %s", token.getFullName());
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

            //check if name needs to be updated
            if (!TextUtils.isEmpty(token.tokenInfo.name) && (TextUtils.isEmpty(realmToken.getName()) || !realmToken.getName().equals(token.tokenInfo.name)))
            {
                realmToken.setName(token.tokenInfo.name);
            }
        }

        //Final check to see if the token should be visible
        if ((token.balance.compareTo(BigDecimal.ZERO) > 0 || token.getBalanceRaw().compareTo(BigDecimal.ZERO) > 0)
                && !realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            if (wasNew) Timber.tag(TAG).d("Save New Token set enable");
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

        realm.insertOrUpdate(realmNFT);
    }

    // NFT Assets From Opensea
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

        realm.insertOrUpdate(realmAsset);
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
                        null, t.getName(), t.getSymbol(), t.getContractType(),
                        getTokenGroup(t.getChainId(), t.getTokenAddress(), t.getContractType()));
                meta.lastTxUpdate = t.getLastTxTime();
                meta.isEnabled = t.isEnabled();

                tokenMetas.add(meta);
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
        finally
        {
            //create metas for any card not previously saved
            for (Long chainId : rootChainTokenCards)
            {
                TokenCardMeta meta = new TokenCardMeta(chainId, wallet.address.toLowerCase(), "0", 0, null, "", "", ContractType.ETHEREUM, TokenGroup.ASSET);
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

                    TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance,
                            t.getUpdateTime(), svs, t.getName(), t.getSymbol(), t.getContractType(),
                            getTokenGroup(t.getChainId(), t.getTokenAddress(), t.getContractType()));
                    meta.lastTxUpdate = t.getLastTxTime();
                    tokenMetas.add(meta);
                    meta.isEnabled = t.isEnabled();
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return tokenMetas.toArray(new TokenCardMeta[0]);
        });
    }

    @Override
    public Single<Pair<Double, Double>> getTotalValue(String currentAddress, List<Long> networkFilters)
    {
        final Wallet wallet = new Wallet(currentAddress);
        return calculateWalletValue(fetchTokenMetasForUpdate(wallet, networkFilters), wallet);
    }

    private Single<Pair<Double, Double>> calculateWalletValue(TokenCardMeta[] metas, Wallet wallet)
    {
        return Single.fromCallable(() -> {
            //fetch all token tickers
            Map<Long, Map<String, TokenTicker>> tickerMap = fetchAllTokenTickers();
            BigDecimal historicalBalance = BigDecimal.ZERO;
            BigDecimal newBalance = BigDecimal.ZERO;
            BigDecimal hundred = BigDecimal.valueOf(100);
            for (TokenCardMeta meta : metas)
            {
                long chainId = meta.getChain();
                String address = meta.isEthereum() ? "eth" : meta.getAddress();
                Map<String, TokenTicker> localTickers = tickerMap.get(chainId);
                TokenTicker ticker = localTickers != null ? localTickers.get(address) : null;
                if (ticker != null && meta.hasPositiveBalance() && !meta.isNFT()) //Currently we don't add NFT value. TODO: potentially get value from OpenSea
                {
                    Token t = fetchToken(chainId, wallet, meta.getAddress());
                    BigDecimal correctedBalance = t.getCorrectedBalance(18);
                    BigDecimal fiatValue = correctedBalance.multiply(new BigDecimal(ticker.price)).setScale(18, RoundingMode.DOWN);
                    historicalBalance = historicalBalance.add(fiatValue.add(fiatValue.multiply((new BigDecimal(ticker.percentChange24h)
                            .divide(hundred)).negate())));
                    newBalance = newBalance.add(fiatValue);
                }
            }

            return new Pair<>(newBalance.doubleValue(), historicalBalance.doubleValue());
        });
    }

    private Map<Long, Map<String, TokenTicker>> fetchAllTokenTickers()
    {
        Map<Long, Map<String, TokenTicker>> tickerMap = new HashMap<>();
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            RealmResults<RealmTokenTicker> realmTickers = realm.where(RealmTokenTicker.class)
                    .findAll();

            for (RealmTokenTicker ticker : realmTickers)
            {
                Map<String, TokenTicker> networkMap = tickerMap.get(ticker.getChain());
                if (networkMap == null)
                {
                    networkMap = new HashMap<>();
                    tickerMap.put(ticker.getChain(), networkMap);
                }

                TokenTicker tt = convertRealmTicker(ticker);

                if (tt != null)
                {
                    networkMap.put(ticker.getContract(), tt);
                }
            }
        }
        catch (Exception e)
        {
            //
        }

        return tickerMap;
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
                        .beginGroup()
                        .like("auxData", "*" + searchTerm + "*", Case.INSENSITIVE)
                        .or().like("symbol", "*" + searchTerm + "*", Case.INSENSITIVE)
                        .or().like("name", "*" + searchTerm + "*", Case.INSENSITIVE)
                        .endGroup()
                        .like("address", ADDRESS_FORMAT)
                        .findAll();

                for (RealmToken t : realmItems)
                {
                    if (networkFilters.size() > 0 && !networkFilters.contains(t.getChainId())) continue;
                    String balance = convertStringBalance(t.getBalance(), t.getContractType());
                    TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance,
                            t.getUpdateTime(), null, t.getAuxData(), t.getSymbol(), t.getContractType(),
                            getTokenGroup(t.getChainId(), t.getTokenAddress(), t.getContractType()));
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
        List<ContractAddress> tickerUpdates = new ArrayList<>();
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                for (long chainId : ethTickers.keySet())
                {
                    if (writeTickerToRealm(r, ethTickers.get(chainId), chainId, "eth"))
                    {
                        tickerUpdates.add(new ContractAddress(chainId, "eth"));
                    }
                }
            });
        }
        catch (Exception e)
        {
            //
        }

        //This will trigger an update of the holder
        updateWalletTokens(tickerUpdates);
    }

    private void updateWalletTokens(final List<ContractAddress> tickerUpdates)
    {
        final String currentWallet = ethereumNetworkRepository.getCurrentWalletAddress();
        if (TextUtils.isEmpty(currentWallet)) return;

        try (Realm realm = realmManager.getRealmInstance(currentWallet))
        {
            realm.executeTransaction(r -> {
                for (ContractAddress contract : tickerUpdates)
                {
                    String contractAddress = contract.address.equals("eth") ? currentWallet : contract.address;
                    RealmToken realmToken = r.where(RealmToken.class)
                            .equalTo("address", databaseKey(contract.chainId, contractAddress))
                            .findFirst();

                    if (realmToken != null && realmToken.isEnabled())
                    {
                        realmToken.setUpdateTime(System.currentTimeMillis());
                    }
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
        List<ContractAddress> tickerUpdates = new ArrayList<>();
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                for (String tokenAddress : erc20Tickers.keySet())
                {
                    if (writeTickerToRealm(r, erc20Tickers.get(tokenAddress), chainId, tokenAddress))
                    {
                        tickerUpdates.add(new ContractAddress(chainId, tokenAddress));
                    }
                }
            });
        }
        catch (Exception e)
        {
            //
        }

        updateWalletTokens(tickerUpdates);
    }

    @Override
    public void updateTicker(long chainId, String address, TokenTicker ticker)
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                writeTickerToRealm(r, ticker, chainId, address);
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
    public Map<String, Long> getTickerTimeMap(long chainId, List<TokenCardMeta> erc20Tokens)
    {
        Map<String, Long> updateMap = new HashMap<>();
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            for (TokenCardMeta meta : erc20Tokens)
            {
                String databaseKey = databaseKey(chainId, meta.getAddress().toLowerCase());
                RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                        .equalTo("contract", databaseKey)
                        .findFirst();

                if (realmItem != null)
                {
                    updateMap.put(meta.getAddress(), realmItem.getUpdatedTime());
                }
            }
        }

        return updateMap;
    }

    /**
     * Returns list of recently updated tickers.
     * This is an optimisation for the TokenAdapter to only update UI elements with recent ticker update
     * @param networkFilter list of displayed networks
     * @return list of recently updated tickers
     */
    @Override
    public Single<List<String>> getTickerUpdateList(List<Long> networkFilter)
    {
        return Single.fromCallable(() -> {
            List<String> tickerContracts = new ArrayList<>();
            try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
            {
                RealmResults<RealmTokenTicker> realmItems = realm.where(RealmTokenTicker.class)
                        .greaterThan("updatedTime", System.currentTimeMillis() - 5*DateUtils.MINUTE_IN_MILLIS)
                        .findAll();

                for (RealmTokenTicker ticker : realmItems)
                {
                    if (networkFilter.contains(ticker.getChain())) tickerContracts.add(ticker.getContract());
                }
            }

            return tickerContracts;
        });
    }

    @Override
    public void deleteTickers()
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                RealmResults<RealmTokenTicker> realmItems = r.where(RealmTokenTicker.class)
                        //.lessThan("updatedTime", System.currentTimeMillis() - TICKER_TIMEOUT)
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
            realm.executeTransaction(r -> {
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

    private boolean writeTickerToRealm(Realm realm, final TokenTicker ticker, long chainId, String tokenAddress)
    {
        if (ticker == null) return false;
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
                //no update, but update the received time
                realmItem.setUpdatedTime(ticker.updateTime);
                return false;
            }
        }

        realmItem.setPercentChange24h(ticker.percentChange24h);
        realmItem.setPrice(ticker.price);
        realmItem.setImage(TextUtils.isEmpty(ticker.image)
                ? ""
                : ticker.image);
        realmItem.setUpdatedTime(ticker.updateTime);
        realmItem.setCurrencySymbol(ticker.priceSymbol);
        realm.insertOrUpdate(realmItem);
        return true;
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
                case ERC721_ENUMERABLE:
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

    public long getLastMappingsUpdate()
    {
        long lastUpdate = 0;
        try (Realm realm = realmManager.getRealmInstance(TOKENS_MAPPING_DB))
        {
            RealmAuxData lastUpdateTime = realm.where(RealmAuxData.class)
                    .equalTo("instanceKey", "UPDATETIME")
                    .findFirst();

            if (lastUpdateTime != null)
            {
                lastUpdate = lastUpdateTime.getResultTime();
            }
        }

        return lastUpdate;
    }

    public int storeTokensMapping(Pair<Map<String, ContractAddress>, Map<String, TokenGroup>> mappings) {
        try (Realm realm = realmManager.getRealmInstance(TOKENS_MAPPING_DB))
        {
            realm.executeTransaction(r -> {
                //erase old mappings
                r.where(RealmTokenMapping.class).findAll().deleteAllFromRealm();

                for (String tokenMapping : mappings.first.keySet())
                {
                    ContractAddress mapping = new ContractAddress(tokenMapping);

                    ContractAddress baseContract = mappings.first.get(tokenMapping);
                    if (baseContract != null && mappings.second.containsKey(baseContract.getAddressKey()))
                    {
                        RealmTokenMapping rtm = r.where(RealmTokenMapping.class)
                                .equalTo("address", mapping.getAddressKey())
                                .findFirst();

                        if (rtm == null) rtm = r.createObject(RealmTokenMapping.class, mapping.getAddressKey());

                        TokenGroup baseGroup = mappings.second.get(baseContract.getAddressKey());
                        rtm.base = baseContract.getAddressKey();
                        rtm.group = baseGroup.ordinal();
                        r.insertOrUpdate(rtm);
                    }
                }

                //now store base contracts, note that since ASSET is the default, we don't need to store the ASSET keys
                for (String baseToken : mappings.second.keySet())
                {
                    TokenGroup baseGroup = mappings.second.get(baseToken);
                    if (baseGroup == null || baseGroup == TokenGroup.ASSET) continue; //no need to explicitly declare ASSET

                    RealmTokenMapping rtm = r.where(RealmTokenMapping.class)
                            .equalTo("address", baseToken)
                            .findFirst();

                    if (rtm == null) rtm = r.createObject(RealmTokenMapping.class, baseToken);
                    rtm.group = baseGroup.ordinal();
                    rtm.base = "";
                    r.insertOrUpdate(rtm);
                }

                RealmAuxData lastUpdateTime = r.where(RealmAuxData.class)
                        .equalTo("instanceKey", "UPDATETIME")
                        .findFirst();

                if (lastUpdateTime == null) lastUpdateTime = r.createObject(RealmAuxData.class, "UPDATETIME");
                lastUpdateTime.setResultTime(System.currentTimeMillis());
                r.insertOrUpdate(lastUpdateTime);
            });
        }

        return mappings.first.keySet().size();
    }

    public ContractAddress getBaseToken(long chainId, String address)
    {
        try (Realm realm = realmManager.getRealmInstance(TOKENS_MAPPING_DB))
        {
            RealmTokenMapping rtm = realm.where(RealmTokenMapping.class)
                    .equalTo("address", databaseKey(chainId, address))
                    .findFirst();

            if (rtm != null)
            {
                return rtm.getBase();
            }
        }

        return null;
    }

    public TokenGroup getTokenGroup(long chainId, String address, ContractType type)
    {
        TokenGroup tg = TokenGroup.ASSET;
        try (Realm realm = realmManager.getRealmInstance(TOKENS_MAPPING_DB))
        {
            RealmTokenMapping rtm = realm.where(RealmTokenMapping.class)
                    .equalTo("address", databaseKey(chainId, address))
                    .findFirst();

            if (rtm != null)
            {
                tg = rtm.getGroup();
            }
        }

        switch (type)
        {
            case NOT_SET:
            case OTHER:
            case ETHEREUM:
            case CURRENCY:
            case CREATION:
            case DELETED_ACCOUNT:
            case ERC20:
            default:
                return tg;

            case ERC721:
            case ERC721_ENUMERABLE:
            case ERC875_LEGACY:
            case ERC875:
            case ERC1155:
            case ERC721_LEGACY:
            case ERC721_TICKET:
            case ERC721_UNDETERMINED:
                return TokenGroup.NFT;
        }
    }
}
