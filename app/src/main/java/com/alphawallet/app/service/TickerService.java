package com.alphawallet.app.service;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CRONOS_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GNOSIS_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.IOTEX_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.LINEA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MILKOMEDA_C1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OKX_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROOTSTOCK_MAINNET_ID;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;

import com.alphawallet.app.entity.CoinGeckoTicker;
import com.alphawallet.app.entity.DexGuruTicker;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.token.entity.EthereumReadBuffer;

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
import org.web3j.utils.Numeric;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
    private static final String MARKET_ORACLE_CONTRACT = "0xdAcAf435f241B1a062B021abEED9CA2F76F22F8D";
    private static final String CONTRACT_ADDR = "[CONTRACT_ADDR]";
    private static final String CHAIN_IDS = "[CHAIN_ID]";
    private static final String CURRENCY_TOKEN = "[CURRENCY]";
    private static final String COINGECKO_CHAIN_CALL = "https://api.coingecko.com/api/v3/simple/price?ids=" + CHAIN_IDS + "&vs_currencies=" + CURRENCY_TOKEN + "&include_24hr_change=true";
    private static final String COINGECKO_API = String.format("https://api.coingecko.com/api/v3/simple/token_price/%s?contract_addresses=%s&vs_currencies=%s&include_24hr_change=true",
            CHAIN_IDS, CONTRACT_ADDR, CURRENCY_TOKEN);
    private static final int    COINGECKO_MAX_FETCH = 10;
    private static final String DEXGURU_API = "https://api.dex.guru/v1/tokens/" + CONTRACT_ADDR + "-" + CHAIN_IDS;
    private static final String CURRENCY_CONV = "currency";
    private static final boolean ALLOW_UNVERIFIED_TICKERS = false; //allows verified:false tickers from DEX.GURU. Not recommended
    public static final long TICKER_TIMEOUT = DateUtils.WEEK_IN_MILLIS; //remove ticker if not seen in one week
    public static final long TICKER_STALE_TIMEOUT = 30 * DateUtils.MINUTE_IN_MILLIS; //Use market API if AlphaWallet market oracle not updating

    private final OkHttpClient httpClient;
    private final PreferenceRepositoryType sharedPrefs;
    private final TokenLocalSource localSource;
    private final Map<Long, TokenTicker> ethTickers = new ConcurrentHashMap<>();
    private double currentConversionRate = 0.0;
    private static String currentCurrencySymbolTxt;
    private static String currentCurrencySymbol;
    private static final ConcurrentLinkedDeque<TokenCardMeta> tokenCheckQueue = new ConcurrentLinkedDeque<>();
    private static final Map<String, TokenCardMeta> dexGuruQuery = new ConcurrentHashMap<>();
    private static long lastTickerUpdate;

    @Nullable
    private Disposable tickerUpdateTimer;

    @Nullable
    private Disposable erc20TickerCheck;

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
        lastTickerUpdate = 0;
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

    private void tickerUpdate()
    {
        mainTickerUpdate = updateCurrencyConversion()
                .flatMap(this::updateTickersFromOracle)
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

    private Single<Integer> updateTickersFromOracle(double conversionRate)
    {
        resetTickerUpdate();
        currentConversionRate = conversionRate;
        return Single.fromCallable(() -> {
            int tickerSize = 0;
            final Web3j web3j = TokenRepository.getWeb3jService(POLYGON_TEST_ID);
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

    private boolean alreadyInQueue(TokenCardMeta tcm)
    {
        for (TokenCardMeta thisTcm : tokenCheckQueue)
        {
            if (tcm.tokenId.equalsIgnoreCase(thisTcm.tokenId))
            {
                return true;
            }
        }

        return dexGuruQuery.containsKey(tcm.tokenId);
    }

    private List<TokenCardMeta> nextTickerSet(int count)
    {
        List<TokenCardMeta> tickerList = new ArrayList<>(count);
        long chainId = 0;
        if (tokenCheckQueue.size() > 0)
        {
            List<TokenCardMeta> addBack = new ArrayList<>();
            TokenCardMeta firstTcm = tokenCheckQueue.removeFirst();
            chainId = firstTcm.getChain();
            tickerList.add(firstTcm);
            count--;

            while (!tokenCheckQueue.isEmpty() && count > 0)
            {
                TokenCardMeta tcm = tokenCheckQueue.removeFirst();
                if (tcm.getChain() == chainId)
                {
                    tickerList.add(tcm);
                    count--;
                }
                else
                {
                    addBack.add(tcm);
                }
            }

            //Add back in any other TCM from other chainIds
            for (TokenCardMeta tcm : addBack)
            {
                tokenCheckQueue.addFirst(tcm);
            }
        }

        return tickerList;
    }

    public Single<Integer> syncERC20Tickers(long chainId, List<TokenCardMeta> erc20Tokens)
    {
        //add to queue here
        long staleTime = System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS;
        //only check networks with value and if there's actually tokens to check
        if (!EthereumNetworkRepository.hasRealValue(chainId) || erc20Tokens.isEmpty())
        {
            return Single.fromCallable(() -> 0);
        }

        Map<String, Long> currentTickerMap = localSource.getTickerTimeMap(chainId, erc20Tokens);

        //determine whether to add to checking queue
        for (TokenCardMeta tcm : erc20Tokens)
        {
            if ((!currentTickerMap.containsKey(tcm.getAddress())
                    || currentTickerMap.get(tcm.getAddress()) < staleTime)
                && !alreadyInQueue(tcm))
            {
                tokenCheckQueue.addLast(tcm);
            }
        }

        if (tokenCheckQueue.size() == 0)
        {
            return Single.fromCallable(() -> 0);
        }
        else
        {
            //start checking the queue
            return Single.fromCallable(this::doCoinGeckoCheck).map(count -> {
                beginTickerCheck();
                return count;
            });
        }
    }

    private int doCoinGeckoCheck()
    {
        //pull first token to get current chainId
        List<TokenCardMeta> tickerSet = nextTickerSet(COINGECKO_MAX_FETCH);
        Map<String, TokenTicker> tickerMap = fetchERC20TokenTickers(tickerSet);
        localSource.updateERC20Tickers(getChainId(tickerSet), tickerMap);
        if (tokenCheckQueue.isEmpty())
        {
            stopTickerCheck();
        }
        return tickerMap.size();
    }

    private void beginTickerCheck()
    {
        if (!tokenCheckQueue.isEmpty() && (erc20TickerCheck == null || erc20TickerCheck.isDisposed()))
        {
            erc20TickerCheck = Observable.interval(2, 2, TimeUnit.SECONDS)
                    .doOnNext(l -> doCoinGeckoCheck()).subscribe();
        }
    }

    private void stopTickerCheck()
    {
        if (erc20TickerCheck != null && !erc20TickerCheck.isDisposed())
        {
            erc20TickerCheck.dispose();
            erc20TickerCheck = null;
        }
    }

    private long getChainId(List<TokenCardMeta> erc20Tokens)
    {
        long chainId = 0;
        if (!erc20Tokens.isEmpty())
        {
            chainId = erc20Tokens.get(0).getChain();
        }

        return chainId;
    }

    private Map<String, TokenTicker> fetchERC20TokenTickers(List<TokenCardMeta> erc20Tokens)
    {
        final Map<String, TokenTicker> erc20Tickers = new HashMap<>();
        final long chainId = getChainId(erc20Tokens);
        if (chainId == 0) { return erc20Tickers; }

        final String apiChainName = coinGeckoChainIdToAPIName.get(chainId);
        final String dexGuruName = dexGuruChainIdToAPISymbol.get(chainId);

        if (apiChainName == null) { return erc20Tickers; }

        final Map<String, TokenCardMeta> lookupMap = new HashMap<>();

        //build ticker header
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (TokenCardMeta tcm : erc20Tokens)
        {
            lookupMap.put(tcm.getAddress().toLowerCase(), tcm);
            if (!isFirst) sb.append(",");
            sb.append(tcm.getAddress());
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
            dexGuruLookup = Observable.interval(500, 1000, TimeUnit.MILLISECONDS)
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
            for (Map.Entry<Long, String> entry : chainPairs.entrySet())
            {
                if (entry.getValue().equals("ethereum"))
                {
                    ethTickers.put(entry.getKey(), ticker);
                }
            }
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
            e.printStackTrace();
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

    private String callSmartContractFunction(Web3j web3j, Function function, String contractAddress)
    {
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
            Timber.e(e);
            return null;
        }
    }

    private static Function getTickers()
    {
        return new Function(
                "getTickers",
                Arrays.asList(),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>()
                {
                }));
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
        //canUpdate.clear();
        ethTickers.clear();
        tokenCheckQueue.clear();
        dexGuruQuery.clear();
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
        put(HECO_ID, "huobi-token");
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
        localSource.deleteTickers();
    }

    // Update from https://api.coingecko.com/api/v3/coins/list
    // If ticker is pegged against ethereum (L2's) then use 'ethereum' here.
    public static final Map<Long, String> chainPairs = new HashMap<>()
    {{
        put(MAINNET_ID, "ethereum");
        put(CLASSIC_ID, "ethereum-classic");
        put(GNOSIS_ID, "xdai");
        put(BINANCE_MAIN_ID, "binancecoin");
        put(HECO_ID, "huobi-token");
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
}
