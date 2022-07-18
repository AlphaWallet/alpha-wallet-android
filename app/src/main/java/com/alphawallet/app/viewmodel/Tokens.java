package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.lifi.Connection;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class Tokens
{
    public static void sortValue(List<Connection.LToken> tokenItems)
    {
        Collections.sort(tokenItems, (l, r) -> {
            BigDecimal lBal = new BigDecimal(l.fiatEquivalent);
            BigDecimal rBal = new BigDecimal(r.fiatEquivalent);
            return rBal.compareTo(lBal);
        });
    }

    public static void sortName(List<Connection.LToken> tokenItems)
    {
        Collections.sort(tokenItems, (l, r) -> {
            if (l.isNativeToken())
            {
                return -1;
            }
            else if (r.isNativeToken())
            {
                return 1;
            }
            else
            {
                return l.name.compareToIgnoreCase(r.name);
            }
        });
    }
}
