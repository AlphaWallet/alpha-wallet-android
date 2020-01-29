package com.alphawallet.app.service;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.AmberDataElement;
import com.alphawallet.app.entity.BlockscoutValue;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
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

public class TickerService implements TickerServiceInterface
{
    private static final int UPDATE_TICKER_CYCLE = 5; //5 Minutes
    private static final String COINMARKET_API_URL = "https://pro-api.coinmarketcap.com";
    private static final String MEDIANIZER = "0x729D19f657BD0614b4985Cf1D82531c67569197B";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Map<String, Ticker> erc20Tickers = new HashMap<>();
    private final Map<Integer, Ticker> ethTickers = new ConcurrentHashMap<>();
    private Disposable tickerUpdateTimer;

    public TickerService(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;

        updateTickers();
    }

    private void updateTickers()
    {
        if (tickerUpdateTimer == null || tickerUpdateTimer.isDisposed())
        {
            tickerUpdateTimer = Observable.interval(0, UPDATE_TICKER_CYCLE, TimeUnit.MINUTES)
                    .doOnNext(l -> fetchTickers()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::checkTickers, this::onTickersError).isDisposed())
                    .subscribe();
        }
    }

    private Single<Map<Integer, Ticker>> fetchTickers()
    {
        final String keyAPI = BuildConfig.CoinmarketCapAPI;
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
                    Ticker     ethTicker = decodeTicker(eth);
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

    private void checkTickers(Map<Integer, Ticker> tickerMap)
    {
        System.out.println("Tickers received: " + tickerMap.size());
    }

    @Override
    public Single<Token> attachTokenTicker(Token token)
    {
        return Single.fromCallable(() -> {
            if (token != null && erc20Tickers != null && token.tokenInfo.chainId == MAINNET_ID)
            {
                attachTokenTicker(token, erc20Tickers.get(token.tokenInfo.address.toLowerCase()));
            }

            return token;
        });
    }

    @Override
    public Single<Token[]> attachTokenTickers(Token[] tokens)
    {
        return Single.fromCallable(() -> {
            for (Token token : tokens)
            {
                if (token != null && token.tokenInfo.chainId == MAINNET_ID)
                {
                    attachTokenTicker(token, erc20Tickers.get(token.tokenInfo.address.toLowerCase()));
                }
            }

            return tokens;
        });
    }

    private void attachTokenTicker(Token token, Ticker ticker)
    {
        if (token != null && ticker != null)
        {
            token.ticker = new TokenTicker(String.valueOf(token.tokenInfo.chainId), token.tokenInfo.address, ticker.price_usd, ticker.percentChange24h, "USD", null);
        }
    }

    @Override
    public TokenTicker getTokenTicker(Token token)
    {
        if (token == null) return null;
        else if (token.isERC20() && token.tokenInfo.chainId == MAINNET_ID)
        {
            Ticker ticker = erc20Tickers.get(token.getAddress());
            if (ticker != null) token.ticker = new TokenTicker(String.valueOf(token.tokenInfo.chainId), token.tokenInfo.address, ticker.price_usd, ticker.percentChange24h, "USD", null);
            return token.ticker;
        }
        else if (token.isEthereum())
        {
            Ticker ticker = ethTickers.get(token.tokenInfo.chainId);
            if (ticker != null) token.ticker = new TokenTicker(String.valueOf(token.tokenInfo.chainId), token.tokenInfo.address, ticker.price_usd, ticker.percentChange24h, "USD", null);
            return token.ticker;
        }
        else
        {
            return null;
        }
    }

    @Override
    public Single<Ticker> getNativeTicker(NetworkInfo network)
    {
        switch (network.tickerId)
        {
            case C.ARTIS_SIGMA_TICKER:
                return convertPair("EUR", "USD")
                        .map(this::getSigmaTicker);
            default:
                return Single.fromCallable(() -> {
                    Ticker ticker = ethTickers.get(network.chainId);
                    if (ticker != null) ticker = new Ticker();
                    return ticker;
                });
        }
    }

    @Override
    public boolean hasTickers()
    {
        updateTickers(); // ensure ticker update thread is functioning (app could have been memory scavenged).
        return erc20Tickers.size() > 0;
    }

    @Override
    public Single<Token[]> getTokensOnNetwork(NetworkInfo info, String address, TokensService tokensService)
    {
        //TODO: find tokens on other networks
        String netName = "ethereum-mainnet";
        if (info.chainId != MAINNET_ID) return Single.fromCallable(() -> { return new Token[0]; });
        List<Token> tokenList = new ArrayList<>();
        final String keyAPI = BuildConfig.AmberdataAPI;
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
                JSONObject t = (JSONObject)tokens.get(i);
                String balanceStr = t.getString("amount");
                if (balanceStr.length() == 0 || balanceStr.equals("0")) continue;
                String    decimalsStr = t.getString("decimals");
                int       decimals    = (decimalsStr.length() > 0) ? Integer.parseInt(decimalsStr) : 0;
                Token existingToken = tokensService.getToken(network.chainId, t.getString("address"));
                if (decimals == 0 || (existingToken != null && !existingToken.isERC20())) continue;
                TokenInfo info        = new TokenInfo(t.getString("address"), t.getString("name"), t.getString("symbol"), decimals, true, network.chainId);
                //now create token with balance info, only for ERC20 for now
                if (decimalsStr.length() > 0)
                {
                    BigDecimal balance = new BigDecimal(balanceStr);
                    Token newToken = tf.createToken(info, balance, null, System.currentTimeMillis(), ContractType.ERC20, network.getShortName(), System.currentTimeMillis());
                    newToken.setTokenWallet(currentAddress);
                    if (existingToken != null) newToken.transferPreviousData(existingToken);
                    attachTokenTicker(newToken, erc20Tickers.get(newToken.tokenInfo.address.toLowerCase()));
                    newToken.refreshCheck = false;
                    tokenList.add(newToken);
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private Single<Map<Integer, Ticker>> addERC20Tickers(Map<Integer, Ticker> tickers)
    {
        return Single.fromCallable(() -> {
            try
            {
                Request request = new Request.Builder()
                        .url("https://web3api.io/api/v2/tokens/rankings?type=erc20")
                        .get()
                        .addHeader("x-api-key", BuildConfig.AmberdataAPI)
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
            Ticker             ticker;
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
            Ticker             ticker;
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

    private Single<Map<Integer, Ticker>> fetchAmberData(Map<Integer, Ticker> tickers)
    {
        final String keyAPI = BuildConfig.AmberdataAPI;
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

    private Ticker tickerFromAmber(AmberDataElement e)
    {
        Ticker ticker = new Ticker();
        ticker.price_usd = String.valueOf(e.currentPrice);
        ticker.id = e.blockchain.blockchainId;
        BigDecimal change = new BigDecimal(e.changeInPriceDaily);
        ticker.percentChange24h = change.setScale(3, RoundingMode.DOWN).toString();
        ticker.symbol = e.symbol;
        ticker.name = e.name;
        return ticker;
    }

    private Ticker tickerFromAmber(JSONObject e) throws JSONException
    {
        Ticker ticker = new Ticker();
        ticker.price_usd = e.getString("currentPrice");
        ticker.id = e.getString("address");
        BigDecimal change = new BigDecimal(e.getString("changeInPriceDaily"));
        ticker.percentChange24h = change.setScale(3, RoundingMode.DOWN).toString();
        ticker.symbol = e.getString("symbol");
        ticker.name = e.getString("name");
        return ticker;
    }

    private Ticker decodeTicker(JSONObject eth)
    {
        Ticker ticker = new Ticker();
        try
        {
            ticker.id = eth.getString("id");
            ticker.name = eth.getString("name");
            ticker.symbol = eth.getString("symbol");
            JSONObject quote = eth.getJSONObject("quote");
            JSONObject usdData = quote.getJSONObject("USD");
            BigDecimal change = new BigDecimal(usdData.getString("percent_change_24h"));
            ticker.percentChange24h = change.setScale(3, RoundingMode.DOWN).toString();
            ticker.price_usd = usdData.getString("price");
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
            String conversionURL = "http://currencies.apps.grandtrunk.net/getlatest/" + currency1 + "/" + currency2;
            Request request = new Request.Builder().url(conversionURL).get().build();

            try
            {
                okhttp3.Response response = httpClient.newCall(request).execute();
                int resultCode = response.code();
                if ((resultCode / 100) == 2 && response.body() != null)
                {
                    String responseBody = response.body().string();
                    double rate = Double.parseDouble(responseBody);
                    return rate;
                }
            }
            catch (Exception e)
            {
                //
            }

            return 1.0;
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
        org.web3j.abi.datatypes.Function function = read();
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

    private static org.web3j.abi.datatypes.Function read() {
        return new org.web3j.abi.datatypes.Function(
                "read",
                Arrays.<Type>asList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private Ticker getSigmaTicker(double rate)
    {
        Ticker artisTicker = new Ticker();
        artisTicker.percentChange24h = "0.00";
        double conversion = (1.0/13.7603)*rate; //13.7603 ATS = 1 EUR
        artisTicker.price_usd = String.valueOf(conversion);
        artisTicker.symbol = "USD";
        return artisTicker;
    }

    private void onTickersError(Throwable throwable)
    {
        throwable.printStackTrace();
    }
}
