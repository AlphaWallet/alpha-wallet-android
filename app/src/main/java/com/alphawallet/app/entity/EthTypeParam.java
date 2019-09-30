package com.alphawallet.app.entity;

import java.math.BigDecimal;

/**
 * Created by James on 23/02/2019.
 * Stormbird in Singapore
 */
public class EthTypeParam
{
    public String type;
    public String value;

    public EthTypeParam(String t, String v)
    {
        type = t;
        value = v;

        if (t.contains("uint"))
        {
            //v is in exp form
            try
            {
                String convStr = String.valueOf(Double.parseDouble(v));
                BigDecimal bi = new BigDecimal(convStr);
                value = bi.toPlainString();
            }
            catch (NumberFormatException e)
            {
                //do nothing
            }
        }
    }
}
