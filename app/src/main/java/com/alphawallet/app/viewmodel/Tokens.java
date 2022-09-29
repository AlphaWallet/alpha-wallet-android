package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.lifi.Token;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class Tokens
{
    public static void sortValue(List<Token> tokenItems)
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

    public static void sortName(List<Token> tokenItems)
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
