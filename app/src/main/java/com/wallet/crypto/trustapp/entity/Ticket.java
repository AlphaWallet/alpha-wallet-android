package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

import org.web3j.abi.datatypes.generated.Uint16;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 27/01/2018.
 */

public class Ticket extends Token implements Parcelable
{
    public final List<Uint16> balanceArray;

    public Ticket(TokenInfo tokenInfo, List<Uint16> balances) {
        super(tokenInfo, BigDecimal.ZERO);
        this.balanceArray = balances;
    }

    private Ticket(Parcel in) {
        super(in);
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        balanceArray = new ArrayList<Uint16>();
        for (Object o : readObjArray)
        {
            Uint16 val = (Uint16)o;
            balanceArray.add(val);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(tokenInfo, flags);
        dest.writeArray(balanceArray.toArray());
        dest.writeString(balance.toString());
    }

    public List<Uint16> parseIDList(String userList)
    {
        List<Uint16> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                Uint16 thisId = new Uint16(Integer.parseInt(trim));

                if (thisId.getValue().intValue() > 0)
                {
                    idList.add(thisId);
                }
            }
        }
        catch (Exception e)
        {
            idList = null;
        }

        return idList;
    }
}
