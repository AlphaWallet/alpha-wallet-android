package com.alphawallet.app.service;

import android.util.Log;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AmberDataElement;
import com.alphawallet.app.entity.BlockscoutValue;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.google.gson.Gson;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenTicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

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


public class CoinmarketcapTickerService implements TickerService
{

    private static final String COINMARKET_API_URL = "https://pro-api.coinmarketcap.com";
    private static final String MEDIANIZER = "0x729D19f657BD0614b4985Cf1D82531c67569197B";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private Map<String, Ticker> erc20Tickers;

    public CoinmarketcapTickerService(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override
    public Single<Map<Integer, Ticker>> fetchCMCTickers()
    {
        Map<Integer, Ticker> tickers = new HashMap<>();
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
                    tickers.put(MAINNET_ID, ethTicker);
                    tickers.put(RINKEBY_ID, ethTicker);
                    tickers.put(ROPSTEN_ID, ethTicker);
                    tickers.put(KOVAN_ID, ethTicker);
                    tickers.put(GOERLI_ID, ethTicker);
                    JSONObject etc = data.getJSONObject("ETC");
                    tickers.put(CLASSIC_ID, decodeTicker(etc));
                    JSONObject dai = data.getJSONObject("DAI");
                    tickers.put(XDAI_ID, decodeTicker(dai));
                    JSONObject poa = data.getJSONObject("POA");
                    tickers.put(POA_ID, decodeTicker(poa));
                    tickers.put(SOKOL_ID, decodeTicker(poa));
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return tickers;
        });
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
                if (token != null && erc20Tickers != null && token.tokenInfo.chainId == MAINNET_ID)
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
        if (token != null && erc20Tickers != null && token.tokenInfo.chainId == MAINNET_ID)
        {
            Ticker ticker = erc20Tickers.get(token.getAddress());
            if (ticker != null) token.ticker = new TokenTicker(String.valueOf(token.tokenInfo.chainId), token.tokenInfo.address, ticker.price_usd, ticker.percentChange24h, "USD", null);
            return token.ticker;
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean hasTickers()
    {
        return erc20Tickers != null;
    }

    @Override
    public Single<Map<Integer, Ticker>> fetchAmberData()
    {
        Map<Integer, Ticker> tickers = new HashMap<>();
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
                                tickers.put(MAINNET_ID, ticker);
                                tickers.put(RINKEBY_ID, ticker);
                                tickers.put(ROPSTEN_ID, ticker);
                                tickers.put(KOVAN_ID, ticker);
                                tickers.put(GOERLI_ID, ticker);
                                break;
                            case "DAI":
                                tickers.put(XDAI_ID, ticker);
                                break;
                            case "etc":
                                tickers.put(CLASSIC_ID, ticker);
                                break;
                        }

                        if (e.address != null && e.specifications != null && e.specifications.length > 0 && e.specifications[0].equals("ERC20"))
                        {
                            if (erc20Tickers == null) erc20Tickers = new HashMap<>();
                            erc20Tickers.put(e.address, ticker);
                        }
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return tickers;
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
        catch (JSONException j)
        {
            j.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return ticker;
    }

    @Override
    public Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency)
    {
        return Single.just(new TokenTicker[0]);
    }

    @Override
    public Single<Ticker> fetchEthPrice(NetworkInfo networkInfo, Ticker ticker)
    {
        return Single.fromCallable(() -> {
            //create a function
            double usdValue = getUSDPrice();
            ticker.price_usd = String.valueOf(usdValue);

            return ticker;
        });
    }

    @Override
    public Single<Ticker> fetchBlockScoutPrice(NetworkInfo networkInfo, final Ticker ticker)
    {
        return Single.fromCallable(() -> {
            Ticker retTicker = ticker;
            try
            {
                Request request = new Request.Builder().url("https://blockscout.com/" + networkInfo.blockscoutAPI + "/api?module=stats&action=ethprice").get().build();
                okhttp3.Response response = httpClient.newCall(request).execute();

                if (response != null && response.code()/200 == 1)
                {
                    if (retTicker == null)
                    {
                        retTicker = new Ticker();
                        retTicker.id = String.valueOf(networkInfo.chainId);
                        retTicker.percentChange24h = "0";
                    }
                    String result = response.body().string();
                    JSONObject stateData = new JSONObject(result);
                    JSONObject resultData = stateData.getJSONObject("result");
                    if (resultData != null)
                    {
                        BlockscoutValue val = gson.fromJson(resultData.toString(), BlockscoutValue.class);
                        retTicker.price_usd = String.valueOf(val.ethusd);
                    }
                }
            }
            catch (Exception e)
            {
                //
            }

            return retTicker;
        });
    }

    private static @NonNull
    <T> ApiErrorOperator<T> apiError(Gson gson)
    {
        return new ApiErrorOperator<>();
    }

    private final static class ApiErrorOperator<T> implements ObservableOperator<T, Response<T>>
    {

        @Override
        public Observer<? super Response<T>> apply(Observer<? super T> observer)
        {
            return new DisposableObserver<Response<T>>()
            {
                @Override
                public void onNext(Response<T> response)
                {
                    if (isDisposed())
                    {
                        return;
                    }
                    observer.onNext(response.body());
                    observer.onComplete();
                }

                @Override
                public void onError(Throwable e)
                {
                    if (!isDisposed())
                    {
                        observer.onError(e);
                    }
                }

                @Override
                public void onComplete()
                {
                    if (!isDisposed())
                    {
                        observer.onComplete();
                    }
                }
            };
        }
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

}
