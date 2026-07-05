package com.alphawallet.app.repository;

import static com.alphawallet.app.repository.EthereumNetworkBase.getNetwork;
import static com.alphawallet.app.service.TickerService.TICKER_STALE_TIMEOUT;
import static com.alphawallet.app.service.TickerService.TICKER_TIMEOUT;
import static com.alphawallet.app.service.TokensService.EXPIRED_CONTRACT;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.ImageEntry;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Attestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.realm.AutoCloseableRealm;
import com.alphawallet.app.realm.RealmCompat;
import com.alphawallet.app.repository.entity.RealmAttestation;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.realm.kotlin.Realm;
import io.realm.kotlin.TypedRealm;
import timber.log.Timber;

public class TokensRealmSource implements TokenLocalSource
{
    public static final String TAG = "TLS";
    public static final String IMAGES_DB = "image_urls_db";
    public static final String ATOKENS_DB = "a_tokens_db";
    public static final String TICKER_DB = "tickers_db";
    public static final String ADDRESS_FORMAT = "0x????????????????????????????????????????-*";
    private static final String WALLET_VALUE_KEY_PREFIX = "VALUE_";

    public static final String EVENT_CARDS = "-eventName";

    private final RealmManager realmManager;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensMappingRepositoryType tokensMappingRepository;

    public TokensRealmSource(RealmManager realmManager, EthereumNetworkRepositoryType ethereumNetworkRepository, TokensMappingRepositoryType tokensMappingRepository)
    {
        this.realmManager = realmManager;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokensMappingRepository = tokensMappingRepository;
    }

    public static String databaseKey(long chainId, String address)
    {
        return address.toLowerCase() + "-" + chainId;
    }

    public static String databaseKey(Token token)
    {
        String address = token.isEthereum() ? "eth" : token.tokenInfo.address.toLowerCase();
        return databaseKey(token.tokenInfo.chainId, address);
    }

    /** Realm row key for a native chain balance or ERC20/NFT token. */
    public static String realmTokenDatabaseKey(long chainId, String address, boolean isNativeEth)
    {
        return databaseKey(chainId, isNativeEth ? "eth" : address.toLowerCase());
    }

    public static String realmTokenDatabaseKey(Token token)
    {
        return databaseKey(token);
    }

    /** Realm primary keys for token rows (excludes event/activity pseudo-tokens). */
    public static boolean isValidTokenDatabaseKey(String address)
    {
        if (TextUtils.isEmpty(address))
        {
            return false;
        }
        if (address.contains(EVENT_CARDS) || address.contains("-eventBlock"))
        {
            return false;
        }
        if (address.regionMatches(true, 0, "migrated-", 0, 9))
        {
            return false;
        }
        if (address.regionMatches(true, 0, "eth-", 0, 4))
        {
            return true;
        }
        int dashIndex = address.indexOf('-');
        if (dashIndex != 42 || !address.regionMatches(true, 0, "0x", 0, 2))
        {
            return false;
        }
        for (int i = 2; i < dashIndex; i++)
        {
            char c = address.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')))
            {
                return false;
            }
        }
        return true;
    }

    /** Parse chain id suffix from a RealmToken primary key. */
    public static long chainIdFromDatabaseKey(String databaseKey)
    {
        if (TextUtils.isEmpty(databaseKey))
        {
            return 0;
        }
        if (databaseKey.regionMatches(true, 0, "eth-", 0, 4))
        {
            try
            {
                return Long.parseLong(databaseKey.substring(4));
            }
            catch (NumberFormatException e)
            {
                return 0;
            }
        }
        int separatorIndex = databaseKey.indexOf('-', 2);
        if (separatorIndex < 0)
        {
            separatorIndex = databaseKey.indexOf('.', 2);
        }
        if (separatorIndex > 0 && separatorIndex < databaseKey.length() - 1)
        {
            try
            {
                String suffix = databaseKey.substring(separatorIndex + 1);
                int nextDash = suffix.indexOf('-');
                if (nextDash > 0)
                {
                    suffix = suffix.substring(0, nextDash);
                }
                return Long.parseLong(suffix);
            }
            catch (NumberFormatException e)
            {
                return 0;
            }
        }
        return 0;
    }

    public static long resolveChainId(RealmToken token)
    {
        long chainId = token.getChainId();
        if (chainId > 0)
        {
            return chainId;
        }
        return chainIdFromDatabaseKey(token.getAddress());
    }

    private static boolean shouldPurgeRealmToken(String address)
    {
        return TextUtils.isEmpty(address) || address.regionMatches(true, 0, "migrated-", 0, 9);
    }

    private static boolean isNativeTokenRow(RealmToken token, Wallet wallet)
    {
        String key = token.getAddress();
        String tokenAddress = token.getTokenAddress();
        return key.regionMatches(true, 0, "eth-", 0, 4)
                || tokenAddress.equalsIgnoreCase("eth")
                || tokenAddress.equalsIgnoreCase(wallet.address);
    }

    private static ContractType resolveContractType(RealmToken token, Wallet wallet)
    {
        ContractType contractType = token.getContractType();
        if (isNativeTokenRow(token, wallet))
        {
            return ContractType.ETHEREUM;
        }
        String tokenAddress = token.getTokenAddress();
        if (contractType == ContractType.NOT_SET
                && (tokenAddress.equalsIgnoreCase(wallet.address) || tokenAddress.equalsIgnoreCase("eth")))
        {
            contractType = ContractType.ETHEREUM;
        }
        return contractType;
    }

    private static String resolveTokenDisplayAddress(RealmToken token, Wallet wallet)
    {
        String tokenAddress = token.getTokenAddress();
        if (tokenAddress.equalsIgnoreCase("eth"))
        {
            return wallet.address.toLowerCase();
        }
        return tokenAddress;
    }

    public static boolean matchesWalletTokenQuery(RealmToken token, Wallet wallet)
    {
        if (token.getEnabled())
        {
            return true;
        }
        if (!token.isVisibilityChanged())
        {
            return true;
        }
        String address = token.getAddress();
        return address.regionMatches(true, 0, wallet.address, 0, wallet.address.length())
                || address.regionMatches(true, 0, "eth-", 0, 4);
    }

    /** Removes empty or migration-orphan rows that match queries but carry no token data. */
    public static void purgeInvalidRealmTokens(Realm realm)
    {
        RealmCompat.executeTransaction(realm, r -> {
            List<RealmToken> invalid = new ArrayList<>();
            for (RealmToken token : RealmCompat.where(r, RealmToken.class).findAll())
            {
                if (shouldPurgeRealmToken(token.getAddress()) || token.getTokenAddress().equals("eth") && token.getName().isEmpty())
                {
                    invalid.add(token);
                }
            }
            for (RealmToken token : invalid)
            {
                RealmCompat.deleteFromRealm(r, token);
            }
            if (!invalid.isEmpty())
            {
                Timber.tag(TAG).w("Purged %d invalid RealmToken rows", invalid.size());
            }
        });
    }

    /** Backfill chainId/interfaceSpec on migrated rows where field values were lost. */
    public static void repairMigratedRealmTokens(Realm realm, Wallet wallet)
    {
        RealmCompat.executeTransaction(realm, r -> {
            int repaired = 0;
            for (RealmToken token : RealmCompat.where(r, RealmToken.class).findAll())
            {
                String key = token.getAddress();
                if (key.isEmpty())
                {
                    continue;
                }
                boolean changed = false;
                if (token.getChainId() <= 0)
                {
                    long chainId = chainIdFromDatabaseKey(key);
                    if (chainId > 0)
                    {
                        token.setChainId(chainId);
                        changed = true;
                    }
                }
                if (isNativeTokenRow(token, wallet))
                {
                    if (token.getInterfaceSpec() != ContractType.ETHEREUM.ordinal())
                    {
                        token.setInterfaceSpec(ContractType.ETHEREUM.ordinal());
                        changed = true;
                    }
                }
                else if (token.getInterfaceSpec() == 0 && key.regionMatches(true, 0, "0x", 0, 2))
                {
                    token.setInterfaceSpec(ContractType.ERC20.ordinal());
                    changed = true;
                }
                if (changed)
                {
                    RealmCompat.insertOrUpdate(r, token);
                    repaired++;
                }
            }
            if (repaired > 0)
            {
                Timber.tag(TAG).w("Repaired %d migrated RealmToken rows", repaired);
            }
        });
    }

    private static boolean isWalletNativeToken(ContractType contractType, String tokenAddress, Wallet wallet)
    {
        return contractType == ContractType.ETHEREUM
                && (tokenAddress.equalsIgnoreCase(wallet.address) || tokenAddress.equalsIgnoreCase("eth"));
    }

    private static boolean isNativeTokenLookup(String address, Wallet wallet)
    {
        return address != null
                && (address.equalsIgnoreCase(wallet.address) || address.equalsIgnoreCase("eth"));
    }

    /** Canonical Realm primary key for a chain's native balance row. */
    public static String nativeTokenStorageKey(long chainId)
    {
        return databaseKey(chainId, "eth");
    }

    /** Key used by TokensService pendingTokenMap; native tokens always use eth-{chainId}. */
    public static String pendingMapKey(long chainId, String tokenAddress, ContractType type, String walletAddress)
    {
        if (type == ContractType.ETHEREUM
                || (walletAddress != null && isNativeTokenLookup(tokenAddress, new Wallet(walletAddress))))
        {
            return nativeTokenStorageKey(chainId);
        }
        return databaseKey(chainId, tokenAddress);
    }

    public static String pendingMapKey(TokenCardMeta meta, String walletAddress)
    {
        return pendingMapKey(meta.getChain(), meta.getAddress(), meta.type, walletAddress);
    }

    public static String pendingMapKey(Token token)
    {
        return databaseKey(token);
    }

    private static final java.util.Set<String> walletMaintenanceComplete =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public static void resetWalletMaintenance(String walletAddress)
    {
        if (walletAddress != null)
        {
            walletMaintenanceComplete.remove(walletAddress.toLowerCase());
        }
    }

    private static void runWalletMaintenanceIfNeeded(Realm realm, Wallet wallet, boolean force)
    {
        String walletKey = wallet.address.toLowerCase();
        if (!force && walletMaintenanceComplete.contains(walletKey))
        {
            return;
        }
        purgeInvalidRealmTokens(realm);
        repairMigratedRealmTokens(realm, wallet);
        consolidateNativeTokenRows(realm, wallet);
        walletMaintenanceComplete.add(walletKey);
    }

    private static RealmToken findNativeRealmToken(TypedRealm realm, long chainId, Wallet wallet)
    {
        RealmToken ethRow = RealmCompat.where(realm, RealmToken.class)
                .equalTo("address", nativeTokenStorageKey(chainId))
                .findFirst();
        if (ethRow != null)
        {
            return ethRow;
        }
        return RealmCompat.where(realm, RealmToken.class)
                .equalTo("address", databaseKey(chainId, wallet.address))
                .findFirst();
    }

    private static void copyRealmTokenFields(RealmToken source, RealmToken target)
    {
        target.setName(source.getName());
        target.setSymbol(source.getSymbol());
        target.setDecimals(source.getDecimals());
        target.setAddedTime(source.getUpdateTime());
        target.setUpdateTime(source.getBalanceUpdateTime());
        target.setLastTxTime(source.getLastTxTime());
        target.setBalance(source.getBalance());
        target.setEnabled(source.getEnabled());
        target.setTokenId(source.getTokenId());
        target.setInterfaceSpec(source.getInterfaceSpec());
        target.setAuxData(source.getAuxData());
        target.setLastBlock(source.getLastBlock());
        target.setChainId(source.getChainId() > 0 ? source.getChainId() : chainIdFromDatabaseKey(source.getAddress()));
        target.setEarliestTransactionBlock(source.getEarliestTransactionBlock());
        target.setVisibilityChanged(source.isVisibilityChanged());
        target.writeErc1155BlockRead(source.readErc1155BlockRead());
    }

    /** Merge legacy 0x{wallet}-{chainId} native rows into canonical eth-{chainId} keys. */
    public static void consolidateNativeTokenRows(Realm realm, Wallet wallet)
    {
        RealmCompat.executeTransaction(realm, r -> {
            java.util.HashMap<Long, RealmToken> walletNativeRows = new java.util.HashMap<>();
            for (RealmToken token : RealmCompat.where(r, RealmToken.class).findAll())
            {
                String key = token.getAddress();
                if (!key.regionMatches(true, 0, wallet.address, 0, wallet.address.length())
                        || key.length() <= wallet.address.length()
                        || key.charAt(wallet.address.length()) != '-')
                {
                    continue;
                }
                long chainId = chainIdFromDatabaseKey(key);
                if (chainId > 0)
                {
                    walletNativeRows.put(chainId, token);
                }
            }

            int merged = 0;
            for (java.util.Map.Entry<Long, RealmToken> entry : walletNativeRows.entrySet())
            {
                long chainId = entry.getKey();
                RealmToken walletRow = entry.getValue();
                String ethKey = nativeTokenStorageKey(chainId);
                RealmToken ethRow = RealmCompat.where(r, RealmToken.class).equalTo("address", ethKey).findFirst();

                if (ethRow == null)
                {
                    ethRow = RealmCompat.createObject(r, RealmToken.class, ethKey);
                    copyRealmTokenFields(walletRow, ethRow);
                    ethRow.setChainId(chainId);
                    ethRow.setInterfaceSpec(ContractType.ETHEREUM.ordinal());
                    RealmCompat.insertOrUpdate(r, ethRow);
                }
                else
                {
                    String walletBalance = walletRow.getBalance();
                    String ethBalance = ethRow.getBalance();
                    if (!walletBalance.equals("0")
                            && (ethBalance.equals("0") || walletRow.getUpdateTime() > ethRow.getUpdateTime()))
                    {
                        ethRow.setBalance(walletBalance);
                    }
                    if (TextUtils.isEmpty(ethRow.getName()) && !TextUtils.isEmpty(walletRow.getName()))
                    {
                        ethRow.setName(walletRow.getName());
                    }
                    if (TextUtils.isEmpty(ethRow.getSymbol()) && !TextUtils.isEmpty(walletRow.getSymbol()))
                    {
                        ethRow.setSymbol(walletRow.getSymbol());
                    }
                    if (ethRow.getInterfaceSpec() == 0)
                    {
                        ethRow.setInterfaceSpec(ContractType.ETHEREUM.ordinal());
                    }
                    if (!ethRow.getEnabled() && walletRow.getEnabled())
                    {
                        ethRow.setEnabled(true);
                    }
                    RealmCompat.insertOrUpdate(r, ethRow);
                }
                RealmCompat.deleteFromRealm(r, walletRow);
                merged++;
            }
            if (merged > 0)
            {
                Timber.tag(TAG).w("Consolidated %d legacy native token rows into eth-* keys", merged);
            }
        });
    }

    private static void ensureNativeTokenContractType(Token token)
    {
        if (token.getWallet() != null
                && token.tokenInfo.address.equalsIgnoreCase(token.getWallet()))
        {
            token.setIsEthereum();
        }
    }

    private static String resolveTokenStorageKey(Token token)
    {
        ensureNativeTokenContractType(token);
        return databaseKey(token);
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

    public static String attestationDatabaseKey(long chainId, String address, String attnId)
    {
        return address.toLowerCase() + "-" + chainId + "-" + attnId;
    }

    public static String attestationDatabaseKey(long chainId, String address, BigInteger conferenceId, BigInteger ticketId)
    {
        return address.toLowerCase() + "-" + chainId + "-" + conferenceId.toString() + "-" + ticketId.toString();
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

    @Override
    public Single<Token[]> saveTokens(Wallet wallet, Token[] items)
    {
        if (!Utils.isAddressValid(wallet.address))
        {
            return Single.fromCallable(() -> items);
        }
        else
        {
            return Single.fromCallable(() -> {
                try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
                {
            Realm realm = realmCloser.getRealm();
                    RealmCompat.executeTransaction(realm, r -> {
                        for (Token token : items)
                        {
                            if (token.tokenInfo != null && token.tokenInfo.name != null && !token.tokenInfo.name.equals(EXPIRED_CONTRACT) && token.tokenInfo.symbol != null)
                            {
                                saveTokenLocal(r, token);
                            }
                        }
                    });
                }
                catch (Exception e)
                {
                    Timber.w(e);
                }
                return items;
            });
        }
    }

    @Override
    public void deleteRealmTokens(Wallet wallet, List<TokenCardMeta> tcmList)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                for (TokenCardMeta tcm : tcmList)
                {
                    String dbKey = isNativeTokenLookup(tcm.getAddress(), wallet)
                            ? nativeTokenStorageKey(tcm.getChain())
                            : databaseKey(tcm.getChain(), tcm.getAddress());
                    RealmToken realmToken = RealmCompat.where(r, RealmToken.class).equalTo("address", dbKey).findFirst();

                    if (realmToken != null)
                    {
                        RealmCompat.deleteFromRealm(r, realmToken);
                    }
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
        if (!Utils.isAddressValid(wallet.address))
        {
            return Single.fromCallable(() -> token);
        }
        else return Single.fromCallable(() -> {
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.executeTransaction(realm, r -> saveTokenLocal(r, token));
            }
            return token;
        });
    }

    @Override
    public String getTokenImageUrl(long networkId, String address)
    {
        String url = "";
        String instanceKey = address.toLowerCase() + "-" + networkId;
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(IMAGES_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmAuxData instance = RealmCompat.where(realm, RealmAuxData.class).equalTo("instanceKey", instanceKey).findFirst();

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

    private void saveTokenLocal(TypedRealm r, Token token)
    {
        ensureNativeTokenContractType(token);
        ContractType type = token.getInterfaceSpec();
        if (type == ContractType.NOT_SET || type == ContractType.MAYBE_ERC20)
        {
            String dbKey = databaseKey(token);
            if (dbKey.regionMatches(true, 0, "eth-", 0, 4)
                    || (token.getWallet() != null && token.tokenInfo.address.equalsIgnoreCase(token.getWallet())))
            {
                type = ContractType.ETHEREUM;
                token.setIsEthereum();
            }
            else if (token.getAddress().startsWith("0x"))
            {
                type = ContractType.ERC20;
            }
        }

        switch (type)
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmToken realmItem = isNativeTokenLookup(address, wallet)
                    ? findNativeRealmToken(realm, chainId, wallet)
                    : RealmCompat.where(realm, RealmToken.class).equalTo("address", databaseKey(chainId, address)).findFirst();

            Token t = convertSingle(realmItem, realm, null, wallet);
            if (t == null && address.equalsIgnoreCase(wallet.address))
            {
                NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
                if (info != null)
                {
                    t = createCurrencyToken(info, wallet);
                }
            }

            if (t != null)
            {
                t.group = getTokenGroup(chainId, address, ContractType.NOT_SET);
            }

            return t;
        }
    }

    private Token fetchAttestation(long chainId, Wallet wallet, RealmAttestation rAttn)
    {
        Token token = fetchToken(chainId, wallet, rAttn.getTokenAddress()); //<-- getTokenAddress() should be the key
        TokenFactory tf = new TokenFactory();
        Token attn = tf.createAttestation(rAttn, token, ethereumNetworkRepository.getNetworkByChain(chainId), wallet.address);
        attn.setTokenWallet(wallet.address);
        return attn;
    }

    @Override
    public Token fetchAttestation(long chainId, Wallet wallet, String address, String attnId)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmAttestation realmAttestation = RealmCompat.where(realm, RealmAttestation.class)
                    .equalTo("address", attestationDatabaseKey(chainId, address, attnId))
                    .findFirst();

            if (realmAttestation != null)
            {
                return fetchAttestation(chainId, wallet, realmAttestation);
            }
        }

        return null;
    }

    @Override
    public List<Token> fetchAttestations(long chainId, String walletAddress, String tokenAddress)
    {
        List<Token> attestations = new ArrayList<>();
        Token baseToken = fetchToken(chainId, new Wallet(walletAddress), tokenAddress);
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(walletAddress)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.Results<RealmAttestation> realmAttestations = RealmCompat.where(realm, RealmAttestation.class)
                    .like("address", databaseKey(chainId, tokenAddress) + "-*", RealmCompat.Case.INSENSITIVE)
                    .findAll();

            // TODO: Restore using TokenFactory
            for (RealmAttestation thisAttn : realmAttestations)
            {
                Attestation att = new Attestation(baseToken.tokenInfo, baseToken.getNetworkName(), thisAttn.getAttestation());
                att.setTokenWallet(walletAddress);
                att.loadAttestationData(thisAttn, ""); //TODO: Store issuer, expiry dates etc in Realm
                attestations.add(att);
            }
        }

        return attestations;
    }

    @Override
    public List<Token> fetchAttestations(String walletAddress)
    {
        List<Token> attestations = new ArrayList<>();
        Wallet wallet = new Wallet(walletAddress);
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.Results<RealmAttestation> realmItems = RealmCompat.where(realm, RealmAttestation.class)
                    .findAll();

            for (RealmAttestation rAtt : realmItems)
            {
                Attestation attn = (Attestation) fetchAttestation(rAtt.getChains().get(0), wallet, rAtt);
                if (attn != null)
                {
                    attestations.add(attn);
                }
            }
        }

        return attestations;
    }

    private TokenTicker convertRealmTicker(RealmTokenTicker rawItem)
    {
        TokenTicker tokenTicker = null;
        if (rawItem != null)
        {
            String currencySymbol = rawItem.getCurrencySymbol();
            String price = rawItem.getPrice();
            String percentChange = rawItem.getPercentChange24h();
            if ((price.equals("0") || TextUtils.isEmpty(price)) && (percentChange.equals("0") || TextUtils.isEmpty(percentChange)))
                return null; // blank placeholder ticker to stop spamming the API

            if (currencySymbol == null || currencySymbol.length() == 0) currencySymbol = "USD";
            tokenTicker = new TokenTicker(rawItem.getPrice(), rawItem.getPercentChange24h(), currencySymbol, rawItem.getImage(), rawItem.getUpdatedTime());
        }

        return tokenTicker;
    }

    @Override
    public void setEnable(Wallet wallet, ContractAddress cAddr, boolean isEnabled)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                RealmToken realmToken = RealmCompat.where(r, RealmToken.class).equalTo("address", cAddr.getAddressKey()).findFirst();

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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(new Wallet(token.getWallet()))))
        {
            Realm realm = realmCloser.getRealm();
            RealmToken realmToken = RealmCompat.where(realm, RealmToken.class).equalTo("address", databaseKey(token)).findFirst();

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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                writeAsset(r, token, tokenId, asset);
            });
        }
    }

    @Override
    public void updateNFTAssets(String wallet, Token token, List<BigInteger> additions, List<BigInteger> removals)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                RealmToken realmToken = createTokenIfRequired(r, token);
                deleteAssets(r, token, removals);
                populateNFTAssets(r, realmToken, token, additions);
                setTokenUpdateTime(realmToken, token);
            });
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    private RealmToken createTokenIfRequired(TypedRealm realm, Token token)
    {
        RealmToken realmToken = RealmCompat.where(realm, RealmToken.class).equalTo("address", databaseKey(token)).findFirst();

        if (realmToken == null)
        {
            realmToken = saveToken(realm, token);
        }
        else if (realmToken.getInterfaceSpec() != token.getInterfaceSpec().ordinal())
        {
            realmToken.setInterfaceSpec(token.getInterfaceSpec().ordinal());
        }

        return realmToken;
    }

    private void setTokenUpdateTime(RealmToken realmToken, Token token)
    {
        realmToken.setLastTxTime(System.currentTimeMillis());

        if (!realmToken.getBalance().equals(token.getBalanceRaw().toString()))
        {
            token.setRealmBalance(realmToken);
        }
    }

    private void populateNFTAssets(TypedRealm realm, RealmToken realmToken, Token token, List<BigInteger> additions) 
    {
        if (!token.isNonFungible()) return;

        BigDecimal balanceCount = BigDecimal.ZERO;

        //load all the old assets
        Map<BigInteger, NFTAsset> assetMap = getNFTAssets(realm, token);

        //create addition asset map
        Map<BigInteger, NFTAsset> additionMap = new HashMap<>();

        for (BigInteger tokenId : additions)
        {
            NFTAsset asset = assetMap.get(tokenId);
            if (asset == null) asset = new NFTAsset(tokenId);
            additionMap.put(tokenId, asset);
        }

        Map<BigInteger, NFTAsset> balanceMap = token.queryAssets(additionMap);

        List<BigInteger> deleteList = new ArrayList<>();

        //update token assets
        for (Map.Entry<BigInteger, NFTAsset> entry : balanceMap.entrySet())
        {
            if (entry.getValue().getBalance().longValue() == 0)
            {
                deleteList.add(entry.getKey());
            }
            else
            {
                writeAsset(realm, token, entry.getKey(), entry.getValue());
                balanceCount = balanceCount.add(entry.getValue().getBalance());
            }
        }

        if (deleteList.size() > 0)
        {
            deleteAssets(realm, token, deleteList);
        }

        //switch visibility if required
        checkTokenVisibility(realmToken, token, balanceCount);
        token.setRealmBalance(realmToken);
    }

    private void checkTokenVisibility(RealmToken realmToken, Token token, BigDecimal balanceCount)
    {
        if (balanceCount.compareTo(BigDecimal.ZERO) > 0 && !realmToken.getEnabled() && !realmToken.isVisibilityChanged() && token.group != TokenGroup.SPAM)
        {
            token.tokenInfo.isEnabled = true;
            realmToken.setEnabled(true);
        }
        else if (balanceCount.compareTo(BigDecimal.ZERO) == 0 && !realmToken.isVisibilityChanged() && realmToken.isEnabled())
        {
            token.tokenInfo.isEnabled = false;
            realmToken.setEnabled(false);
        }
    }

    @Override
    public void setVisibilityChanged(Wallet wallet, ContractAddress cAddr)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                RealmToken realmToken = RealmCompat.where(r, RealmToken.class).equalTo("address", cAddr.getAddressKey()).findFirst();

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

    @Override
    public boolean updateTokenBalance(Wallet wallet, Token token, BigDecimal balance, List<BigInteger> balanceArray)
    {
        boolean balanceChanged = false;
        if (token.getWallet() == null)
        {
            token.setTokenWallet(wallet.address);
        }
        ensureNativeTokenContractType(token);

        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            String key = resolveTokenStorageKey(token);
            RealmToken realmToken = RealmCompat.where(realm, RealmToken.class).equalTo("address", key).findFirst();

            if (realmToken != null)
            {
                final String currentBalance = realmToken.getBalance();
                final String newBalance = (balanceArray == null || balanceArray.isEmpty()) ? balance.toString() : Utils.bigIntListToString(balanceArray, true);

                //does the token need updating?
                if (token.checkInfoRequiresUpdate(realmToken))
                {
                    RealmCompat.executeTransaction(realm, r -> {
                        RealmToken live = RealmCompat.latest(r, realmToken);
                        live.setName(token.tokenInfo.name);
                        live.setSymbol(token.tokenInfo.symbol);
                        live.setDecimals(token.tokenInfo.decimals);
                        live.setInterfaceSpec(token.getInterfaceSpec().ordinal());
                    });
                }

                validateTokenName(realm, realmToken, token, balance);

                if ((token.isERC721()) && balance.equals(BigDecimal.ZERO) && !currentBalance.equals("0"))
                {
                    //only used for determining if balance is now zero
                    RealmCompat.executeTransaction(realm, r -> {
                        RealmToken live = RealmCompat.latest(r, realmToken);
                        live.setBalance(newBalance);
                        deleteAllAssets(r, key);
                    });
                    Timber.tag(TAG).d("Zero out ERC721 balance: %s :%s", realmToken.getName(), token.getAddress());
                    balanceChanged = true;
                }
                else if (!TextUtils.isEmpty(newBalance) && !newBalance.equals(currentBalance))
                {
                    RealmCompat.executeTransaction(realm, r -> {
                        RealmToken live = RealmCompat.latest(r, realmToken);
                        live.setBalance(newBalance);
                        if (token.isEthereum() && live.getInterfaceSpec() != ContractType.ETHEREUM.ordinal())
                        {
                            live.setInterfaceSpec(ContractType.ETHEREUM.ordinal());
                        }
                        if (currentBalance.equals("0") && !live.isVisibilityChanged())
                        {
                            live.setEnabled(true);
                            live.setUpdateTime(System.currentTimeMillis());
                        }
                    });
                    Timber.tag(TAG).d("Update Token Balance: %s :%s", realmToken.getName(), token.getAddress());
                    balanceChanged = true;
                }
                else if (!TextUtils.isEmpty(newBalance))
                {
                    markBalanceChecked(realm, realmToken);
                }

                if (!realmToken.isVisibilityChanged() && realmToken.isEnabled() && newBalance != null && newBalance.equals("0") && !(token.isEthereum() && CustomViewSettings.alwaysShow(token.tokenInfo.chainId)))
                {
                    RealmCompat.executeTransaction(realm, r -> {
                        RealmToken live = RealmCompat.latest(r, realmToken);
                        live.setEnabled(false);
                        if (!currentBalance.equals("0"))
                        {
                            live.setBalance("0");
                        }
                    });
                }
                else if (token.group != TokenGroup.SPAM && (!realmToken.isVisibilityChanged() && !realmToken.isEnabled()) && (token.balance.compareTo(BigDecimal.ZERO) > 0 || (token.isEthereum() && CustomViewSettings.alwaysShow(token.tokenInfo.chainId) && !realmToken.isEnabled()))) // enable if base token should be showing
                {
                    RealmCompat.executeTransaction(realm, r -> {
                        RealmToken live = RealmCompat.latest(r, realmToken);
                        live.setEnabled(true);
                        live.setUpdateTime(System.currentTimeMillis());
                    });
                }
            }
            else
            {
                balanceChanged = true;
                if (token.isEthereum() && CustomViewSettings.alwaysShow(token.tokenInfo.chainId))
                    token.tokenInfo.isEnabled = true;
                //write token
                RealmCompat.executeTransaction(realm, r -> {
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

    /** Records that a balance RPC completed without changing stored value (feeds update throttle). */
    private static void markBalanceChecked(TypedRealm realm, RealmToken realmToken)
    {
        RealmCompat.executeTransaction(realm, r -> {
            RealmToken live = RealmCompat.latest(r, realmToken);
            live.setAddedTime(System.currentTimeMillis());
        });
    }

    private void validateTokenName(TypedRealm realm, RealmToken realmToken, Token token, BigDecimal balance)
    {
        if (TextUtils.isEmpty(token.tokenInfo.name) && TextUtils.isEmpty(token.tokenInfo.symbol) && balance.compareTo(BigDecimal.ZERO) > 0)
        {
            RealmCompat.executeTransaction(realm, r -> {
                RealmToken live = RealmCompat.latest(r, realmToken);
                live.setName(Utils.formatAddress(token.tokenInfo.address));
            });
        }
    }

    @Override
    public Single<TokenInfo> storeTokenInfo(Wallet wallet, TokenInfo tInfo, ContractType type)
    {
        return Single.fromCallable(() -> {
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.executeTransaction(realm, r -> {
                    Token token = new TokenFactory().createToken(tInfo, BigDecimal.ZERO, null, System.currentTimeMillis(), type,
                            ethereumNetworkRepository.getNetworkByChain(tInfo.chainId).getShortName(), System.currentTimeMillis());
                    token.setTokenWallet(wallet.address);
                    saveToken(r, token);
                });
            }

            return tInfo;
        });
    }

    @Override
    public void storeTokenUrl(List<ImageEntry> entries)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(IMAGES_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                for (ImageEntry thisEntry : entries)
                {
                    final String instanceKey = thisEntry.address.toLowerCase() + "-" + thisEntry.chainId;
                    RealmAuxData instance = RealmCompat.where(r, RealmAuxData.class).equalTo("instanceKey", instanceKey).findFirst();

                    if (instance == null || !instance.getResult().equals(thisEntry.imageUrl))
                    {
                        if (instance == null)
                        {
                            instance = RealmCompat.createObject(r, RealmAuxData.class, instanceKey);
                        }

                        instance.setResult(thisEntry.imageUrl);
                        instance.setResultTime(System.currentTimeMillis());
                        RealmCompat.insertOrUpdate(r, instance);
                    }
                }
            });
        }
    }

    private RealmToken saveToken(TypedRealm realm, Token token) 
    {
        String databaseKey = databaseKey(token);
        RealmToken realmToken = RealmCompat.where(realm, RealmToken.class).equalTo("address", databaseKey).findFirst();

        boolean wasNew = false;

        if (realmToken == null)
        {
            Timber.tag(TAG).d("Save New Token: " + token.getFullName() + " :" + token.tokenInfo.address + " : " + token.balance.toString());
            realmToken = RealmCompat.createObject(realm, RealmToken.class, databaseKey);
            realmToken.setName(token.tokenInfo.name);
            realmToken.setSymbol(token.tokenInfo.symbol);
            realmToken.setDecimals(token.tokenInfo.decimals);
            token.setRealmBalance(realmToken);
            token.setRealmInterfaceSpec(realmToken);
            token.setRealmLastBlock(realmToken);
            realmToken.setEnabled(token.tokenInfo.isEnabled);
            realmToken.setChainId(token.tokenInfo.chainId);
            if (realmToken.getChainId() <= 0)
            {
                realmToken.setChainId(chainIdFromDatabaseKey(databaseKey));
            }
            wasNew = true;
            Timber.tag(TAG).d("Saved New Token pk=%s chainId=%d balance=%s",
                    realmToken.getAddress(), realmToken.getChainId(), realmToken.getBalance());
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

            if (oldToken.getInterfaceSpec() != token.getInterfaceSpec())
            {
                realmToken.setInterfaceSpec(token.getInterfaceSpec().ordinal());
            }

            checkNameUpdate(realmToken, token);
        }

        TokenGroup group = getTokenGroup(token.tokenInfo.chainId, token.tokenInfo.address, ContractType.NOT_SET);
        //Final check to see if the token should be visible
        if (group == TokenGroup.SPAM)
        {
            token.tokenInfo.isEnabled = false;
            realmToken.setEnabled(false);
        }
        else if (token.hasPositiveBalance() && !realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            if (wasNew) Timber.tag(TAG).d("Save New Token set enable");
            token.tokenInfo.isEnabled = true;
            realmToken.setEnabled(true);
        }
        else if (!token.isEthereum() && !token.hasPositiveBalance() && realmToken.getEnabled() && !realmToken.isVisibilityChanged())
        {
            token.tokenInfo.isEnabled = false;
            realmToken.setEnabled(false);
        }

        return realmToken;
    }

    private void checkNameUpdate(RealmToken realmToken, Token token)
    {
        //check if name needs to be updated
        if (!TextUtils.isEmpty(token.tokenInfo.name) && (TextUtils.isEmpty(realmToken.getName()) || !realmToken.getName().equals(token.tokenInfo.name)))
        {
            realmToken.setName(token.tokenInfo.name);
        }

        //This will be an update from the transfer
        if (token.tokenInfo.name.equalsIgnoreCase(Utils.formatAddress(token.tokenInfo.address)) || !TextUtils.isEmpty(token.tokenInfo.name))
        {
            realmToken.setName(token.tokenInfo.name);
            realmToken.setSymbol(token.tokenInfo.symbol);
            realmToken.setDecimals(token.tokenInfo.decimals);
        }
    }

    private void writeAssetContract(final TypedRealm realm, Token token)
    {
        if (token == null || token.getAssetContract() == null) return;

        String databaseKey = databaseKey(token);
        RealmNFTAsset realmNFT = RealmCompat.where(realm, RealmNFTAsset.class).equalTo("tokenIdAddr", databaseKey).findFirst();

        if (realmNFT == null)
        {
            realmNFT = RealmCompat.createObject(realm, RealmNFTAsset.class, databaseKey);
            realmNFT.setMetaData(token.getAssetContract().getJSON());
        }
        else
        {
            realmNFT.setMetaData(token.getAssetContract().getJSON());
        }

        RealmCompat.insertOrUpdate(realm, realmNFT);
    }

    // NFT Assets From Opensea - assume this list is trustworthy - events will catch up with it
    @Override
    public Token initNFTAssets(Wallet wallet, Token token)
    {
        if (!token.isNonFungible()) return token;
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                //load all the assets from the database
                Map<BigInteger, NFTAsset> assetMap = getNFTAssets(r, token);

                //run through the new assets and patch
                for (Map.Entry<BigInteger, NFTAsset> entry : token.getTokenAssets().entrySet())
                {
                    NFTAsset fromOpenSea = entry.getValue();
                    NFTAsset fromDataBase = assetMap.get(entry.getKey());

                    fromOpenSea.updateAsset(fromDataBase);

                    token.getTokenAssets().put(entry.getKey(), fromOpenSea);

                    //write to realm
                    writeAsset(r, token, entry.getKey(), fromOpenSea);
                }
            });
        }
        catch (Exception e)
        {
            Timber.w(e);
        }

        return token;
    }

    private void writeAsset(TypedRealm realm, Token token, BigInteger tokenId, NFTAsset asset)
    {
        String key = RealmNFTAsset.databaseKey(token, tokenId);
        RealmNFTAsset realmAsset = RealmCompat.where(realm, RealmNFTAsset.class).equalTo("tokenIdAddr", key).findFirst();

        if (realmAsset == null)
        {
            realmAsset = RealmCompat.createObject(realm, RealmNFTAsset.class, key);
        }
        else if (asset.equals(realmAsset))
        {
            return;
        }

        realmAsset.setMetaData(asset.jsonMetaData());
        realmAsset.setBalance(asset.getBalance());

        RealmCompat.insertOrUpdate(realm, realmAsset);
    }

    private void deleteAllAssets(TypedRealm realm, String dbKey) 
    {
        String key = dbKey + "-";

        RealmCompat.Results<RealmNFTAsset> realmAssets = RealmCompat.where(realm, RealmNFTAsset.class).beginsWith("tokenIdAddr", key, RealmCompat.Case.INSENSITIVE).findAll();

        realmAssets.deleteAllFromRealm(realm);
    }

    private void deleteAssets(TypedRealm realm, Token token, List<BigInteger> assetIds)
    {
        for (BigInteger tokenId : assetIds)
        {
            RealmNFTAsset realmAsset = RealmCompat.where(realm, RealmNFTAsset.class).equalTo("tokenIdAddr", RealmNFTAsset.databaseKey(token, tokenId)).findFirst();

            if (realmAsset != null) RealmCompat.deleteFromRealm(realm, realmAsset);
            token.getTokenAssets().remove(tokenId);
        }
    }

    private Map<BigInteger, NFTAsset> getNFTAssets(TypedRealm realm, Token token)
    {
        Map<BigInteger, NFTAsset> assets = new HashMap<>();

        RealmCompat.Results<RealmNFTAsset> results = RealmCompat.where(realm, RealmNFTAsset.class).like("tokenIdAddr", databaseKey(token) + "-*", RealmCompat.Case.INSENSITIVE).findAll();

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
        return loadTokenMetasFromRealm(wallet, networkFilters, null, false, false);
    }

    /**
     * Fetches all enabled TokenMetas in database, adding in chain tokens if required
     *
     * @param wallet
     * @param networkFilters
     * @param svs
     * @return
     */
    public Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Long> networkFilters, AssetDefinitionService svs)
    {
        return Single.fromCallable(() -> loadTokenMetasFromRealm(wallet, networkFilters, svs, true, true))
                .flatMap(metas -> joinAttestations(wallet, metas, networkFilters, svs));
    }

    private TokenCardMeta[] loadTokenMetasFromRealm(Wallet wallet, List<Long> networkFilters, AssetDefinitionService svs, boolean restoreDisplayFlags, boolean forceMaintenance)
    {
        List<TokenCardMeta> tokenMetas = new ArrayList<>();
        List<Long> rootChainTokenCards = new ArrayList<>(networkFilters);
        Timber.d("TIMER: Start meta load");
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
        {
            Realm realm = realmCloser.getRealm();
            //runWalletMaintenanceIfNeeded(realm, wallet, forceMaintenance);
            RealmCompat.Results<RealmToken> realmItems = RealmCompat.where(realm, RealmToken.class)
                    .sort("addedTime", RealmCompat.Sort.ASCENDING)
                    .findAll();

            Timber.d("TIMER: iterate metas");

            for (RealmToken t : realmItems)
            {
                long chainId = resolveChainId(t);
                String name = t.getName();
                if (!isValidTokenDatabaseKey(t.getAddress()) || !matchesWalletTokenQuery(t, wallet))
                {
                    continue;
                }
                if (!networkFilters.isEmpty() && !networkFilters.contains(resolveChainId(t)))
                {
                    continue;
                }
                if (!t.getEnabled() && t.isVisibilityChanged())
                {
                    continue;
                }

                ContractType contractType = resolveContractType(t, wallet);
                String tokenAddress = t.getTokenAddress();
                if (contractType == ContractType.ETHEREUM && (!isWalletNativeToken(contractType, tokenAddress, wallet) || name.isEmpty()))
                {
                    continue;
                }
                if (ethereumNetworkRepository.isChainContract(chainId, tokenAddress))
                {
                    continue;
                }

                String balance = convertStringBalance(t.getBalance(), contractType);
                String displayAddress = resolveTokenDisplayAddress(t, wallet);

                if (contractType == ContractType.ETHEREUM) //only allow 1 base per chain
                {
                    if (rootChainTokenCards.contains(chainId))
                    {
                        rootChainTokenCards.remove(chainId);
                    }
                    else
                    {
                        continue;
                    }
                }

                long lastBalanceCheck = Math.max(t.getUpdateTime(), t.getBalanceUpdateTime());
                TokenCardMeta meta = new TokenCardMeta(chainId, displayAddress, balance, lastBalanceCheck, svs,
                        name, t.getSymbol(), contractType, getTokenGroup(chainId, displayAddress, contractType));
                meta.lastTxUpdate = t.getLastTxTime();
                meta.isEnabled = t.isEnabled();
                if (restoreDisplayFlags && !meta.isEnabled && !t.isVisibilityChanged() && !balance.equals("0"))
                {
                    meta.isEnabled = true;
                }
                tokenMetas.add(meta);
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
        finally
        {
            //create metas for any chain root token not previously saved
            for (Long chainId : rootChainTokenCards)
            {
                String name = "";
                String symbol = "";
                //get name for root token
                NetworkInfo chainNet = getNetwork(chainId);
                if (chainNet != null)
                {
                    name = chainNet.name;
                    symbol = chainNet.symbol;
                }

                TokenCardMeta meta = new TokenCardMeta(chainId, wallet.address.toLowerCase(), "0", 0, svs, name, symbol, ContractType.ETHEREUM, TokenGroup.ASSET);
                meta.lastTxUpdate = 0;
                meta.isEnabled = meta.group != TokenGroup.SPAM;
                tokenMetas.add(meta);
            }
        }

        Timber.d("TIMER: return metas");

        return tokenMetas.toArray(new TokenCardMeta[0]);
    }

    private Single<TokenCardMeta[]> joinAttestations(Wallet wallet, TokenCardMeta[] metas, List<Long> networkFilters, AssetDefinitionService svs)
    {
        return Single.fromCallable(() -> {
            List<TokenCardMeta> metaList = new ArrayList<>();
            metaList.addAll(Arrays.asList(metas));
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.Results<RealmAttestation> realmItems = RealmCompat.where(realm, RealmAttestation.class)
                        .findAll();

                for (RealmAttestation rAtt : realmItems)
                {
                    try
                    {
                        if (rAtt.supportsChain(networkFilters))
                        {
                            long chainId = rAtt.getChains().get(0);
                            Attestation attn = (Attestation) fetchAttestation(chainId, wallet, rAtt);
                            if (attn.tokenInfo == null)
                            {
                                continue;
                            }
                            TokenCardMeta tcmAttestation = new TokenCardMeta(chainId,
                                    rAtt.getTokenAddress(), "1", System.currentTimeMillis(),
                                    svs, rAtt.getName(), attn.tokenInfo.symbol, attn.getBaseTokenType(), TokenGroup.ATTESTATION, attn.getAttestationUID());
                            tcmAttestation.isEnabled = true;
                            metaList.add(tcmAttestation);
                        }
                    }
                    catch (Exception e)
                    {
                        Timber.w(e);
                    }
                }
            }

            return metaList.toArray(new TokenCardMeta[0]);
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
                if (ticker != null && meta.hasPositiveBalance() && !meta.isNFT() && meta.group != TokenGroup.SPAM) //Currently we don't add NFT value. TODO: potentially get value from OpenSea
                {
                    Token t = fetchToken(chainId, wallet, meta.getAddress());
                    BigDecimal correctedBalance = t.getCorrectedBalance(18);
                    BigDecimal fiatValue = correctedBalance.multiply(new BigDecimal(ticker.price)).setScale(18, RoundingMode.DOWN);
                    historicalBalance = historicalBalance.add(fiatValue.add(fiatValue.multiply((new BigDecimal(ticker.percentChange24h).divide(hundred)).negate())));
                    newBalance = newBalance.add(fiatValue);
                }
            }

            storeWalletValue(wallet.address, newBalance.doubleValue());

            return new Pair<>(newBalance.doubleValue(), historicalBalance.doubleValue());
        });
    }

    private static String walletValueKey(String walletAddress)
    {
        return WALLET_VALUE_KEY_PREFIX + walletAddress.toLowerCase();
    }

    /**
     * Caches the wallet's last computed total value (in USD) so it can be displayed immediately
     * on startup/wallet-switch, before the fresh balance/ticker scan completes.
     */
    private void storeWalletValue(String walletAddress, double value)
    {
        if (TextUtils.isEmpty(walletAddress)) return;

        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            String key = walletValueKey(walletAddress);
            RealmCompat.executeTransaction(realm, r -> {
                RealmAuxData entry = RealmCompat.where(r, RealmAuxData.class)
                        .equalTo("instanceKey", key)
                        .findFirst();

                if (entry == null)
                {
                    entry = RealmCompat.createObject(r, RealmAuxData.class, key);
                }

                entry.setResult(String.valueOf(value));
                entry.setResultTime(System.currentTimeMillis());
                RealmCompat.insertOrUpdate(r, entry);
            });
        }
        catch (Exception e)
        {
            Timber.tag(TAG).e(e, "Failed to store wallet value for %s", walletAddress);
        }
    }

    /**
     * Returns the last cached total wallet value (in USD), or 0.0 if none has been stored yet.
     */
    @Override
    public double getStoredWalletValue(String walletAddress)
    {
        if (TextUtils.isEmpty(walletAddress)) return 0.0;

        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmAuxData entry = RealmCompat.where(realm, RealmAuxData.class)
                    .equalTo("instanceKey", walletValueKey(walletAddress))
                    .findFirst();

            if (entry == null || TextUtils.isEmpty(entry.getResult())) return 0.0;

            return Double.parseDouble(entry.getResult());
        }
        catch (Exception e)
        {
            return 0.0;
        }
    }

    private Map<Long, Map<String, TokenTicker>> fetchAllTokenTickers()
    {
        Map<Long, Map<String, TokenTicker>> tickerMap = new HashMap<>();
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.Results<RealmTokenTicker> realmTickers = RealmCompat.where(realm, RealmTokenTicker.class).findAll();

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
     *
     * @param wallet
     * @param svs
     * @return
     */
    @Override
    public Single<Integer> fixFullNames(Wallet wallet, AssetDefinitionService svs)
    {
        return Single.fromCallable(() -> {
            final int[] updated = {0};
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.Results<RealmToken> realmItems = RealmCompat.where(realm, RealmToken.class).sort("addedTime", RealmCompat.Sort.ASCENDING).like("address", ADDRESS_FORMAT).findAll();

                RealmCompat.executeTransaction(realm, r -> {
                    for (RealmToken t : realmItems)
                    {
                        RealmToken live = RealmCompat.latest(r, t);
                        String svsName = svs.getTokenName(live.getChainId(), live.getTokenAddress(), 1);
                        final String fullName = svsName != null ? svsName : live.getName();

                        if (!fullName.equals(live.getAuxData()))
                        {
                            live.setAuxData(fullName);
                            updated[0]++;
                        }
                    }
                });
            }

            return updated[0];
        });
    }

    /**
     * Fetches all TokenMeta currently in the database with search term, without fixing chain tokens if missing
     *
     * @param wallet
     * @param networkFilters
     * @return
     */
    @Override
    public Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Long> networkFilters, String searchTerm)
    {
        List<TokenCardMeta> tokenMetas = new ArrayList<>();
        return Single.fromCallable(() -> {
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.Results<RealmToken> realmItems = RealmCompat.where(realm, RealmToken.class).beginGroup().like("auxData", "*" + searchTerm + "*", RealmCompat.Case.INSENSITIVE).or().like("symbol", "*" + searchTerm + "*", RealmCompat.Case.INSENSITIVE).or().like("name", "*" + searchTerm + "*", RealmCompat.Case.INSENSITIVE).or().like("address", "*" + searchTerm + "*", RealmCompat.Case.INSENSITIVE).endGroup().like("address", ADDRESS_FORMAT).findAll();

                for (RealmToken t : realmItems)
                {
                    if (!networkFilters.isEmpty() && !networkFilters.contains(t.getChainId()))
                        continue;
                    String balance = convertStringBalance(t.getBalance(), t.getContractType());
                    TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), balance, t.getUpdateTime(), null, t.getAuxData(), t.getSymbol(), t.getContractType(), getTokenGroup(t.getChainId(), t.getTokenAddress(), t.getContractType()));
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
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(walletAddress)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.Results<RealmToken> realmItems = RealmCompat.where(realm, RealmToken.class) //TODO: Work out how to specify '?' in a Realm filter
                        .findAll();

                TokenFactory tf = new TokenFactory();
                for (RealmToken realmItem : realmItems)
                {
                    if (!networkFilters.isEmpty() && !networkFilters.contains(realmItem.getChainId())) continue;
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
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(walletAddress)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.Results<RealmToken> realmItems = RealmCompat.where(realm, RealmToken.class)
                        .like("address", ADDRESS_FORMAT)
                        .like("name", "")
                        .findAll();

                for (RealmToken realmItem : realmItems)
                {
                    if (!networkFilters.isEmpty() && !networkFilters.contains(realmItem.getChainId())) continue;
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
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
            Timber.w(e);
        }

        //This will trigger an update of the holder
        updateWalletTokens(tickerUpdates);
    }

    private void updateWalletTokens(final List<ContractAddress> tickerUpdates)
    {
        final String currentWallet = ethereumNetworkRepository.getCurrentWalletAddress();
        if (TextUtils.isEmpty(currentWallet)) return;

        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(currentWallet)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                for (ContractAddress contract : tickerUpdates)
                {
                    String contractAddress = contract.address.equals("eth") ? currentWallet : contract.address;
                    RealmToken realmToken = RealmCompat.where(r, RealmToken.class)
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
        if (erc20Tickers.isEmpty())
        {
            return;
        }

        List<ContractAddress> tickerUpdates = new ArrayList<>();
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            for (TokenCardMeta meta : erc20Tokens)
            {
                String databaseKey = databaseKey(chainId, meta.isEthereum() ? "eth" : meta.getAddress().toLowerCase());
                RealmTokenTicker realmItem = RealmCompat.where(realm, RealmTokenTicker.class)
                        .equalTo("contract", databaseKey)
                        .findFirst();

                if (realmItem != null)
                {
                    // how old is the update time?
                    long updateTime = realmItem.getUpdatedTime();
                    long age = System.currentTimeMillis() - updateTime;
                    if (age < TICKER_STALE_TIMEOUT) //ticker old, don't fetch
                    {
                        updateMap.put(meta.getAddress(), realmItem.getUpdatedTime());
                    }
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
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.Results<RealmTokenTicker> realmItems = RealmCompat.where(realm, RealmTokenTicker.class)
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                RealmCompat.Results<RealmTokenTicker> realmItems = RealmCompat.where(r, RealmTokenTicker.class)
                        //.lessThan("updatedTime", System.currentTimeMillis() - TICKER_TIMEOUT)
                        .findAll();

                realmItems.deleteAllFromRealm(r);
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmTokenTicker realmItem = RealmCompat.where(realm, RealmTokenTicker.class)
                    .equalTo("contract", key)
                    .findFirst();

            if (realmItem != null)
            {
                tt = convertRealmTicker(realmItem);
            }
        }

        return tt;
    }

    /**
     * Batch ticker lookup: opens a single Realm session and fetches tickers for all supplied keys,
     * avoiding the overhead of opening/closing a Realm instance for every individual token.
     */
    @Override
    public Map<String, TokenTicker> getCurrentTickers(List<String> keys)
    {
        Map<String, TokenTicker> result = new HashMap<>();
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            for (String key : keys)
            {
                RealmTokenTicker realmItem = RealmCompat.where(realm, RealmTokenTicker.class)
                        .equalTo("contract", key)
                        .findFirst();

                TokenTicker tt = convertRealmTicker(realmItem);
                if (tt != null)
                {
                    result.put(key, tt);
                }
            }
        }

        return result;
    }

    @Override
    public void removeOutdatedTickers()
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(TICKER_DB)))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                RealmCompat.Results<RealmTokenTicker> realmItems = RealmCompat.where(r, RealmTokenTicker.class)
                        .lessThan("updatedTime", System.currentTimeMillis() - TICKER_TIMEOUT)
                        .findAll();

                for (RealmTokenTicker data : realmItems)
                {
                    RealmCompat.deleteFromRealm(r, data);
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    private boolean writeTickerToRealm(TypedRealm realm, final TokenTicker ticker, long chainId, String tokenAddress)
    {
        if (ticker == null) return false;
        String databaseKey = databaseKey(chainId, tokenAddress.toLowerCase());
        RealmTokenTicker realmItem = RealmCompat.where(realm, RealmTokenTicker.class)
                .equalTo("contract", databaseKey)
                .findFirst();

        if (realmItem == null)
        {
            realmItem = RealmCompat.createObject(realm, RealmTokenTicker.class, databaseKey);
            realmItem.setCreatedTime(ticker.updateTime);
        }

        realmItem.setPercentChange24h(ticker.percentChange24h);
        realmItem.setPrice(ticker.price);
        realmItem.setImage(TextUtils.isEmpty(ticker.image)
                ? ""
                : ticker.image);
        realmItem.setUpdatedTime(ticker.updateTime);
        realmItem.setCurrencySymbol(ticker.priceSymbol);
        RealmCompat.insertOrUpdate(realm, realmItem);
        return true;
    }

    private Token convertSingle(RealmToken realmItem, TypedRealm realm, TokenFactory tf, Wallet wallet)
    {
        if (realmItem == null) return null;
        if (tf == null) tf = new TokenFactory();
        TokenInfo info = tf.createTokenInfo(realmItem);
        if (info.chainId <= 0)
        {
            info = new TokenInfo(info.address, info.name, info.symbol, info.decimals, info.isEnabled,
                    resolveChainId(realmItem));
        }
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(info.chainId);
        if (network == null) return null;

        String tokenAddress = realmItem.getTokenAddress();
        if (tokenAddress.equalsIgnoreCase("eth")
                || realmItem.getAddress().regionMatches(true, 0, "eth-", 0, 4))
        {
            info = new TokenInfo(wallet.address, info.name, info.symbol, info.decimals, info.isEnabled, info.chainId);
        }

        Token result = tf.createToken(info, realmItem, realmItem.getUpdateTime(), network.getShortName());
        result.setTokenWallet(wallet.address);
        if (tokenAddress.equalsIgnoreCase("eth")
                || realmItem.getAddress().regionMatches(true, 0, "eth-", 0, 4)
                || tokenAddress.equalsIgnoreCase(wallet.address))
        {
            result.setIsEthereum();
        }

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

    private void loadAssetContract(TypedRealm realm, Token token)
    {
        String databaseKey = databaseKey(token);
        RealmNFTAsset realmNFT = RealmCompat.where(realm, RealmNFTAsset.class)
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

    public TokenGroup getTokenGroup(long chainId, String address, ContractType type)
    {
        return tokensMappingRepository.getTokenGroup(chainId, address, type);
    }
}
