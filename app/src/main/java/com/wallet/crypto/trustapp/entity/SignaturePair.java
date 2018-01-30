package com.wallet.crypto.trustapp.entity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * Created by James on 30/01/2018.
 */

public class SignaturePair {
    public final byte[] selection;
    public final byte[] signature;

    public SignaturePair(String selection, byte[] signature)
    {
        //convert selection into optimised selection
        //first 8 bit value is the length of the message
        byte[] select = null;
        try
        {
            ByteArrayOutputStream sBuilder = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(sBuilder);
            int trailingZeros = getTrailingZeros(selection);
            String truncatedValue = selection.substring(0, selection.length() - trailingZeros);
            int newLength = truncatedValue.length(); //only need to know the length of the truncated selection value
            ds.writeByte(newLength);
            ds.writeByte(trailingZeros);
            ds.write(truncatedValue.getBytes());
            ds.flush();
            select = sBuilder.toByteArray();
        }
        catch (Exception e)
        {
            select = null;
        }
        finally
        {
            this.signature = signature;
            this.selection = select;
        }
    }

    private int getTrailingZeros(String s)
    {
        int i = (s.length() - 1);
        int trailingZeros = 0;
        while (s.charAt(i--) == '0')
        {
            trailingZeros++;
        }

        return trailingZeros;
    }
}
