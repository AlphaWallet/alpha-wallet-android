package com.alphawallet.app.helper;

import com.alphawallet.app.entity.lifi.Connection;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class TokenSortHelper
{
    public static void sort(List<Connection.LToken> tokenItems)
    {
        Collections.sort(tokenItems, (l, r) -> l.name.compareToIgnoreCase(r.name));
        Collections.sort(tokenItems, (l, r) -> {
            BigDecimal lBal = new BigDecimal(l.balance);
            BigDecimal rBal = new BigDecimal(r.balance);
            return rBal.compareTo(lBal);
        });
    }

}
