package com.alphawallet.app.util.das;

import android.text.TextUtils;

/**
 * Created by JB on 17/09/2021.
 */
public class DASAccount
{
    String owner_address_chain;
    String owner_address;
    DASRecord[] records;

    String getEthOwner()
    {
        if (!TextUtils.isEmpty(owner_address_chain) && owner_address_chain.equalsIgnoreCase("ETH"))
        {
            return owner_address;
        }
        else
        {
            return null;
        }
    }
}
