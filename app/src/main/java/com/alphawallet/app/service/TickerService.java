package com.alphawallet.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.CoinGeckoTicker;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.token.entity.EthereumReadBuffer;
import com.alphawallet.token.tools.Numeric;
import com.google.gson.Gson;

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
import java.text.DecimalFormat;
import java.util.Arrays;
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
import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.SharedPreferenceRepository.CURRENCY_CODE_KEY;
import static com.alphawallet.app.repository.SharedPreferenceRepository.CURRENCY_SYMBOL_KEY;
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

import androidx.preference.PreferenceManager;

public class TickerService
{
    private static final int UPDATE_TICKER_CYCLE = 5; //5 Minutes
    private static final String MEDIANIZER = "0x729D19f657BD0614b4985Cf1D82531c67569197B";
    private static final String BLOCKSCOUT = "https://blockscout.com/poa/[CORE]/api?module=stats&action=ethprice";
    private static final String MARKET_ORACLE_CONTRACT = "0xf155a7eb4a2993c8cf08a76bca137ee9ac0a01d8";
    private static final String CONTRACT_ADDR = "[CONTRACT_ADDR]";
    private static final String CHAIN_ID = "[CHAIN_ID]";
    private static final String COINGECKO_API = "https://api.coingecko.com/api/v3/simple/token_price/" + CHAIN_ID + "?contract_addresses=" +CONTRACT_ADDR + "&vs_currencies=USD&include_24hr_change=true";
    private static final String COINGECKO_COINS_API = "https://api.coingecko.com/api/v3/simple/price?ids=ethereum%2Cxdai%2Cetc&vs_currencies=USD&include_24hr_change=true";
    private static final String CURRENCY_CONV = "currency";

    public static final long TICKER_TIMEOUT = DateUtils.HOUR_IN_MILLIS; //remove ticker if not seen in one hour
    public static final long TICKER_STALE_TIMEOUT = 15 * DateUtils.MINUTE_IN_MILLIS; //try to use market API if AlphaWallet market oracle not updating

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Context context;
    private final TokenLocalSource localSource;
    private final Map<Integer, TokenTicker> ethTickers = new ConcurrentHashMap<>();
    private Disposable tickerUpdateTimer;
    private double currentConversionRate = 0.0;
    private static String currentCurrencySymbolTxt;
    private static String currentCurrencySymbol;
    private static final Map<Integer, Boolean> canUpdate = new ConcurrentHashMap<>();

    public TickerService(OkHttpClient httpClient, Gson gson, Context ctx, TokenLocalSource localSource)
    {
        this.httpClient = httpClient;
        this.gson = gson;
        this.context = ctx;
        this.localSource = localSource;

        resetTickerUpdate();
        initCurrency();
    }

    public void updateTickers()
    {
        if (tickerUpdateTimer != null && !tickerUpdateTimer.isDisposed()) tickerUpdateTimer.dispose();

        tickerUpdateTimer = Observable.interval(0, UPDATE_TICKER_CYCLE, TimeUnit.MINUTES)
                    .doOnNext(l -> tickerUpdate())
                    .subscribe();
    }

    private void tickerUpdate()
    {
        updateCurrencyConversion()
                .flatMap(this::updateTickersFromOracle)
                .flatMap(this::fetchTickersSeparatelyIfRequired)
                .flatMap(this::addArtisTicker)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkTickers, this::onTickersError).isDisposed();
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
            return getStoredTicker();
        }
        else
        {
            TokenTicker currencyTicker = new TokenTicker(Double.toString(rate), "0", currentCurrencySymbolTxt, null, System.currentTimeMillis());
            localSource.updateERC20Tickers(0, new HashMap<String, TokenTicker>() {{ put(CURRENCY_CONV, currencyTicker); }});
            return rate;
        }
    }

    private Double getStoredTicker()
    {
        try (Realm realm = localSource.getTickerRealmInstance())
        {
            String key = TokensRealmSource.databaseKey(0, CURRENCY_CONV);
            RealmTokenTicker realmItem = realm.where(RealmTokenTicker.class)
                    .equalTo("contract", key)
                    .findFirst();

            if (realmItem != null)
            {
                return Double.parseDouble(realmItem.getPrice());
            }
        }
        catch (Exception e)
        {
            //
        }

        return 0.0;
    }

    private Single<Integer> fetchTickersSeparatelyIfRequired(int tickerCount)
    {
        //check base chain tickers
        if (tickerCount > 0) return Single.fromCallable(() -> tickerCount);
        else return fetchEthAndXdai(tickerCount)
                .flatMap(count -> fetchBlockScoutTicker(CLASSIC_ID, "etc", count))
                .flatMap(count -> fetchBlockScoutTicker(POA_ID, "core", count));
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

    public Single<Integer> getERC20Tickers(int chainId, List<TokenCardMeta> erc20Tokens)
    {
        String apiChainName = coinGeckoChainIdToAPIName.get(chainId);
        if (apiChainName == null || (canUpdate.containsKey(chainId) && !canUpdate.get(chainId)) || erc20Tokens.size() == 0)
            return Single.fromCallable(() -> 0);

        return Single.fromCallable(() -> {
            int newSize = 0;

            final Map<String, TokenTicker> erc20Tickers = new HashMap<>();
            try
            {
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
                        .url(COINGECKO_API.replace(CHAIN_ID, apiChainName).replace(CONTRACT_ADDR, sb.toString()))
                        .get()
                        .build();

                okhttp3.Response response = httpClient.newCall(request)
                        .execute();

                List<CoinGeckoTicker> tickers = CoinGeckoTicker.buildTickerList(response.body().string());
                newSize = tickers.size();

                for (CoinGeckoTicker t : tickers)
                {
                    BigDecimal changeValue = new BigDecimal(t.usdChange);
                    TokenTicker tTicker = new TokenTicker(String.valueOf(t.usdPrice * currentConversionRate),
                            changeValue.setScale(3, RoundingMode.DOWN).toString(), currentCurrencySymbolTxt, "", System.currentTimeMillis());

                    //store ticker
                    erc20Tickers.put(t.address, tTicker);
                }

                canUpdate.put(chainId, false);
                localSource.updateERC20Tickers(chainId, erc20Tickers);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return newSize;
        });
    }

    private void checkPeggedTickers(int chainId, TokenTicker ticker)
    {
        switch (chainId)
        {
            case MAINNET_ID:
                //add pegged chains
                ethTickers.put(ARBITRUM_MAIN_ID, ticker);
                ethTickers.put(OPTIMISTIC_MAIN_ID, ticker);
                break;
            default:
                break;
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

            ethTickers.put(chainId.intValue(), tTicker);
            checkPeggedTickers(chainId.intValue(), tTicker);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Single<Integer> fetchEthAndXdai(int tickerCount)
    {
        return Single.fromCallable(() -> {
            int newTickers = 0;
            try
            {
                Request request = new Request.Builder()
                        .url(COINGECKO_COINS_API)
                        .get()
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();
                if (response.code() / 200 == 1)
                {
                    List<CoinGeckoTicker> tickers = CoinGeckoTicker.buildTickerList(response.body().string());

                    for (CoinGeckoTicker t : tickers)
                    {
                        BigDecimal changeValue = new BigDecimal(t.usdChange);
                        TokenTicker tTicker = new TokenTicker(String.valueOf(t.usdPrice * currentConversionRate),
                                changeValue.setScale(3, RoundingMode.DOWN).toString(), currentCurrencySymbolTxt, "", System.currentTimeMillis());

                        //store ticker
                        int id = MAINNET_ID;
                        switch (t.address)
                        {
                            case "ethereum":
                                id = MAINNET_ID;
                                break;
                            case "xdai":
                                id = XDAI_ID;
                                break;
                            default:
                                break;
                        }

                        ethTickers.put(id, tTicker);
                        newTickers++;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return tickerCount + newTickers;
        });
    }

    private Single<Integer> fetchBlockScoutTicker(int chainId, String core, int tickers)
    {
        return Single.fromCallable(() -> {
            int newTickers = 0;
            try
            {
                Request request = new Request.Builder()
                        .url(BLOCKSCOUT.replace("[CORE]", core))
                        .get()
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();
                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();
                    JSONObject stateData = new JSONObject(result);
                    JSONObject data = stateData.getJSONObject("result");
                    TokenTicker tt = decodeBlockScoutTicker(data);
                    ethTickers.put(chainId, tt);
                    newTickers = 1;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return tickers + newTickers;
        });
    }

    private void checkTickers(int tickerSize)
    {
        if (BuildConfig.DEBUG) System.out.println("Tickers received: " + tickerSize);
        //store ticker values. If values have changed then update the token's update time so the wallet view will update
        localSource.updateEthTickers(ethTickers);
        localSource.removeOutdatedTickers();
    }

    public TokenTicker getEthTicker(int chainId)
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

    private TokenTicker decodeBlockScoutTicker(JSONObject eth)
    {
        TokenTicker ticker = null;
        try
        {
            double usdPrice = eth.getDouble("ethusd");// getString("price");
            String localePrice = String.valueOf(usdPrice * currentConversionRate);
            ticker = new TokenTicker(localePrice, "0.00", currentCurrencySymbolTxt, "", System.currentTimeMillis());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ticker = new TokenTicker();
        }

        return ticker;
    }

    public Single<Double> convertPair(String currency1, String currency2)
    {
        return Single.fromCallable(() -> {
            if (currency1 == null || currency2 == null || currency1.equals(currency2)) return (Double)1.0;
            String conversionURL = "http://currencies.apps.grandtrunk.net/getlatest/" + currency1 + "/" + currency2;
            okhttp3.Response response = null;
            double rate = 0.0;

            try
            {
                Request request = new Request.Builder()
                        .url(conversionURL)
                        .addHeader("Connection","close")
                        .get()
                        .build();
                response = httpClient.newCall(request)
                        .execute();

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
            finally
            {
                if (response != null) response.close();
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

    private double getUSDPrice() throws Exception {
        Web3j web3j = TokenRepository.getWeb3jService(MAINNET_ID);
        Function function = read();
        String responseValue = callSmartContractFunction(web3j, function, MEDIANIZER);

        BigDecimal usdRaw = BigDecimal.ZERO;

        if (responseValue == null) return usdRaw.doubleValue();

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());

        if (response.size() > 0)
        {
            usdRaw = new BigDecimal(((Uint256) response.get(0)).getValue());
            usdRaw = usdRaw.divide(new BigDecimal(Math.pow(10, 18)));
        }

        return usdRaw.doubleValue();
    }

    private static Function read() {
        return new Function(
                "read",
                Arrays.<Type>asList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private static Function getTickers() {
        return new Function(
                "getTickers",
                Arrays.<Type>asList(),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    public void addCustomTicker(int chainId, TokenTicker ticker)
    {
        if (ticker != null)
        {
            ethTickers.put(chainId, ticker);
        }
    }

    public void addCustomTicker(int chainId, String address, TokenTicker ticker)
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
        throwable.printStackTrace();
    }

    //TODO: Refactor this as required
    public static String getCurrencyString(double price)
    {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        df.setRoundingMode(RoundingMode.CEILING);
        if (price >= 0) {
            return currentCurrencySymbol + df.format(price);
        } else {
            return "-" + currentCurrencySymbol + df.format(Math.abs(price));
        }
    }

    public static String getCurrencyWithoutSymbol(double price)
    {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        df.setRoundingMode(RoundingMode.DOWN);
        return df.format(price);
    }

    private void initCurrency()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        currentCurrencySymbolTxt = pref.getString(CURRENCY_CODE_KEY, "USD");
        currentCurrencySymbol = pref.getString(CURRENCY_SYMBOL_KEY, "$");
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
        for (Integer chainId : coinGeckoChainIdToAPIName.keySet())
        {
            canUpdate.put(chainId, true);
        }
    }

    private static final Map<Integer, String> coinGeckoChainIdToAPIName = new HashMap<Integer, String>(){{
        put(MAINNET_ID, "ethereum");
        put(XDAI_ID, "xdai");
        put(BINANCE_MAIN_ID, "binance-smart-chain");
        put(MATIC_ID, "polygon-pos");
        put(CLASSIC_ID, "ethereum-classic");
        put(FANTOM_ID, "fantom");
        put(AVALANCHE_ID, "avalanche");
        put(HECO_ID, "huobi-token");
        put(ARBITRUM_MAIN_ID, "arbitrum-one");
        put(66, "okex-chain");
        put(1666600000, "harmony-shard-0");
        put(321, "kucoin-community-chain");
        put(88, "tomochain");
        put(42220, "celo");
    }};
}
