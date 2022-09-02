package com.alphawallet.app;

import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static org.junit.Assert.assertEquals;

import com.alphawallet.app.entity.CovalentTransaction;
import com.alphawallet.app.entity.EtherscanEvent;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import timber.log.Timber;

/**
 * Created by JB on 2/09/2022.
 */
public class CovalentProcessingTest
{
    String APIReturn;

    String walletAddr = "0x99c839a196497eda48c5dee9545ce10d497fd8f5";
    private static final String filePath = "src/test/java/com/alphawallet/app/resources/";

    public CovalentProcessingTest()
        {
            String validStructuredDataJSONFilePath = filePath +
                    "covalenttxs.json";
            try
            {
                APIReturn = getResource(validStructuredDataJSONFilePath);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

    private String getResource(String jsonFile) throws IOException {
        return new String(
                Files.readAllBytes(Paths.get(jsonFile).toAbsolutePath()), StandardCharsets.UTF_8);
    }

    @Test
    public void testCovalentTx() {
        try
        {
            CovalentTransaction[] covalentTransactions = getCovalentTransactions(APIReturn, walletAddr);

            NetworkInfo info = new NetworkInfo("Klaytn", "Klaytn", "", "", KLAYTN_ID, "", "");
            EtherscanEvent[] events = CovalentTransaction.toEtherscanEvents(covalentTransactions);
            EtherscanTransaction[] txs = CovalentTransaction.toRawEtherscanTransactions(covalentTransactions, info);

            assertEquals(events.length, 517);
            assertEquals(txs.length, 139);

            EtherscanEvent ev = events[0];
            assertEquals(ev.value, "2700000000");
            assertEquals(ev.tokenDecimal, "6");

            ev = events[516];

            assertEquals(ev.value, "");
            assertEquals(ev.tokenID, "20007");
            assertEquals(ev.tokenDecimal, "0");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private CovalentTransaction[] getCovalentTransactions(String response, String walletAddress) throws JSONException
    {
        if (response == null || response.length() < 80)
        {
            return new CovalentTransaction[0];
        }
        JSONObject stateData = null;
        try
        {
            stateData = new JSONObject(response);
        }
        catch (JSONException e)
        {
            Timber.w(e);
        }

        JSONObject data = stateData.getJSONObject("data");
        JSONArray orders = data.getJSONArray("items");
        CovalentTransaction[] ctxs = new Gson().fromJson(orders.toString(), CovalentTransaction[].class);

        return ctxs;
    }
}


