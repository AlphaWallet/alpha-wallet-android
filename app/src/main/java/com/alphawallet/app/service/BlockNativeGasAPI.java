package com.alphawallet.app.service;

import android.text.TextUtils;

import com.alphawallet.app.entity.EIP1559FeeOracleResult;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.JsonUtils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class BlockNativeGasAPI
{
    public static BlockNativeGasAPI instance;
    private final OkHttpClient httpClient;

    public static BlockNativeGasAPI get(OkHttpClient httpClient)
    {
        if (instance == null)
        {
            instance = new BlockNativeGasAPI(httpClient);
        }
        return instance;
    }

    public BlockNativeGasAPI(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    private Request buildRequest(String api)
    {
        Request.Builder requestB = new Request.Builder()
                .url(api)
                .header("Content-Type", "application/json")
                .addHeader("Authorization", KeyProviderFactory.get().getBlockNativeKey())
                .get();
        return requestB.build();
    }

    public Single<Map<Integer, EIP1559FeeOracleResult>> fetchGasEstimates(long chainId)
    {
        String oracleAPI = EthereumNetworkBase.getBlockNativeOracle(chainId);
        return Single.fromCallable(() -> buildOracleResult(executeRequest(oracleAPI))); // any kind of error results in blank mapping,
                                                                                        // if blank, fall back to calculation method
    }

    private Map<Integer, EIP1559FeeOracleResult> buildOracleResult(String oracleReturn)
    {
        Map<Integer, EIP1559FeeOracleResult> results = new HashMap<>();
        try
        {
            JSONObject prices = new JSONObject(oracleReturn);
            //get base fee per gas
            JSONArray blockPrices = prices.getJSONArray("blockPrices");
            JSONObject blockPrice0 = blockPrices.getJSONObject(0);
            String baseFeePerGasStr = blockPrice0.getString("baseFeePerGas");
            BigDecimal baseFeePerGas = new BigDecimal(baseFeePerGasStr);
            BigInteger baseFeePerGasWei = BalanceUtils.gweiToWei(baseFeePerGas);
            //get the array
            String estimatedPrices = blockPrice0.getJSONArray("estimatedPrices").toString();
            PriceElement[] priceElements = new Gson().fromJson(estimatedPrices, PriceElement[].class);

            results.put(0, new EIP1559FeeOracleResult(priceElements[0].getFeeOracleResult(baseFeePerGasWei)));
            results.put(1, new EIP1559FeeOracleResult(priceElements[2].getFeeOracleResult(baseFeePerGasWei)));
            results.put(2, new EIP1559FeeOracleResult(priceElements[3].getFeeOracleResult(baseFeePerGasWei)));
            results.put(3, new EIP1559FeeOracleResult(priceElements[4].getFeeOracleResult(baseFeePerGasWei)));
        }
        catch (JSONException e)
        {
            // map will be empty; default to using backup calculation method
            Timber.w(e);
        }
        return results;
    }

    private String executeRequest(String api)
    {
        if (!TextUtils.isEmpty(api))
        {
            try (okhttp3.Response response = httpClient.newCall(buildRequest(api)).execute())
            {
                if (response.isSuccessful())
                {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null)
                    {
                        return responseBody.string();
                    }
                }
                else
                {
                    return Objects.requireNonNull(response.body()).string();
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
                return e.getMessage();
            }
        }

        return JsonUtils.EMPTY_RESULT;
    }

    private static class PriceElement
    {
        public String confidence;
        public String price;
        public String maxPriorityFeePerGas;
        public String maxFeePerGas;

        public BigInteger getMaxPriorityFeePerGasWei()
        {
            return elementToWei(maxPriorityFeePerGas);
        }

        public BigInteger getMaxFeePerGasWei()
        {
            return elementToWei(maxFeePerGas);
        }

        private BigInteger elementToWei(String value)
        {
            try
            {
                BigDecimal gweiValue = new BigDecimal(value);
                return BalanceUtils.gweiToWei(gweiValue);
            }
            catch (Exception e)
            {
                return BigInteger.ZERO;
            }
        }

        public EIP1559FeeOracleResult getFeeOracleResult(BigInteger baseFee)
        {
            return new EIP1559FeeOracleResult(getMaxFeePerGasWei(), getMaxPriorityFeePerGasWei(), baseFee);
        }
    }
}
