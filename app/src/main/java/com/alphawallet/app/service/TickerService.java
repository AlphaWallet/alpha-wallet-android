package com.alphawallet.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.token.entity.EthereumReadBuffer;
import com.alphawallet.token.tools.Numeric;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
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
import java.io.InterruptedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.EthereumNetworkBase.ARTIS_SIGMA1_ID;
import static com.alphawallet.app.repository.EthereumNetworkBase.ARTIS_TAU1_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.CLASSIC_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.GOERLI_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.KOVAN_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.POA_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.RINKEBY_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.ROPSTEN_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.XDAI_ID;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TickerService
{
    private static final int UPDATE_TICKER_CYCLE = 5; //5 Minutes
    private static final String COINMARKET_API_URL = "https://pro-api.coinmarketcap.com";
    private static final String MEDIANIZER = "0x729D19f657BD0614b4985Cf1D82531c67569197B";
    private static final String BLOCKSCOUT = "https://blockscout.com/poa/[CORE]/api?module=stats&action=ethprice";
    private static final String ETHERSCAN = "https://api.etherscan.io/api?module=stats&action=ethprice";
    private static final String MARKET_ORACLE_CONTRACT = "0xf155a7eb4a2993c8cf08a76bca137ee9ac0a01d8";
    private static final String ORACLE_ETHERSCAN_API = "https://api-rinkeby.etherscan.io/api?module=account&action=txlist&address=[ORACLE]&sort=desc&page=1&offset=1".replace("[ORACLE]", MARKET_ORACLE_CONTRACT);

    public static final long TICKER_TIMEOUT = DateUtils.HOUR_IN_MILLIS; //remove ticker if not seen in one hour
    public static final long TICKER_STALE_TIMEOUT = 15 * DateUtils.MINUTE_IN_MILLIS; //try to use market API if AlphaWallet market oracle not updating

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Context context;
    private final TokenLocalSource localSource;
    private final Map<String, TokenTicker> erc20Tickers = new HashMap<>();
    private final Map<Integer, TokenTicker> ethTickers = new ConcurrentHashMap<>();
    private Disposable tickerUpdateTimer;
    private double currentConversionRate = 0.0;
    private static String currentCurrencySymbolTxt;
    private static String currentCurrencySymbol;

    public static native String getCMCKey();
    public static native String getAmberDataKey();

    static {
        System.loadLibrary("keys");
    }

    public TickerService(OkHttpClient httpClient, Gson gson, Context ctx, TokenLocalSource localSource)
    {
        this.httpClient = httpClient;
        this.gson = gson;
        this.context = ctx;
        this.localSource = localSource;

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
                .flatMap(this::fetchLastMarketContractWrite)
                .flatMap(this::updateTickersFromOracle)
                .flatMap(this::fetchTickersSeparatelyIfRequired)
                .flatMap(this::addERC20Tickers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkTickers, this::onTickersError).isDisposed();
    }

    private Single<Double> updateCurrencyConversion()
    {
        initCurrency();
        return convertPair("USD", currentCurrencySymbolTxt);
    }

    private Single<Integer> fetchTickersSeparatelyIfRequired(int tickerCount)
    {
        if (tickerCount > 0) return Single.fromCallable(() -> tickerCount);
        else return fetchEtherscanTicker(tickerCount)
                .flatMap(count -> fetchBlockScoutTicker(CLASSIC_ID, "etc", count))
                .flatMap(count -> fetchBlockScoutTicker(XDAI_ID, "xdai", count))
                .flatMap(count -> fetchBlockScoutTicker(POA_ID, "core", count))
                .flatMap(this::addArtisTicker);
    }

    private Single<Integer> updateTickersFromOracle(long lastTxTime)
    {
        return Single.fromCallable(() -> {
            int tickerSize = 0;
            Web3j web3j = TokenRepository.getWeb3jService(RINKEBY_ID);
            //fetch current tickers
            Function function = getTickers();
            String responseValue = callSmartContractFunction(web3j, function, MARKET_ORACLE_CONTRACT);
            List<Type> responseValues = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

            if (!responseValues.isEmpty())
            {
                Type T = responseValues.get(0);
                List<Uint256> values = (List) T.getValue();
                long tickerUpdateTime = (lastTxTime > 0 ? lastTxTime : values.get(0).getValue().longValue()) * 1000L;

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

    private Single<Long> fetchLastMarketContractWrite(double conversionRate)
    {
        currentConversionRate = conversionRate;
        return Single.fromCallable(() -> {
            Long lastTxTime = 0L;
            try
            {
                Request request = new Request.Builder()
                        .url(ORACLE_ETHERSCAN_API)
                        .get()
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();
                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();

                    EtherscanTransaction[] txs = getEtherscanTransactions(result);

                    if (txs.length > 0)
                    {
                        Transaction tx = txs[0].createTransaction(null, RINKEBY_ID);
                        lastTxTime = tx.timeStamp;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return lastTxTime;
        });
    }

    private EtherscanTransaction[] getEtherscanTransactions(String response) throws JSONException
    {
        JSONObject stateData = new JSONObject(response);
        JSONArray orders = stateData.getJSONArray("result");
        return gson.fromJson(orders.toString(), EtherscanTransaction[].class);
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
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Single<Integer> fetchEtherscanTicker(int tickerCount)
    {
        return Single.fromCallable(() -> {
            int newTickers = 0;
            try
            {
                Request request = new Request.Builder()
                        .url(ETHERSCAN)
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
                    TokenTicker tt = decodeEtherscanTicker(data);
                    ethTickers.put(MAINNET_ID, tt);
                    newTickers = 5;
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
        System.out.println("Tickers received: " + tickerSize);
        //store ticker values. If values have changed then update the token's update time so the wallet view will update
        localSource.updateEthTickers(ethTickers);
        localSource.updateERC20Tickers(erc20Tickers);
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

    public Single<Token[]> getTokensOnNetwork(NetworkInfo info, String address, TokensService tokensService)
    {
        //TODO: find tokens on other networks
        String netName = "ethereum-mainnet";
        if (info.chainId != MAINNET_ID) return Single.fromCallable(() -> { return new Token[0]; });
        List<Token> tokenList = new ArrayList<>();
        final String keyAPI = getAmberDataKey();
        return Single.fromCallable(() -> {
            try
            {
                String url = "https://web3api.io/api/v2/addresses/" + address + "/balances";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("x-api-key", keyAPI)
                        .addHeader("x-amberdata-blockchain-id", netName)
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();
                if (response.code() / 200 == 1)
                {
                    String result = response.body().string();
                    handleTokenList(info, tokenList, result, address, tokensService);
                }
            }
            catch (InterruptedIOException e)
            {
                // silent fail, expected
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return tokenList.toArray(new Token[0]);
        });
    }

    private void handleTokenList(NetworkInfo network, List<Token> tokenList, String result, String currentAddress, TokensService tokensService)
    {
        if (result.contains("NOTOK")) return;

        try
        {
            JSONObject   json    = new JSONObject(result);
            JSONObject   res     = json.getJSONObject("payload");
            JSONArray    tokens  = res.getJSONArray("tokens");

            TokenFactory tf      = new TokenFactory();

            for (int i = 0; i < tokens.length(); i++)
            {
                ContractType cType = ContractType.ERC20;
                JSONObject t          = (JSONObject) tokens.get(i);
                String     balanceStr = t.getString("amount");
                if (balanceStr.length() == 0 || balanceStr.equals("0")) continue;
                String decimalsStr   = t.getString("decimals");
                int    decimals      = (decimalsStr.length() > 0) ? Integer.parseInt(decimalsStr) : 0;
                Token  existingToken = tokensService.getToken(network.chainId, t.getString("address"));
                if (existingToken == null)
                {
                    cType = ContractType.OTHER; //if we haven't seen this token before mark as needing contract type check
                }
                else if (!existingToken.isERC20() && existingToken.getInterfaceSpec() != ContractType.OTHER) //allow tokens still classified as 'OTHER' to be updated.
                {                                                                                            //we may be able to categorise them later
                    continue;
                }
                else if (isDynamicBalanceToken(existingToken))
                {
                    continue;
                }

                TokenInfo info = new TokenInfo(t.getString("address"), t.getString("name"), t.getString("symbol"), decimals, true, network.chainId);
                //now create token with balance info, only for ERC20 for now
                BigDecimal balance  = new BigDecimal(balanceStr);
                Token      newToken = tf.createToken(info, balance, null, System.currentTimeMillis(), cType, network.getShortName(), System.currentTimeMillis());
                newToken.setTokenWallet(currentAddress);
                tokenList.add(newToken);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private boolean isDynamicBalanceToken(Token existingToken)
    {
        for (String dynamicTokenAddress : DYNAMIC_BALANCE_TOKENS)
        {
            if (existingToken.getAddress().equalsIgnoreCase(dynamicTokenAddress)) return true;
        }

        return false;
    }

    private Single<Integer> addERC20Tickers(int currentSize)
    {
        return Single.fromCallable(() -> {
            int newSize = currentSize;
            try
            {
                Request request = new Request.Builder()
                        .url("https://web3api.io/api/v2/tokens/rankings?type=erc20")
                        .get()
                        .addHeader("x-api-key", getAmberDataKey())
                        .build();

                okhttp3.Response response = httpClient.newCall(request)
                        .execute();

                newSize += handleTokenTickers(response);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return newSize;
        });
    }

    private int handleTokenTickers(@NotNull okhttp3.Response response) throws IOException, JSONException
    {
        if (response.code() / 200 == 1)
        {
            String result = response.body()
                    .string();
            JSONObject         stateData = new JSONObject(result);
            JSONObject         payload   = stateData.getJSONObject("payload");
            JSONArray          data      = payload.getJSONArray("data");
            TokenTicker        ticker;
            for (int i = 0; i < data.length(); i++)
            {
                JSONObject e = (JSONObject) data.get(i);
                ticker = tickerFromAmber(e);
                if (ticker != null) erc20Tickers.put(e.getString("address").toLowerCase(), ticker);
            }
        }

        return erc20Tickers.size();
    }

    private TokenTicker tickerFromAmber(JSONObject e) throws JSONException
    {
        TokenTicker ticker = null;
        try
        {
            BigDecimal change = new BigDecimal(e.getString("changeInPriceDaily"));
            String percentChange = change.setScale(3, RoundingMode.DOWN).toString();
            double usdPrice = e.getDouble("currentPrice");
            double currentPrice = usdPrice * currentConversionRate;
            String priceStr = String.valueOf(currentPrice);
            ticker = new TokenTicker(priceStr, percentChange, currentCurrencySymbolTxt, "", System.currentTimeMillis());
        }
        catch (NumberFormatException nf)
        {
            //
        }

        return ticker;
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

    private TokenTicker decodeEtherscanTicker(JSONObject eth)
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
                    return Double.parseDouble(responseBody);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (response != null) response.close();
            }

            return (Double)1.0;
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

    public void addCustomTicker(String address, TokenTicker ticker)
    {
        if (ticker != null && address != null)
        {
            erc20Tickers.put(address, ticker);
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
        return currentCurrencySymbol + df.format(price);
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
        currentCurrencySymbolTxt = pref.getString("currency_locale", "USD");
        currentCurrencySymbol = pref.getString("currency_symbol", "$");
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

    // These ERC20 can't have balance updated from the market service
    private static final String[] DYNAMIC_BALANCE_TOKENS = {
            "0x3a3A65aAb0dd2A17E3F1947bA16138cd37d08c04", //AAVE
            "0x71fc860f7d3a592a4a98740e39db31d25db65ae8"  //AAVE
    };
}
