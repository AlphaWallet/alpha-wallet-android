package com.alphawallet.app.util.das;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by JB on 17/09/2021.
 */
public class DASBody
{
    String jsonrpc;
    int id;
    public DASResult result;
    public final Map<String, DASRecord> records = new HashMap<>();

    public void buildMap()
    {
        if (result == null || result.data == null || result.data.account_data == null || result.data.account_data.records == null)
            return;

        for (DASRecord record : result.data.account_data.records)
        {
            records.put(record.key, record);
        }
    }

    public String getEthOwner()
    {
        if (result != null && result.data != null && result.data.account_data != null)
        {
            return result.data.account_data.getEthOwner();
        }
        else
        {
            return null;
        }
    }
}
