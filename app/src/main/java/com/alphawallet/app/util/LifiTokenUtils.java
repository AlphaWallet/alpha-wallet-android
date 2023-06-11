package com.alphawallet.app.util;


import com.alphawallet.app.entity.lifi.LifiToken;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class LifiTokenUtils
{
    public static void sortValue(List<LifiToken> tokenItems)
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
                BigDecimal lBal = new BigDecimal(l.fiatEquivalent);
                BigDecimal rBal = new BigDecimal(r.fiatEquivalent);
                return rBal.compareTo(lBal);
            }
        });
    }

    public static void sortName(List<LifiToken> tokenItems)
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
                return l.name.trim().compareToIgnoreCase(r.name.trim());
            }
        });
    }
}
