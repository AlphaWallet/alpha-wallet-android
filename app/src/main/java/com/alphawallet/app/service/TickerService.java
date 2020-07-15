package com.alphawallet.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.alphawallet.app.entity.AmberDataElement;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokenRepository;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigDecimal;
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
import static com.alphawallet.app.repository.EthereumNetworkRepository.SOKOL_ID;
import static com.alphawallet.app.repository.EthereumNetworkRepository.XDAI_ID;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TickerService
{
    private static final int UPDATE_TICKER_CYCLE = 5; //5 Minutes
    private static final String COINMARKET_API_URL = "https://pro-api.coinmarketcap.com";
    private static final String MEDIANIZER = "0x729D19f657BD0614b4985Cf1D82531c67569197B";
    private static final String BLOCKSCOUT = "https://blockscout.com/poa/[CORE]/api?module=stats&action=ethprice";

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
    private Wallet wallet;

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
    }

    public void updateTickers(Wallet wallet)
    {
        this.wallet = wallet;
        if (tickerUpdateTimer != null && !tickerUpdateTimer.isDisposed()) tickerUpdateTimer.dispose();

        tickerUpdateTimer = Observable.interval(0, UPDATE_TICKER_CYCLE, TimeUnit.MINUTES)
                    .doOnNext(l -> tickerUpdate())
                    .subscribe();
    }

    private void tickerUpdate()
    {
        updateCurrencyConversion()
                .flatMap(this::addArtisTicker)
                .flatMap(a -> fetchBlockScoutTicker(XDAI_ID, "xdai"))
                .flatMap(a -> fetchBlockScoutTicker(POA_ID, "core"))
                .map(poaTicker -> { if (!poaTicker.price.equals("0")) ethTickers.put(SOKOL_ID, poaTicker); return poaTicker; })
                .flatMap(this::fetchTickers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkTickers, this::onTickersError).isDisposed();
    }

    private Single<Double> updateCurrencyConversion()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        currentCurrencySymbolTxt = pref.getString("currency_locale", "USD");
        currentCurrencySymbol = pref.getString("currency_symbol", "$");
        return convertPair("USD", currentCurrencySymbolTxt);
    }

    private Single<Map<Integer, TokenTicker>> fetchTickers(TokenTicker artisTicker)
    {
        final String keyAPI = getCMCKey();
        return Single.fromCallable(() -> {
            try
            {
                Request request = new Request.Builder()
                        .url(COINMARKET_API_URL + "/v1/cryptocurrency/quotes/latest?symbol=ETH,ETC,DAI,POA")
                        .get()
                        .addHeader("X-CMC_PRO_API_KEY", keyAPI)
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();
                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();
                    JSONObject stateData = new JSONObject(result);
                    JSONObject data      = stateData.getJSONObject("data");
                    JSONObject eth       = data.getJSONObject("ETH");
                    TokenTicker ethTicker = decodeTicker(eth);
                    ethTickers.put(MAINNET_ID, ethTicker);
                    ethTickers.put(RINKEBY_ID, ethTicker);
                    ethTickers.put(ROPSTEN_ID, ethTicker);
                    ethTickers.put(KOVAN_ID, ethTicker);
                    ethTickers.put(GOERLI_ID, ethTicker);
                    JSONObject etc = data.getJSONObject("ETC");
                    ethTickers.put(CLASSIC_ID, decodeTicker(etc));
                    JSONObject dai = data.getJSONObject("DAI");
                    ethTickers.put(XDAI_ID, decodeTicker(dai));
                    JSONObject poa = data.getJSONObject("POA");
                    ethTickers.put(POA_ID, decodeTicker(poa));
                    ethTickers.put(SOKOL_ID, decodeTicker(poa));
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return ethTickers;
        }).flatMap(this::fetchAmberData)
          .flatMap(this::addERC20Tickers);
    }

    private Single<TokenTicker> fetchBlockScoutTicker(int chainId, String core)
    {
        return Single.fromCallable(() -> {
            TokenTicker tt = new TokenTicker();
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
                    tt = decodeBlockScoutTicker(data);
                    ethTickers.put(chainId, tt);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return tt;
        });
    }

    private void checkTickers(Map<Integer, TokenTicker> tickerMap)
    {
        System.out.println("Tickers received: " + tickerMap.size());
        //store ticker values. If values have changed then update the token's update time so the wallet view will update
        localSource.updateEthTickers(ethTickers, wallet);
        localSource.updateERC20Tickers(erc20Tickers, wallet);
        localSource.removeOutdatedTickers(wallet);
    }

    public Single<Token> attachTokenTicker(Token token)
    {
        return Single.fromCallable(() -> {
            if (token != null)
            {
                attachTokenTicker(token, getTokenTicker(token));
            }

            return token;
        });
    }

    public Single<Token[]> attachTokenTickers(Token[] tokens)
    {
        return Single.fromCallable(() -> {
            for (Token token : tokens)
            {
                if (token != null)
                {
                    attachTokenTicker(token, getTokenTicker(token));
                }
            }

            return tokens;
        });
    }

    private void attachTokenTicker(Token token, TokenTicker ticker)
    {
        if (token != null && ticker != null)
        {
            token.ticker = ticker;
        }
    }

    public TokenTicker getTokenTicker(Token token)
    {
        if (token == null) return null;
        TokenTicker serviceTicker;
        if (token.isEthereum()) serviceTicker = ethTickers.get(token.tokenInfo.chainId);
        else serviceTicker = erc20Tickers.get(token.getAddress().toLowerCase());

        //Only update the ticker if service ticker is more recent than stored ticker
        if (serviceTicker != null &&
                (token.ticker == null || (serviceTicker.updateTime > token.ticker.updateTime)))
        {
            return serviceTicker;
        }
        else
        {
            return token.ticker;
        }
    }

    public TokenTicker getEthTicker(int chainId)
    {
        return ethTickers.get(chainId);
    }

    private Single<TokenTicker> addArtisTicker(Double currentConversion)
    {
        currentConversionRate = currentConversion;
        return convertPair("EUR", currentCurrencySymbolTxt)
                .flatMap(this::getSigmaTicker)
                .map(this::addArtisTickers);
    }

    private TokenTicker addArtisTickers(TokenTicker tokenTicker)
    {
        ethTickers.put(ARTIS_SIGMA1_ID, tokenTicker);
        ethTickers.put(ARTIS_TAU1_ID, tokenTicker);
        return tokenTicker;
    }

    public TokenTicker updateTicker(Token token, TokenTicker oldTicker) {
        return checkTicker(getTokenTicker(token), oldTicker);
    }

    private TokenTicker checkTicker(TokenTicker ticker, TokenTicker oldTicker)
    {
        if (ticker != null && ticker.updateTime > 0) return ticker;
        else return oldTicker;
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
                attachTokenTicker(newToken, erc20Tickers.get(newToken.tokenInfo.address.toLowerCase()));
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

    private Single<Map<Integer, TokenTicker>> addERC20Tickers(Map<Integer, TokenTicker> tickers)
    {
        return Single.fromCallable(() -> {
            try
            {
                Request request = new Request.Builder()
                        .url("https://web3api.io/api/v2/tokens/rankings?type=erc20")
                        .get()
                        .addHeader("x-api-key", getAmberDataKey())
                        .build();

                okhttp3.Response response = httpClient.newCall(request)
                        .execute();

                handleTokenTickers(response);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return tickers;
        });
    }

    private void handleTokenTickers(@NotNull okhttp3.Response response) throws IOException, JSONException
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
                erc20Tickers.put(e.getString("address").toLowerCase(), ticker);
            }
        }
    }

    private void handleTickers(@NotNull okhttp3.Response response) throws IOException, JSONException
    {
        if (response.code() / 200 == 1)
        {
            String result = response.body()
                    .string();
            JSONObject         stateData = new JSONObject(result);
            JSONObject         payload   = stateData.getJSONObject("payload");
            JSONArray          data      = payload.getJSONArray("data");
            AmberDataElement[] elements  = gson.fromJson(data.toString(), AmberDataElement[].class);
            TokenTicker        ticker;
            for (AmberDataElement e : elements)
            {
                ticker = tickerFromAmber(e);
                switch (e.symbol)
                {
                    case "eth":
                        ethTickers.put(MAINNET_ID, ticker);
                        ethTickers.put(RINKEBY_ID, ticker);
                        ethTickers.put(ROPSTEN_ID, ticker);
                        ethTickers.put(KOVAN_ID, ticker);
                        ethTickers.put(GOERLI_ID, ticker);
                        break;
                    case "DAI":
                        ethTickers.put(XDAI_ID, ticker);
                        break;
                    case "etc":
                        ethTickers.put(CLASSIC_ID, ticker);
                        break;
                }

                if (e.address != null && e.specifications != null && e.specifications.length > 0 && e.specifications[0].equals("ERC20"))
                {
                    erc20Tickers.put(e.address, ticker);
                }
            }
        }
    }

    private Single<Map<Integer, TokenTicker>> fetchAmberData(Map<Integer, TokenTicker> tickers)
    {
        final String keyAPI = getAmberDataKey();
        return Single.fromCallable(() -> {
            try
            {
                Request request = new Request.Builder()
                        .url("https://web3api.io/api/v1/market/rankings")
                        .get()
                        .addHeader("x-api-key", keyAPI)
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();

                handleTickers(response);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return ethTickers;
        });
    }

    private TokenTicker tickerFromAmber(AmberDataElement e)
    {
        String currencyPrice = String.valueOf(e.currentPrice * currentConversionRate);
        BigDecimal change = new BigDecimal(e.changeInPriceDaily);
        String percentChange24h = change.setScale(3, RoundingMode.DOWN).toString();
        String image = "";
        if (e.blockchain != null) image = e.blockchain.icon != null ? e.blockchain.icon : "";
        TokenTicker ticker = new TokenTicker(currencyPrice, percentChange24h, currentCurrencySymbolTxt, image, System.currentTimeMillis());
        return ticker;
    }

    private TokenTicker tickerFromAmber(JSONObject e) throws JSONException
    {
        BigDecimal change = new BigDecimal(e.getString("changeInPriceDaily"));
        String percentChange = change.setScale(3, RoundingMode.DOWN).toString();
        double usdPrice = e.getDouble("currentPrice");
        double currentPrice = usdPrice * currentConversionRate;
        String priceStr = String.valueOf(currentPrice);
        TokenTicker ticker = new TokenTicker(priceStr, percentChange, currentCurrencySymbolTxt, "", System.currentTimeMillis());
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

    private TokenTicker decodeTicker(JSONObject eth)
    {
        TokenTicker ticker = null;
        try
        {
            JSONObject quote = eth.getJSONObject("quote");
            JSONObject usdData = quote.getJSONObject("USD");

            BigDecimal change = new BigDecimal(usdData.getString("percent_change_24h"));
            String percentChange = change.setScale(3, RoundingMode.DOWN).toString();
            double usdPrice = usdData.getDouble("price");// getString("price");
            String localePrice = String.valueOf(usdPrice * currentConversionRate);
            ticker = new TokenTicker(localePrice, percentChange, currentCurrencySymbolTxt, "", System.currentTimeMillis());
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
