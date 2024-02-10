package com.alphawallet.app.service;

import static com.alphawallet.app.util.BalanceUtils.gweiToWei;

import android.text.TextUtils;

import com.alphawallet.app.entity.EIP1559FeeOracleResult;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.HttpServiceHelper;
import com.alphawallet.app.repository.KeyProviderFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class InfuraGasAPI
{
    public static Single<Map<Integer, EIP1559FeeOracleResult>> get1559GasEstimates(final long chainId, final OkHttpClient httpClient)
    {
        return Single.fromCallable(() -> {
            Map<Integer, EIP1559FeeOracleResult> gasMap = new HashMap<>();

            //ensure we have correct Infura details
            String gasOracleAPI = EthereumNetworkRepository.getGasOracle(chainId);
            String infuraKey = KeyProviderFactory.get().getInfuraKey();
            String infuraSecret = KeyProviderFactory.get().getInfuraSecret();

            if (TextUtils.isEmpty(gasOracleAPI) || TextUtils.isEmpty(infuraKey) || TextUtils.isEmpty(infuraSecret))
            {
                //require Infura key with API and secret to operate the gas API
                return gasMap;
            }

            final Request.Builder rqBuilder = new Request.Builder()
                    .url(gasOracleAPI)
                    .get();

            HttpServiceHelper.addInfuraGasCredentials(rqBuilder, KeyProviderFactory.get().getInfuraSecret());

            try (Response response = httpClient.newCall(rqBuilder.build()).execute())
            {
                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();
                    gasMap = readGasMap(result);
                }
            }
            catch (Exception e)
            {
                Timber.w(e);
            }

            return gasMap;
        });
    }

    private static Map<Integer, EIP1559FeeOracleResult> readGasMap(String apiReturn)
    {
        Map<Integer, EIP1559FeeOracleResult> gasMap = new HashMap<>();
        try
        {
            BigDecimal rBaseFee = BigDecimal.ZERO;
            JSONObject result = new JSONObject(apiReturn);
            if (result.has("estimatedBaseFee"))
            {
                rBaseFee = new BigDecimal(result.getString("estimatedBaseFee"));
            }

            EIP1559FeeOracleResult low = readFeeResult(result, "low", rBaseFee);
            EIP1559FeeOracleResult medium = readFeeResult(result, "medium", rBaseFee);
            EIP1559FeeOracleResult high = readFeeResult(result, "high", rBaseFee);

            if (low == null || medium == null || high == null)
            {
                return gasMap;
            }

            BigInteger rapidPriorityFee = (new BigDecimal(high.priorityFee)).multiply(BigDecimal.valueOf(1.2)).toBigInteger();
            EIP1559FeeOracleResult rapid = new EIP1559FeeOracleResult(high.maxFeePerGas, rapidPriorityFee, gweiToWei(rBaseFee));

            gasMap.put(0, rapid);
            gasMap.put(1, high);
            gasMap.put(2, medium);
            gasMap.put(3, low);
        }
        catch (JSONException e)
        {
            //
        }

        return gasMap;
    }

    private static EIP1559FeeOracleResult readFeeResult(JSONObject result, String speed, BigDecimal rBaseFee)
    {
        EIP1559FeeOracleResult oracleResult = null;

        try
        {
            if (result.has(speed))
            {
                JSONObject thisSpeed = result.getJSONObject(speed);
                BigDecimal maxFeePerGas = new BigDecimal(thisSpeed.getString("suggestedMaxFeePerGas"));
                BigDecimal priorityFee = new BigDecimal(thisSpeed.getString("suggestedMaxPriorityFeePerGas"));
                oracleResult = new EIP1559FeeOracleResult(gweiToWei(maxFeePerGas), gweiToWei(priorityFee), gweiToWei(rBaseFee));
            }
        }
        catch (Exception e)
        {
            Timber.e("Infura GasOracle read failing; please adjust your Infura API settings.");
        }

        return oracleResult;
    }
}
