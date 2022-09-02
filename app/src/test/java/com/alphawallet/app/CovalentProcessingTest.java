package com.alphawallet.app;

import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static org.junit.Assert.assertEquals;

import com.alphawallet.app.entity.CovalentTransaction;
import com.alphawallet.app.entity.EtherscanEvent;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.google.common.io.Resources;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by JB on 2/09/2022.
 */
public class CovalentProcessingTest
{
    private String APIReturn;

    public CovalentProcessingTest() throws IOException
    {
        URL url = Resources.getResource("covalenttxs.json");
        APIReturn = Resources.toString(url, StandardCharsets.UTF_8);
    }

    @Test
    public void testCovalentTx() throws JSONException
    {
        CovalentTransaction[] covalentTransactions = getCovalentTransactions(APIReturn);

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
        assertEquals(ev.from, "0xc067a53c91258ba513059919e03b81cf93f57ac7");
        assertEquals(ev.to, "0xf9c883c8dca140ebbdc87a225fe6e330be5d25ef");
    }

    private CovalentTransaction[] getCovalentTransactions(String response) throws JSONException
    {
        if (response == null || response.length() < 80)
        {
            return new CovalentTransaction[0];
        }
        JSONObject stateData = new JSONObject(response);

        JSONObject data = stateData.getJSONObject("data");
        JSONArray orders = data.getJSONArray("items");
        CovalentTransaction[] ctxs = new Gson().fromJson(orders.toString(), CovalentTransaction[].class);

        return ctxs;
    }
}


