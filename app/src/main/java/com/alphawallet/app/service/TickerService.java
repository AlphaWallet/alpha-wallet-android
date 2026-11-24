package com.alphawallet.app.service;

import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BASE_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CRONOS_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GNOSIS_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.LINEA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MANTLE_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MILKOMEDA_C1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OKX_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROOTSTOCK_MAINNET_ID;

import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.realm.AutoCloseableRealm;
import com.alphawallet.app.realm.RealmCompat;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.util.BalanceUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.realm.kotlin.Realm;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class TickerService
{
    private final Subject<Integer> tickerUpdateComplete = PublishSubject.<Integer>create().toSerialized();
    private static final int UPDATE_TICKER_CYCLE = 5; //5 Minutes
    private static final String CONTRACT_ADDR = "[CONTRACT_ADDR]";
    private static final String CHAIN_IDS = "[CHAIN_ID]";
    private static final String CURRENCY_TOKEN = "[CURRENCY]";
    private static final String COINGECKO_CHAIN_CALL = "https://api.coingecko.com/api/v3/simple/price?ids=" + CHAIN_IDS + "&vs_currencies=" + CURRENCY_TOKEN + "&include_24hr_change=true";
    private static final String COINGECKO_API = String.format("https://api.coingecko.com/api/v3/simple/token_price/%s?contract_addresses=%s&vs_currencies=%s&include_24hr_change=true",
            CHAIN_IDS, CONTRACT_ADDR, CURRENCY_TOKEN);
    private static final int    COINGECKO_MAX_FETCH = 10;
    // private static final String DEXGURU_API = "https://api.dex.guru/v1/tokens/" + CONTRACT_ADDR + "-" + CHAIN_IDS;
    private static final String CURRENCY_CONV = "currency";
    // private static final boolean ALLOW_UNVERIFIED_TICKERS = false; //allows verified:false tickers from DEX.GURU. Not recommended
    public static final long TICKER_TIMEOUT = DateUtils.WEEK_IN_MILLIS; //remove ticker if not seen in one week
    public static final long TICKER_STALE_TIMEOUT = 30 * DateUtils.MINUTE_IN_MILLIS; //Use market API if AlphaWallet market oracle not updating
    private final KeyProvider keyProvider = KeyProviderFactory.get();
    private final OkHttpClient httpClient;
    private final PreferenceRepositoryType sharedPrefs;
    private final TokenLocalSource localSource;
    private final Map<Long, TokenTicker> ethTickers = new ConcurrentHashMap<>();
    private double currentConversionRate = 0.0;
    private static String currentCurrencySymbolTxt;
    private static String currentCurrencySymbol;
    private static long lastTickerUpdate;
    private static int keyCycle = 0;
    private static final String AW_HOST = "ticker.percolate.one";//"192.168.50.206:8088";// "percolate.one:8088"; "192.168.50.147:8088";
    private static final String FULL_AW_HOST = "https://" + AW_HOST;//"https://" + AW_HOST;
    private static final int AW_API_BATCH_SIZE = 20;
    private static final String TOKEN_LIST_PATH = "/token-list";
    private static final String TOKEN_LIST_ENTRY_KEY = "tokenListJson";
    private static final long TOKEN_LIST_UPDATE_INTERVAL = DateUtils.DAY_IN_MILLIS;

    /** Chain ID -> addresses the AW API doesn't have (no entry returned). Skip these in future requests. */
    private static final Map<Long, Set<String>> awApiTokensNotFound = new ConcurrentHashMap<>();

    /**
     * Chain ID -> set of known-token keys (first 8 + last 2 hex chars of the address, lowercase) returned by the
     * AW token-list API. Only chains present in the last successful response have an entry here; a chain with no
     * entry is treated as "unknown" and is not filtered (fail-open).
     */
    private final Map<Long, Set<String>> tokenListKeys = new ConcurrentHashMap<>();

    @Nullable
    private Disposable tickerUpdateTimer;

    @Nullable
    private Disposable mainTickerUpdate;

    @Nullable
    private Disposable tokenListUpdateTimer;

    @Nullable
    private Disposable tokenListDisposable;

    public TickerService(OkHttpClient httpClient, PreferenceRepositoryType sharedPrefs, TokenLocalSource localSource)
    {
        this.httpClient = httpClient;
        this.sharedPrefs = sharedPrefs;
        this.localSource = localSource;

        //deleteTickers();

        resetTickerUpdate();
        initCurrency();
        lastTickerUpdate = 0;

        startTokenListUpdate();
    }

    public void updateTickers()
    {
        if (mainTickerUpdate != null && !mainTickerUpdate.isDisposed() && System.currentTimeMillis() > (lastTickerUpdate + DateUtils.MINUTE_IN_MILLIS))
        {
            return; //do not update if update is currently in progress
        }
        if (tickerUpdateTimer != null && !tickerUpdateTimer.isDisposed()) tickerUpdateTimer.dispose();
        sharedPrefs.commit();

        tickerUpdateTimer = Observable.interval(0, UPDATE_TICKER_CYCLE, TimeUnit.MINUTES)
                .doOnNext(l -> tickerUpdate())
                .subscribe();
    }

    private void addAPIHeader(Request.Builder buildRequest)
    {
        String coinGeckoKey = keyProvider.getCoinGeckoKey();
        String backupKey = keyProvider.getBackupKey();

        if (!TextUtils.isEmpty(coinGeckoKey))
        {
            if (keyCycle%2 == 0)
            {
                buildRequest.addHeader("x-cg-demo-api-key", coinGeckoKey);
            }
            else if (keyCycle%2 == 1)
            {
                buildRequest.addHeader("x-cg-demo-api-key", backupKey);
            }
        }
    }

    private void tickerUpdate()
    {
        mainTickerUpdate = updateCurrencyConversion()
                .flatMap(this::fetchChainsFromAwApi)
                .flatMap(this::fetchTickersSeparatelyIfRequired)
                .map(this::checkTickers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::tickersUpdated, this::onTickersError);
    }

    private void tickersUpdated(int tickerCount)
    {
        Timber.d("Tickers Updated: %s", tickerCount);
        mainTickerUpdate = null;
        lastTickerUpdate = System.currentTimeMillis();
        tickerUpdateComplete.onNext(tickerCount);
    }

    public Observable<Integer> tickerUpdateComplete()
    {
        return tickerUpdateComplete.hide();
    }

    public Single<Double> updateCurrencyConversion()
    {
        initCurrency();
        return convertPair("USD", currentCurrencySymbolTxt)
                .map(this::storeCurrentRate);
    }

    private Double storeCurrentRate(Double rate)
    {
        if (rate == 0.0)
        {
            TokenTicker tt = localSource.getCurrentTicker(TokensRealmSource.databaseKey(0, CURRENCY_CONV));
            if (tt != null)
            {
                return Double.parseDouble(tt.price);
            }
            else
            {
                return 0.0;
            }
        }
        else
        {
            TokenTicker currencyTicker = new TokenTicker(Double.toString(rate), "0", currentCurrencySymbolTxt, null, System.currentTimeMillis());
            localSource.updateERC20Tickers(0, new HashMap<String, TokenTicker>()
            {{
                put(CURRENCY_CONV, currencyTicker);
            }});
            return rate;
        }
    }

    private Single<Integer> fetchTickersSeparatelyIfRequired(int tickerCount)
    {
        //check base chain tickers
        if (receivedAllChainPairs()) return Single.fromCallable(() -> tickerCount);
        else return fetchCoinGeckoChainPrices(); //fetch directly
    }

    private Single<Integer> fetchCoinGeckoChainPrices()
    {
        return Single.fromCallable(() -> {
            int tickers = 0;
            Request request = new Request.Builder()
                    .url(getCoinGeckoChainCall())
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute())
            {
                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();
                    JSONObject data = new JSONObject(result);
                    for (long chainId : chainPairs.keySet())
                    {
                        String chainSymbol = chainPairs.get(chainId);
                        if (!data.has(chainSymbol)) continue;
                        JSONObject tickerData = (JSONObject) data.get(chainSymbol);
                        TokenTicker tTicker = decodeCoinGeckoTicker(tickerData);
                        ethTickers.put(chainId, tTicker);
                        checkPeggedTickers(chainId, tTicker);
                        tickers++;
                    }
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return tickers;
        });
    }

    /**
     * Fetches base chain tickers (ETH, MATIC, etc.) from AW API.
     * Falls back to CoinGecko for any chains not returned.
     */
    private Single<Integer> fetchChainsFromAwApi(double conversionRate)
    {
        resetTickerUpdate();
        currentConversionRate = conversionRate;

        if (TextUtils.isEmpty(keyProvider.getAwApiKey()))
        {
            return Single.fromCallable(() -> 0);
        }

        return Single.fromCallable(() -> {
            List<Long> chainIds = new ArrayList<>(chainPairs.keySet());
            JSONObject body = getJsonObjectForChains(chainIds);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(FULL_AW_HOST + "/chains")
                    .post(requestBody)
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute())
            {
                if (response.code() / 100 != 2 || response.body() == null)
                {
                    return 0;
                }

                JSONObject json = new JSONObject(response.body().string());
                JSONArray results = json.getJSONArray("results");

                for (int i = 0; i < results.length(); i++)
                {
                    JSONObject item = results.getJSONObject(i);
                    if (item.has("error")) continue;

                    long chainId = item.optLong("chain", item.optLong("chainId", -1));
                    if (chainId < 0) continue;

                    double thisPrice = parseDouble(item.optString("price", "0"));
                    double change24h = parseDouble(item.optString("change24h", "0"));

                    TokenTicker tTicker = new TokenTicker(
                            String.valueOf(thisPrice * currentConversionRate),
                            String.valueOf(change24h),
                            currentCurrencySymbolTxt, "", System.currentTimeMillis());

                    ethTickers.put(chainId, tTicker);
                    checkPeggedTickers(chainId, tTicker);
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return ethTickers.size();
        });
    }

    private double parseDouble(String value)
    {
        if (TextUtils.isEmpty(value)) return 0.0;
        try
        {
            // Replace all characters except digits, decimal point, and minus sign
            String cleanValue = value.replaceAll("[^\\d.-]", "");
            return Double.parseDouble(cleanValue);
        }
        catch (NumberFormatException e)
        {
            return 0.0;
        }
    }

    @NonNull
    private JSONObject getJsonObjectForChains(List<Long> chainIds) throws JSONException
    {
        JSONArray chainsArray = new JSONArray();
        for (Long chainId : chainIds)
        {
            chainsArray.put(chainId);
        }

        long unixMinutes = System.currentTimeMillis() / 60000;
        String chainsStr = chainsArray.toString().replaceAll("[\\[\\]\"\\s]", "");
        String paramStr = "chains:[" + chainsStr + "]";
        String message = AW_HOST + "/chains/" + paramStr + "/" + unixMinutes;

        Timber.d("AW API chains message: %s", message);

        Credentials credentials = Credentials.create(keyProvider.getAwApiKey());
        Sign.SignatureData signatureData = Sign.signPrefixedMessage(
                message.getBytes(), credentials.getEcKeyPair());

        byte[] sigBytes = new byte[65];
        System.arraycopy(signatureData.getR(), 0, sigBytes, 0, 32);
        System.arraycopy(signatureData.getS(), 0, sigBytes, 32, 32);
        System.arraycopy(signatureData.getV(), 0, sigBytes, 64, 1);
        String sig = Numeric.toHexStringNoPrefix(sigBytes);

        JSONObject body = new JSONObject();
        body.put("chains", chainsArray);
        body.put("sig", sig);
        return body;
    }

    public Single<Integer> syncERC20Tickers(long chainId, List<TokenCardMeta> erc20Tokens)
    {
        //only check networks with value and if there's actually tokens to check
        if (!EthereumNetworkRepository.hasRealValue(chainId) || erc20Tokens.isEmpty())
        {
            return Single.fromCallable(() -> 0);
        }

        Map<String, Long> currentTickerMap = localSource.getTickerTimeMap(chainId, erc20Tokens);

        List<TokenCardMeta> tokensNeedingTickers = new ArrayList<>();
        for (TokenCardMeta tcm : erc20Tokens)
        {
            if (!currentTickerMap.containsKey(tcm.getAddress()))
            {
                tokensNeedingTickers.add(tcm);
            }
        }

        if (tokensNeedingTickers.isEmpty())
        {
            return Single.fromCallable(() -> 0);
        }

        return fetchAwApiTickersInBatches(chainId, tokensNeedingTickers)
                .flatMap(awTickers -> fetchCoinGeckoFallbackIfNeeded(chainId, tokensNeedingTickers, awTickers))
                .map(tickers -> {
                    if (!tickers.isEmpty())
                    {
                        localSource.updateERC20Tickers(chainId, tickers);
                    }
                    return tickers.size();
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * Fetches tickers from AW API in batches of 20 tokens per call.
     * Skips addresses we already know the service doesn't have.
     */
    private Single<Map<String, TokenTicker>> fetchAwApiTickersInBatches(long chainId, List<TokenCardMeta> tokens)
    {
        if (TextUtils.isEmpty(keyProvider.getAwApiKey()))
        {
            return Single.fromCallable(HashMap::new);
        }

        Set<String> notFound = getAwApiTokensNotFoundForChain(chainId);
        List<TokenCardMeta> tokensToFetch = new ArrayList<>();
        for (TokenCardMeta tcm : tokens)
        {
            String addr = tcm.getAddress().toLowerCase();
            if (!notFound.contains(addr) && isTokenOnKnownList(chainId, addr))
            {
                tokensToFetch.add(tcm);
            }
        }

        if (tokensToFetch.isEmpty())
        {
            return Single.fromCallable(HashMap::new);
        }

        return Single.fromCallable(() -> {
            Map<String, TokenTicker> allTickers = new HashMap<>();
            for (int i = 0; i < tokensToFetch.size(); i += AW_API_BATCH_SIZE)
            {
                int end = Math.min(i + AW_API_BATCH_SIZE, tokensToFetch.size());
                List<TokenCardMeta> batch = tokensToFetch.subList(i, end);
                Map<String, TokenTicker> batchTickers = fetchAwApiTickersBatch(batch, chainId);
                recordAwApiTokensNotFound(chainId, batch, batchTickers);
                allTickers.putAll(batchTickers);
            }
            return allTickers;
        }).subscribeOn(Schedulers.io());
    }

    private Set<String> getAwApiTokensNotFoundForChain(long chainId)
    {
        return awApiTokensNotFound.computeIfAbsent(chainId, k -> ConcurrentHashMap.newKeySet());
    }

    /**
     * Records addresses we requested but the AW API didn't return (no entry = doesn't have it).
     */
    private void recordAwApiTokensNotFound(long chainId, List<TokenCardMeta> requested, Map<String, TokenTicker> received)
    {
        Set<String> notFound = getAwApiTokensNotFoundForChain(chainId);
        for (TokenCardMeta tcm : requested)
        {
            String addr = tcm.getAddress().toLowerCase();
            if (!received.containsKey(addr))
            {
                notFound.add(addr);
            }
        }
    }

    ///////////////////////////////////////////
    // Token list - once-a-day allow-list of addresses the AW ticker API actually has data for.
    // Used to avoid wasting signed AW API requests on tokens it doesn't cover.

    /**
     * Starts the once-a-day check for a new token list. Fires immediately on startup (which will
     * load from cache without hitting the network if the cached copy is still fresh), then once every
     * 24 hours afterwards.
     */
    private void startTokenListUpdate()
    {
        if (tokenListUpdateTimer != null && !tokenListUpdateTimer.isDisposed())
        {
            return;
        }

        tokenListUpdateTimer = Observable.interval(0, 1, TimeUnit.DAYS)
                .doOnNext(l -> checkTokenListUpdate())
                .subscribe();
    }

    private void checkTokenListUpdate()
    {
        if (tokenListDisposable != null && !tokenListDisposable.isDisposed())
        {
            return;
        }

        tokenListDisposable = Single.fromCallable(this::refreshTokenListIfRequired)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(count -> Timber.d("Token list keys loaded: %s", count), Timber::e);
    }

    /**
     * Loads the cached token list into memory (cheap, no network) if it hasn't been loaded yet, then
     * fetches a fresh copy from the AW API if the cached copy is more than a day old.
     *
     * @return the number of chains currently held in the in-memory token list
     */
    private int refreshTokenListIfRequired()
    {
        if (tokenListKeys.isEmpty())
        {
            String cachedJson = getStoredTokenListJson();
            if (!TextUtils.isEmpty(cachedJson))
            {
                parseTokenListJson(cachedJson);
            }
        }

        if (System.currentTimeMillis() - getStoredTokenListTime() < TOKEN_LIST_UPDATE_INTERVAL)
        {
            return tokenListKeys.size(); //not due for a refresh yet
        }

        String json = fetchTokenListFromApi();
        if (!TextUtils.isEmpty(json))
        {
            parseTokenListJson(json);
            storeTokenListJson(json);
        }

        return tokenListKeys.size();
    }

    /**
     * @return true if this chain has no known-token data (fail-open, don't restrict), or if the address
     * is present in the chain's known-token list.
     */
    private boolean isTokenOnKnownList(long chainId, String address)
    {
        Set<String> keys = tokenListKeys.get(chainId);
        if (keys == null)
        {
            return true;
        }
        return keys.contains(tokenListKey(address));
    }

    /**
     * The token-list API identifies addresses by the first 8 and last 2 hex characters (10 chars total,
     * lowercase, no "0x" prefix).
     */
    private static String tokenListKey(String address)
    {
        String addr = address.toLowerCase();
        if (addr.startsWith("0x"))
        {
            addr = addr.substring(2);
        }
        if (addr.length() < 10)
        {
            return addr;
        }
        return addr.substring(0, 8) + addr.substring(addr.length() - 2);
    }

    @Nullable
    private String fetchTokenListFromApi()
    {
        if (TextUtils.isEmpty(keyProvider.getAwApiKey()))
        {
            return null;
        }

        try
        {
            JSONObject body = getJsonObjectForTokenList();

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(FULL_AW_HOST + TOKEN_LIST_PATH)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute())
            {
                if (response.code() / 100 != 2 || response.body() == null)
                {
                    return null;
                }

                JSONObject json = new JSONObject(response.body().string());
                return json.has("data") ? json.getJSONObject("data").toString() : null;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
            return null;
        }
    }

    @NonNull
    private JSONObject getJsonObjectForTokenList() throws JSONException
    {
        long unixMinutes = System.currentTimeMillis() / 60000;
        String message = AW_HOST + TOKEN_LIST_PATH + "//" + unixMinutes;

        Timber.d("AW API token-list message: %s", message);

        Credentials credentials = Credentials.create(keyProvider.getAwApiKey());
        Sign.SignatureData signatureData = Sign.signPrefixedMessage(
                message.getBytes(), credentials.getEcKeyPair());

        byte[] sigBytes = new byte[65];
        System.arraycopy(signatureData.getR(), 0, sigBytes, 0, 32);
        System.arraycopy(signatureData.getS(), 0, sigBytes, 32, 32);
        System.arraycopy(signatureData.getV(), 0, sigBytes, 64, 1);
        String sig = Numeric.toHexStringNoPrefix(sigBytes);

        JSONObject body = new JSONObject();
        body.put("sig", sig);
        return body;
    }

    /**
     * Parses the "data" object of the token-list response: chain ID (as string) -> array of known-token keys.
     */
    private void parseTokenListJson(String dataJson)
    {
        try
        {
            JSONObject data = new JSONObject(dataJson);
            Map<Long, Set<String>> newMap = new HashMap<>();
            Iterator<String> chainKeys = data.keys();
            while (chainKeys.hasNext())
            {
                String chainKeyStr = chainKeys.next();
                long chainId;
                try
                {
                    chainId = Long.parseLong(chainKeyStr);
                }
                catch (NumberFormatException e)
                {
                    continue;
                }

                JSONArray addressKeys = data.getJSONArray(chainKeyStr);
                Set<String> keySet = new HashSet<>();
                for (int i = 0; i < addressKeys.length(); i++)
                {
                    keySet.add(addressKeys.getString(i).toLowerCase());
                }
                newMap.put(chainId, keySet);
            }

            tokenListKeys.clear();
            tokenListKeys.putAll(newMap);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    @Nullable
    private String getStoredTokenListJson()
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(localSource.getTickerRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmAuxData entry = RealmCompat.where(realm, RealmAuxData.class)
                    .equalTo("instanceKey", TOKEN_LIST_ENTRY_KEY)
                    .findFirst();
            return entry != null ? entry.getResult() : null;
        }
        catch (Exception e)
        {
            Timber.e(e);
            return null;
        }
    }

    private long getStoredTokenListTime()
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(localSource.getTickerRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmAuxData entry = RealmCompat.where(realm, RealmAuxData.class)
                    .equalTo("instanceKey", TOKEN_LIST_ENTRY_KEY)
                    .findFirst();
            return entry != null ? entry.getResultTime() : 0;
        }
        catch (Exception e)
        {
            Timber.e(e);
            return 0;
        }
    }

    private void storeTokenListJson(String json)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(localSource.getTickerRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                RealmAuxData entry = RealmCompat.where(r, RealmAuxData.class)
                        .equalTo("instanceKey", TOKEN_LIST_ENTRY_KEY)
                        .findFirst();

                if (entry == null)
                {
                    entry = RealmCompat.createObject(r, RealmAuxData.class, TOKEN_LIST_ENTRY_KEY);
                }

                entry.setResult(json);
                entry.setResultTime(System.currentTimeMillis());
                RealmCompat.insertOrUpdate(r, entry);
            });
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    /**
     * Fallback to CoinGecko for tokens not found in AW API response.
     * Kept for users who want to build their own without AW API access.
     */
    private Single<Map<String, TokenTicker>> fetchCoinGeckoFallbackIfNeeded(long chainId,
            List<TokenCardMeta> tokensNeedingTickers, Map<String, TokenTicker> awTickers)
    {
        List<TokenCardMeta> missingTokens = new ArrayList<>();
        for (TokenCardMeta tcm : tokensNeedingTickers)
        {
            String addr = tcm.getAddress().toLowerCase();
            if (!awTickers.containsKey(addr) && coinGeckoChainIdToAPIName.containsKey(chainId))
            {
                missingTokens.add(tcm);
            }
        }
        if (missingTokens.isEmpty())
        {
            return Single.just(awTickers);
        }
        return fetchCoinGeckoTokenPrices(chainId, missingTokens)
                .map(cgTickers -> {
                    Map<String, TokenTicker> merged = new HashMap<>(awTickers);
                    merged.putAll(cgTickers);
                    return merged;
                })
                .onErrorReturnItem(awTickers);
    }

    private Map<String, TokenTicker> fetchAwApiTickersBatch(List<TokenCardMeta> batch, long chainId)
    {
        Map<String, TokenTicker> tickersMap = new HashMap<>();
        try
        {
            JSONObject body = getJsonObjectForBatch(batch, chainId);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json"));

            Request.Builder buildRequest = new Request.Builder()
                    .url(FULL_AW_HOST + "/tokens")
                    .post(requestBody);

            try (okhttp3.Response response = httpClient.newCall(buildRequest.build())
                    .execute())
            {
                int code = response.code();
                if (code / 100 != 2)
                {
                    return tickersMap;
                }

                JSONObject json = new JSONObject(response.body().string());
                JSONArray results = json.getJSONArray("results");

                for (int i = 0; i < results.length(); i++)
                {
                    JSONObject item = results.getJSONObject(i);
                    if (item.has("error")) continue;

                    String address = item.getString("address").toLowerCase();
                    double thisPrice = parseDouble(item.optString("price", "0"));
                    double change24h = parseDouble(item.optString("change24h", "0"));

                    TokenTicker tTicker = new TokenTicker(String.valueOf(thisPrice * currentConversionRate),
                            String.valueOf(change24h), currentCurrencySymbolTxt, "", System.currentTimeMillis());

                    tickersMap.put(address, tTicker);
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return tickersMap;
    }

    @NonNull
    private JSONObject getJsonObjectForBatch(List<TokenCardMeta> batch, long chainId) throws JSONException
    {
        JSONArray addressArray = new JSONArray();
        for (TokenCardMeta tcm : batch)
        {
            addressArray.put(tcm.getAddress().toLowerCase());
        }

        long unixMinutes = System.currentTimeMillis() / 60000;
        String paramStr = "addresses:" + addressArray.toString().replaceAll("[\"\\s]", "")
                + ",chain:" + chainId;
        String message = AW_HOST + "/tokens/" + paramStr + "/" + unixMinutes;

        Timber.d("AW API message: %s", message);

        // Sign with EIP-191 personal_sign
        Credentials credentials = Credentials.create(keyProvider.getAwApiKey());
        Sign.SignatureData signatureData = Sign.signPrefixedMessage(
                message.getBytes(), credentials.getEcKeyPair());

        byte[] sigBytes = new byte[65];
        System.arraycopy(signatureData.getR(), 0, sigBytes, 0, 32);
        System.arraycopy(signatureData.getS(), 0, sigBytes, 32, 32);
        System.arraycopy(signatureData.getV(), 0, sigBytes, 64, 1);
        String sig = Numeric.toHexStringNoPrefix(sigBytes);

        JSONObject body = new JSONObject();
        body.put("chain", chainId);
        body.put("addresses", addressArray);
        body.put("sig", sig);
        return body;
    }

    /**
     * CoinGecko fallback for ERC20 token prices. Used when AW API returns no data or for forks.
     */
    private Single<Map<String, TokenTicker>> fetchCoinGeckoTokenPrices(long chainId, List<TokenCardMeta> tokens)
    {
        String apiChainName = coinGeckoChainIdToAPIName.get(chainId);
        if (apiChainName == null) return Single.just(new HashMap<>());

        return Single.fromCallable(() -> {
            Map<String, TokenTicker> tickersMap = new HashMap<>();
            for (int i = 0; i < tokens.size(); i += COINGECKO_MAX_FETCH)
            {
                int end = Math.min(i + COINGECKO_MAX_FETCH, tokens.size());
                List<TokenCardMeta> batch = tokens.subList(i, end);
                StringBuilder addresses = new StringBuilder();
                for (int j = 0; j < batch.size(); j++)
                {
                    if (j > 0) addresses.append(",");
                    addresses.append(batch.get(j).getAddress().toLowerCase());
                }
                String url = COINGECKO_API.replace(CHAIN_IDS, apiChainName)
                        .replace(CONTRACT_ADDR, addresses.toString())
                        .replace(CURRENCY_TOKEN, currentCurrencySymbolTxt);

                Request.Builder requestBuilder = new Request.Builder().url(url).get();
                addAPIHeader(requestBuilder);

                try (Response response = httpClient.newCall(requestBuilder.build()).execute())
                {
                    if (response.code() / 100 != 2 || response.body() == null) continue;

                    JSONObject data = new JSONObject(response.body().string());
                    for (TokenCardMeta tcm : batch)
                    {
                        String addr = tcm.getAddress().toLowerCase();
                        if (data.has(addr))
                        {
                            JSONObject tickerData = data.getJSONObject(addr);
                            TokenTicker tt = decodeCoinGeckoTicker(tickerData);
                            tickersMap.put(addr, tt);
                        }
                    }
                }
                catch (Exception e)
                {
                    Timber.e(e);
                }
            }
            return tickersMap;
        });
    }

    private void checkPeggedTickers(long chainId, TokenTicker ticker)
    {
        if (chainId == MAINNET_ID)
        {
            for (Map.Entry<Long, String> entry : chainPairs.entrySet())
            {
                if (entry.getValue().equals("ethereum"))
                {
                    ethTickers.put(entry.getKey(), ticker);
                }
            }
        }
    }

    private int checkTickers(int tickerSize)
    {
        Timber.d("Tickers received: %s", tickerSize);
        //store ticker values. If values have changed then update the token's update time so the wallet view will update
        localSource.updateEthTickers(ethTickers);
        //localSource.removeOutdatedTickers();
        return tickerSize;
    }

    public TokenTicker getEthTicker(long chainId)
    {
        return ethTickers.get(chainId);
    }

    private TokenTicker decodeCoinGeckoTicker(JSONObject eth)
    {
        TokenTicker tTicker;
        try
        {
            BigDecimal changeValue = BigDecimal.ZERO;
            double fiatPrice = 0.0;
            String fiatChangeStr = "0.0";
            if (eth.has(currentCurrencySymbolTxt.toLowerCase()))
            {
                fiatPrice = eth.getDouble(currentCurrencySymbolTxt.toLowerCase());
                fiatChangeStr = eth.getString(currentCurrencySymbolTxt.toLowerCase() + "_24h_change");
            }
            else
            {
                fiatPrice = eth.getDouble("usd") * currentConversionRate;
                fiatChangeStr = eth.getString("usd_24h_change");
            }
            if (!TextUtils.isEmpty(fiatChangeStr) && Character.isDigit(fiatChangeStr.charAt(0)))
                changeValue = BigDecimal.valueOf(eth.getDouble(currentCurrencySymbolTxt.toLowerCase() + "_24h_change"));

            tTicker = new TokenTicker(String.valueOf(fiatPrice),
                    changeValue.setScale(3, RoundingMode.DOWN).toString(), currentCurrencySymbolTxt, "", System.currentTimeMillis());
        }
        catch (Exception e)
        {
            tTicker = new TokenTicker();
        }

        return tTicker;
    }

    public Single<Double> convertPair(String currency1, String currency2)
    {
        return Single.fromCallable(() -> {
            if (currency1 == null || currency2 == null || currency1.equals(currency2)) return (Double) 1.0;
            String conversionURL = "http://currencies.apps.grandtrunk.net/getlatest/" + currency1 + "/" + currency2;

            double rate = 0.0;

            Request request = new Request.Builder()
                    .url(conversionURL)
                    .addHeader("Connection", "close")
                    .get()
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute())
            {
                int resultCode = response.code();
                if ((resultCode / 100) == 2 && response.body() != null)
                {
                    String responseBody = response.body().string();
                    rate = Double.parseDouble(responseBody);
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
                rate = 0.0;
            }

            return rate;
        });
    }

    /**
     * Potentially used by forks to add a custom ticker
     *
     * @param chainId
     * @param ticker
     */
    @SuppressWarnings("unused")
    public void addCustomTicker(long chainId, TokenTicker ticker)
    {
        if (ticker != null)
        {
            ethTickers.put(chainId, ticker);
        }
    }

    /**
     * Potentially used by forks
     *
     * @param chainId
     * @param address
     * @param ticker
     */
    @SuppressWarnings("unused")
    public void addCustomTicker(long chainId, String address, TokenTicker ticker)
    {
        if (ticker != null && address != null)
        {
            Single.fromCallable(() -> {
                        localSource.updateERC20Tickers(chainId, new HashMap<String, TokenTicker>()
                        {{
                            put(address, ticker);
                        }});
                        return true;
                    }).subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe().isDisposed();
        }
    }

    private void onTickersError(Throwable throwable)
    {
        mainTickerUpdate = null;
        Timber.e(throwable);
    }

    public static String getFullCurrencyString(double price)
    {
        return getCurrencyString(price) + " " + currentCurrencySymbolTxt;
    }

    public static String getCurrencyString(double price)
    {
        return BalanceUtils.genCurrencyString(price, currentCurrencySymbol);
    }

    public static String getCurrencyWithoutSymbol(double price)
    {
        return BalanceUtils.genCurrencyString(price, "");
    }

    public static String getPercentageConversion(double d)
    {
        return BalanceUtils.getScaledValue(BigDecimal.valueOf(d), 0, 2);
    }

    private void initCurrency()
    {
        currentCurrencySymbolTxt = sharedPrefs.getDefaultCurrency();
        currentCurrencySymbol = sharedPrefs.getDefaultCurrencySymbol();
    }

    /**
     * Returns the current ISO currency string eg EUR, AUD etc.
     *
     * @return 3 character currency ISO text
     */
    public static String getCurrencySymbolTxt()
    {
        return currentCurrencySymbolTxt;
    }

    public static String getCurrencySymbol()
    {
        return currentCurrencySymbol;
    }

    public double getCurrentConversionRate()
    {
        return currentConversionRate;
    }

    private void resetTickerUpdate()
    {
        ethTickers.clear();
    }

    // Update this list from here: https://api.coingecko.com/api/v3/asset_platforms
    public static final Map<Long, String> coinGeckoChainIdToAPIName = new HashMap<>()
    {{
        put(MAINNET_ID, "ethereum");
        put(GNOSIS_ID, "xdai");
        put(BINANCE_MAIN_ID, "binance-smart-chain");
        put(POLYGON_ID, "polygon-pos");
        put(CLASSIC_ID, "ethereum-classic");
        put(FANTOM_ID, "fantom");
        put(AVALANCHE_ID, "avalanche");
        put(ARBITRUM_MAIN_ID, "arbitrum-one");
        put(OKX_ID, "okex-chain");
        put(1666600000L, "harmony-shard-0");
        put(321L, "kucoin-community-chain");
        put(88L, "tomochain");
        put(42220L, "celo");
        put(KLAYTN_ID, "klay-token");
        put(IOTEX_MAINNET_ID, "iotex");
        put(AURORA_MAINNET_ID, "aurora");
        put(MILKOMEDA_C1_ID, "cardano");
        put(CRONOS_MAIN_ID, "cronos");
        put(ROOTSTOCK_MAINNET_ID, "rootstock");
        put(LINEA_ID, "linea");
        put(BASE_MAINNET_ID, "base");
        put(MANTLE_MAINNET_ID, "mantle");
    }};

    // For now, don't use Dexguru unless we obtain API key
    private static final Map<Long, String> dexGuruChainIdToAPISymbol = new HashMap<Long, String>()
    {{
        //put(MAINNET_ID, "eth");
        //put(BINANCE_MAIN_ID, "bsc");
        //put(POLYGON_ID, "polygon");
        //put(AVALANCHE_ID, "avalanche");
    }};

    public void deleteTickers()
    {
        Single.fromCallable(() -> {
            localSource.deleteTickers();
            return true;
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe();
    }

    // Update from https://api.coingecko.com/api/v3/coins/list
    // If ticker is pegged against ethereum (L2's) then use 'ethereum' here.
    public static final Map<Long, String> chainPairs = new HashMap<>()
    {{
        put(MAINNET_ID, "ethereum");
        put(CLASSIC_ID, "ethereum-classic");
        put(GNOSIS_ID, "xdai");
        put(BINANCE_MAIN_ID, "binancecoin");
        put(AVALANCHE_ID, "avalanche-2");
        put(FANTOM_ID, "fantom");
        put(POLYGON_ID, "matic-network");
        put(ARBITRUM_MAIN_ID, "ethereum");
        put(OPTIMISTIC_MAIN_ID, "ethereum");
        put(KLAYTN_ID, "klay-token");
        put(IOTEX_MAINNET_ID, "iotex");
        put(AURORA_MAINNET_ID, "aurora");
        put(MILKOMEDA_C1_ID, "cardano");
        put(CRONOS_MAIN_ID, "crypto-com-chain");
        put(OKX_ID, "okb");
        put(ROOTSTOCK_MAINNET_ID, "rootstock");
        put(LINEA_ID, "ethereum");
        put(BASE_MAINNET_ID, "base");
        put(MANTLE_MAINNET_ID, "mantle");
    }};

    public static boolean validateCoinGeckoAPI(Token token)
    {
        if (token.isEthereum() && chainPairs.containsKey(token.tokenInfo.chainId)) return true;
        else if ((!token.isEthereum() && !token.isNonFungible()) && coinGeckoChainIdToAPIName.containsKey(token.tokenInfo.chainId)) return true;
        else return false;
    }

    private String getCoinGeckoChainCall()
    {
        StringBuilder tokenList = new StringBuilder();
        boolean firstPair = true;
        for (long chainId : chainPairs.keySet())
        {
            if (ethTickers.containsKey(chainId))
            {
                continue;
            }
            if (!firstPair) tokenList.append(",");
            firstPair = false;
            tokenList.append(chainPairs.get(chainId));
        }

        return COINGECKO_CHAIN_CALL.replace(CHAIN_IDS, tokenList.toString()).replace(CURRENCY_TOKEN, currentCurrencySymbolTxt);
    }

    private boolean receivedAllChainPairs()
    {
        for (long chainId : chainPairs.keySet())
        {
            if (!ethTickers.containsKey(chainId))
            {
                return false;
            }
        }

        return true;
    }

    //Store received ticker if required
    public void storeTickers(long chainId, Map<String, TokenTicker> tickerMap)
    {
        //if ticker not found or out of date update the price
        //ticker up to date?
        Map<String, TokenTicker> tickerUpdateMap = new HashMap<>();
        for (String key : tickerMap.keySet())
        {
            String dbKey = TokensRealmSource.databaseKey(chainId, key);
            TokenTicker fromDb = localSource.getCurrentTicker(dbKey);
            if (fromDb == null || fromDb.getTickerAgeMillis() > TICKER_STALE_TIMEOUT)
            {
                tickerUpdateMap.put(key, tickerMap.get(key));
            }
        }

        if (!tickerUpdateMap.isEmpty())
        {
            localSource.updateERC20Tickers(chainId, tickerUpdateMap);
        }
    }
}
