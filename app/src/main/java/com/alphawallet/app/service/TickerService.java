package com.alphawallet.app.service;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_SIGMA1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.RINKEBY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.XDAI_ID;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.CoinGeckoTicker;
import com.alphawallet.app.entity.DexGuruTicker;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.token.entity.EthereumReadBuffer;
import com.alphawallet.token.tools.Numeric;

import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class TickerService
{
    private static final int UPDATE_TICKER_CYCLE = 5; //5 Minutes
    private static final String MEDIANIZER = "0x729D19f657BD0614b4985Cf1D82531c67569197B";
    private static final String MARKET_ORACLE_CONTRACT = "0xf155a7eb4a2993c8cf08a76bca137ee9ac0a01d8";
    private static final String CONTRACT_ADDR = "[CONTRACT_ADDR]";
    private static final String CHAIN_IDS = "[CHAIN_ID]";
    private static final String CURRENCY_TOKEN = "[CURRENCY]";
    private static final String COINGECKO_CHAIN_CALL = "https://api.coingecko.com/api/v3/simple/price?ids=" + CHAIN_IDS + "&vs_currencies=" + CURRENCY_TOKEN + "&include_24hr_change=true";
    private static final String COINGECKO_API = "https://api.coingecko.com/api/v3/simple/token_price/" + CHAIN_IDS + "?contract_addresses=" + CONTRACT_ADDR + "&vs_currencies=" + CURRENCY_TOKEN + "&include_24hr_change=true";
    private static final String DEXGURU_API = "https://api.dex.guru/v1/tokens/" + CONTRACT_ADDR + "-" + CHAIN_IDS;
    private static final String CURRENCY_CONV = "currency";
    private static final boolean ALLOW_UNVERIFIED_TICKERS = false; //allows verified:false tickers from DEX.GURU. Not recommended
    public static final long TICKER_TIMEOUT = DateUtils.WEEK_IN_MILLIS; //remove ticker if not seen in one week
    public static final long TICKER_STALE_TIMEOUT = 15 * DateUtils.MINUTE_IN_MILLIS; //try to use market API if AlphaWallet market oracle not updating

    private final OkHttpClient httpClient;
    private final PreferenceRepositoryType sharedPrefs;
    private final TokenLocalSource localSource;
    private final Map<Long, TokenTicker> ethTickers = new ConcurrentHashMap<>();
    private double currentConversionRate = 0.0;
    private static String currentCurrencySymbolTxt;
    private static String currentCurrencySymbol;
    private static final Map<Long, Long> canUpdate = new ConcurrentHashMap<>();
    private static final Map<String, TokenCardMeta> dexGuruQuery = new ConcurrentHashMap<>();

    @Nullable
    private Disposable tickerUpdateTimer;

    @Nullable
    private Disposable dexGuruLookup;

    @Nullable
    private Disposable mainTickerUpdate;

    public TickerService(OkHttpClient httpClient, PreferenceRepositoryType sharedPrefs, TokenLocalSource localSource)
    {
        this.httpClient = httpClient;
        this.sharedPrefs = sharedPrefs;
        this.localSource = localSource;

        resetTickerUpdate();
        initCurrency();
    }

    public void updateTickers()
    {
        if (mainTickerUpdate != null && !mainTickerUpdate.isDisposed())
        {
            return; //do not update if update is currently in progress
        }
        if (tickerUpdateTimer != null && !tickerUpdateTimer.isDisposed()) tickerUpdateTimer.dispose();
        sharedPrefs.commit();

        tickerUpdateTimer = Observable.interval(0, UPDATE_TICKER_CYCLE, TimeUnit.MINUTES)
                    .doOnNext(l -> tickerUpdate())
                    .subscribe();
    }

    private void tickerUpdate()
    {
        mainTickerUpdate = updateCurrencyConversion()
                .flatMap(this::updateTickersFromOracle)
                .flatMap(this::fetchTickersSeparatelyIfRequired)
                .flatMap(this::addArtisTicker)
                .map(this::checkTickers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::tickersUpdated, this::onTickersError);
    }

    private void tickersUpdated(int tickerCount)
    {
        Timber.d("Tickers Updated: " + tickerCount);
        mainTickerUpdate = null;
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
            if (tt != null) { return Double.parseDouble(tt.price); }
            else { return 0.0; }
        }
        else
        {
            TokenTicker currencyTicker = new TokenTicker(Double.toString(rate), "0", currentCurrencySymbolTxt, null, System.currentTimeMillis());
            localSource.updateERC20Tickers(0, new HashMap<String, TokenTicker>() {{ put(CURRENCY_CONV, currencyTicker); }});
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
            catch (IOException e)
            {
                Timber.e(e);
            }

            return tickers;
        });
    }

    private Single<Integer> updateTickersFromOracle(double conversionRate)
    {
        resetTickerUpdate();
        currentConversionRate = conversionRate;
        return Single.fromCallable(() -> {
            int tickerSize = 0;
            final Web3j web3j = TokenRepository.getWeb3jService(RINKEBY_ID);
            //fetch current tickers
            Function function = getTickers();
            String responseValue = callSmartContractFunction(web3j, function, MARKET_ORACLE_CONTRACT);
            List<Type> responseValues = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

            if (!responseValues.isEmpty())
            {
                Type T = responseValues.get(0);
                List<Uint256> values = (List) T.getValue();
                long tickerUpdateTime = values.get(0).getValue().longValue() * 1000L;

                if ((System.currentTimeMillis() - tickerUpdateTime) < TICKER_STALE_TIMEOUT)
                {
                    for (int i = 1; i < values.size(); i++)
                    {
                        //decode ticker values and populate
                        BigInteger tickerInfo = values.get(i).getValue();
                        addToTokenTickers(tickerInfo, tickerUpdateTime);
                        tickerSize++;
                    }
                }
            }

            return tickerSize;
        });
    }

    private static final int GREATEST_MAPSIZE = 75;

    public Single<Integer> syncERC20Tickers(long chainId, List<TokenCardMeta> erc20Tokens)
    {
        if (!canUpdate(chainId) || erc20Tokens.size() == 0)
            return Single.fromCallable(() -> 0);
        //this function called after sync is complete, only update tickers that need updating
        //first fetch the tickers for these
        long staleTime = System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS;
        Map<String, Long> currentTickerMap = localSource.getTickerTimeMap(chainId, erc20Tokens);
        final Map<String, TokenCardMeta> lookupMap = new HashMap<>();

        canUpdate.put(chainId, System.currentTimeMillis() + 15 * DateUtils.SECOND_IN_MILLIS);
        nextUpdate = System.currentTimeMillis() + 5 * DateUtils.SECOND_IN_MILLIS;

        for (TokenCardMeta tcm : erc20Tokens)
        {
            if (!dexGuruQuery.containsKey(tcm.tokenId) // don't include any token in the dexguru queue
                && (!currentTickerMap.containsKey(tcm.getAddress())
                    || currentTickerMap.get(tcm.getAddress()) < staleTime)) //include tokens who's tickers have gone stale
            {
                lookupMap.put(tcm.getAddress().toLowerCase(), tcm);
                if (lookupMap.size() > GREATEST_MAPSIZE) break;
            }
        }

        if (lookupMap.size() == 0) return Single.fromCallable(() -> 0);

        return Single.fromCallable(() -> {
            Map<String, TokenTicker> tickerMap = fetchERC20TokenTickers(chainId, lookupMap.values());
            localSource.updateERC20Tickers(chainId, tickerMap);
            return tickerMap.size();
        });
    }

    private long nextUpdate = 0;

    private boolean canUpdate(long chainId)
    {
        if (System.currentTimeMillis() < nextUpdate) return false;

        if (canUpdate.containsKey(chainId))
        {
            return System.currentTimeMillis() > canUpdate.get(chainId);
        }
        else
        {
            return true;
        }
    }

    private Map<String, TokenTicker> fetchERC20TokenTickers(long chainId, Collection<TokenCardMeta> erc20Tokens)
    {
        final String apiChainName = coinGeckoChainIdToAPIName.get(chainId);
        final String dexGuruName = dexGuruChainIdToAPISymbol.get(chainId);
        final Map<String, TokenTicker> erc20Tickers = new HashMap<>();
        if (apiChainName == null) return erc20Tickers;

        final Map<String, TokenCardMeta> lookupMap = new HashMap<>();
        for (TokenCardMeta tcm : erc20Tokens) { lookupMap.put(tcm.getAddress().toLowerCase(), tcm); }

        //build ticker header
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (TokenCardMeta t : erc20Tokens)
        {
            if (!isFirst) sb.append(",");
            sb.append(t.getAddress());
            isFirst = false;
        }

        Request request = new Request.Builder()
                .url(COINGECKO_API.replace(CHAIN_IDS, apiChainName).replace(CONTRACT_ADDR, sb.toString()).replace(CURRENCY_TOKEN, currentCurrencySymbolTxt))
                .get()
                .build();

        try (okhttp3.Response response = httpClient.newCall(request)
                .execute())
        {
            String responseStr = response.body().string();
            List<CoinGeckoTicker> tickers = CoinGeckoTicker.buildTickerList(responseStr, currentCurrencySymbolTxt, currentConversionRate);
            for (CoinGeckoTicker t : tickers)
            {
                //store ticker
                erc20Tickers.put(t.address, t.toTokenTicker(currentCurrencySymbolTxt));
                lookupMap.remove(t.address.toLowerCase());
            }

            if (dexGuruName != null)
            {
                addDexGuruTickers(lookupMap.values());
            }
            else
            {
                final Map<String, TokenTicker> blankTickers = new HashMap<>(); //These tokens have no ticker, don't check them again today
                for (String address : lookupMap.keySet())
                {
                    blankTickers.put(address, new TokenTicker(System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS));
                }
                localSource.updateERC20Tickers(chainId, blankTickers);
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return erc20Tickers;
    }

    private void addDexGuruTickers(Collection<TokenCardMeta> tokens)
    {
        for (TokenCardMeta tcm : tokens)
        {
            dexGuruQuery.put(tcm.tokenId, tcm);
        }

        if (dexGuruLookup == null || dexGuruLookup.isDisposed())
        {
            dexGuruLookup = Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                    .doOnNext(l -> getDexGuruTicker()).subscribe();
        }
    }

    private void getDexGuruTicker()
    {
        if (dexGuruQuery.keySet().iterator().hasNext())
        {
            String key = dexGuruQuery.keySet().iterator().next();
            TokenCardMeta tcm = dexGuruQuery.get(key);
            dexGuruQuery.remove(key);

            //fetch next token
            Request request = new Request.Builder()
                    .url(DEXGURU_API.replace(CHAIN_IDS, dexGuruChainIdToAPISymbol.get(tcm.getChain())).replace(CONTRACT_ADDR, tcm.getAddress()))
                    .get()
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request)
                    .execute())
            {
                if ((response.code() / 100) == 2 && response.body() != null)
                {
                    DexGuruTicker t = new DexGuruTicker(response.body().string());
                    if (t.verified || ALLOW_UNVERIFIED_TICKERS)
                    {
                        BigDecimal changeValue = new BigDecimal(t.usdChange).setScale(3, RoundingMode.DOWN);

                        TokenTicker tTicker = new TokenTicker(String.valueOf(t.usdPrice * currentConversionRate),
                                changeValue.toString(), currentCurrencySymbolTxt, "", System.currentTimeMillis());

                        localSource.updateERC20Tickers(tcm.getChain(), new HashMap<String, TokenTicker>()
                        {{
                            put(tcm.getAddress(), tTicker);
                        }});
                        return;
                    }
                }
                localSource.updateTicker(tcm.getChain(), tcm.getAddress(), new TokenTicker(System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS));
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }
        else
        {
            if (dexGuruLookup != null && !dexGuruLookup.isDisposed()) dexGuruLookup.dispose();
        }
    }

    private void checkPeggedTickers(long chainId, TokenTicker ticker)
    {
        if (chainId == MAINNET_ID)
        {
            ethTickers.put(ARBITRUM_MAIN_ID, ticker);
            ethTickers.put(OPTIMISTIC_MAIN_ID, ticker);
        }
    }

    private void addToTokenTickers(BigInteger tickerInfo, long tickerTime)
    {
        try
        {
            byte[] tickerData = Numeric.toBytesPadded(tickerInfo, 32);
            ByteArrayInputStream buffer = new ByteArrayInputStream(tickerData);
            EthereumReadBuffer ds = new EthereumReadBuffer(buffer);

            BigInteger chainId = ds.readBI(4);
            int changeVal = ds.readInt();
            BigInteger correctedPrice = ds.readBI(24);
            ds.close();

            BigDecimal changeValue = new BigDecimal(changeVal).movePointLeft(3);
            BigDecimal priceValue = new BigDecimal(correctedPrice).movePointLeft(12);

            double price = priceValue.doubleValue();

            TokenTicker tTicker = new TokenTicker(String.valueOf(price * currentConversionRate),
                    changeValue.setScale(3, RoundingMode.DOWN).toString(), currentCurrencySymbolTxt, "", tickerTime);

            ethTickers.put(chainId.longValue(), tTicker);
            checkPeggedTickers(chainId.longValue(), tTicker);
        }
        catch (Exception e)
        {
            e.printStackTrace();
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

    private Single<Integer> addArtisTicker(int tickerCount)
    {
        return convertPair("EUR", currentCurrencySymbolTxt)
                .flatMap(this::getSigmaTicker)
                .map(this::addArtisTickers)
                .map(ticker -> (tickerCount + 2));
    }

    private TokenTicker addArtisTickers(TokenTicker tokenTicker)
    {
        ethTickers.put(ARTIS_SIGMA1_ID, tokenTicker);
        return tokenTicker;
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
            if (!TextUtils.isEmpty(fiatChangeStr) && Character.isDigit(fiatChangeStr.charAt(0))) changeValue = BigDecimal.valueOf(eth.getDouble(currentCurrencySymbolTxt.toLowerCase() + "_24h_change"));

            tTicker = new TokenTicker(String.valueOf(fiatPrice),
                    changeValue.setScale(3, RoundingMode.DOWN).toString(), currentCurrencySymbolTxt, "", System.currentTimeMillis());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            tTicker = new TokenTicker();
        }

        return tTicker;
    }

    public Single<Double> convertPair(String currency1, String currency2)
    {
        return Single.fromCallable(() -> {
            if (currency1 == null || currency2 == null || currency1.equals(currency2)) return (Double)1.0;
            String conversionURL = "http://currencies.apps.grandtrunk.net/getlatest/" + currency1 + "/" + currency2;

            double rate = 0.0;

            Request request = new Request.Builder()
                    .url(conversionURL)
                    .addHeader("Connection","close")
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
                e.printStackTrace();
                rate = 0.0;
            }

            return rate;
        });
    }

    private String callSmartContractFunction(Web3j web3j,
                                             Function function, String contractAddress) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        try
        {
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(ZERO_ADDRESS, contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (IOException e)
        {
            //Connection error. Use cached value
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static Function getTickers() {
        return new Function(
                "getTickers",
                Arrays.asList(),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    /**
     * Potentially used by forks to add a custom ticker
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
                {{ put(address, ticker); }});
                return true;
            }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe().isDisposed();
        }
    }

    private Single<TokenTicker> getSigmaTicker(double rate)
    {
        return Single.fromCallable(() -> {
            String percentageChange = "0.00";
            double conversion = (1.0 / 13.7603) * rate; //13.7603 ATS = 1 EUR
            String price_usd = String.valueOf(conversion);
            String image = "https://artis.eco/i/favicon.png";
            return new TokenTicker(price_usd, percentageChange, currentCurrencySymbolTxt, image, System.currentTimeMillis());
        });
    }

    private void onTickersError(Throwable throwable)
    {
        mainTickerUpdate = null;
        throwable.printStackTrace();
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
        return BalanceUtils.getScaledValue(BigDecimal.valueOf(d), 2, 2);
    }

    private void initCurrency()
    {
        currentCurrencySymbolTxt = sharedPrefs.getDefaultCurrency();
        currentCurrencySymbol = sharedPrefs.getDefaultCurrencySymbol();
    }

    /**
     * Returns the current ISO currency string eg EUR, AUD etc.
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
        canUpdate.clear();
        dexGuruQuery.clear();
        ethTickers.clear();
    }

    public static final Map<Long, String> coinGeckoChainIdToAPIName = new HashMap<Long, String>(){{
        put(MAINNET_ID, "ethereum");
        put(XDAI_ID, "xdai");
        put(BINANCE_MAIN_ID, "binance-smart-chain");
        put(MATIC_ID, "polygon-pos");
        put(CLASSIC_ID, "ethereum-classic");
        put(FANTOM_ID, "fantom");
        put(AVALANCHE_ID, "avalanche");
        put(HECO_ID, "huobi-token");
        put(ARBITRUM_MAIN_ID, "arbitrum-one");
        put(66L, "okex-chain");
        put(1666600000L, "harmony-shard-0");
        put(321L, "kucoin-community-chain");
        put(88L, "tomochain");
        put(42220L, "celo");
    }};

    private static final Map<Long, String> dexGuruChainIdToAPISymbol = new HashMap<Long, String>(){{
        put(MAINNET_ID, "eth");
        put(BINANCE_MAIN_ID, "bsc");
        put(MATIC_ID, "polygon");
        put(AVALANCHE_ID, "avalanche");
    }};

    public void deleteTickers()
    {
        localSource.deleteTickers();
    }

    public static final Map<Long, String> chainPairs = new HashMap<Long, String>(){{
        put(MAINNET_ID, "ethereum");
        put(CLASSIC_ID, "ethereum-classic");
        put(POA_ID, "poa-network");
        put(XDAI_ID, "xdai");
        put(BINANCE_MAIN_ID, "binancecoin");
        put(HECO_ID, "huobi-token");
        put(AVALANCHE_ID, "avalanche-2");
        put(FANTOM_ID, "fantom");
        put(MATIC_ID, "matic-network");
        put(ARBITRUM_MAIN_ID, "ethereum");
        put(OPTIMISTIC_MAIN_ID, "ethereum");
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
        for (String cp : chainPairs.values())
        {
            if (!firstPair) tokenList.append(",");
            firstPair = false;
            tokenList.append(cp);
        }

        return COINGECKO_CHAIN_CALL.replace(CHAIN_IDS, tokenList.toString()).replace(CURRENCY_TOKEN, currentCurrencySymbolTxt);
    }

    private boolean receivedAllChainPairs()
    {
        for (long chainId : chainPairs.keySet())
        {
            if (!ethTickers.containsKey(chainId)) { return false; }
        }

        return true;
    }
}
