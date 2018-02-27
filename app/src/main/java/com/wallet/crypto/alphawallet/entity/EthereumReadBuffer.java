package com.wallet.crypto.alphawallet.entity;

import android.support.annotation.NonNull;

import org.web3j.crypto.Sign;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by James on 24/02/2018.
 */

public class EthereumReadBuffer extends DataInputStream
{
    private final byte[] readBuffer;

    public EthereumReadBuffer(@NonNull InputStream in)
    {
        super(in);
        readBuffer = new byte[32];
    }

    public BigInteger readBI()
    {
        BigInteger retVal = BigInteger.ZERO;

        try
        {
            read(readBuffer);
            retVal = new BigInteger(readBuffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return retVal;
    }

    public String readAddress()
    {
        String addr = "0x";

        try
        {
            byte[] buffer20 = new byte[20];
            read(buffer20);
            addr = bytesToHex(readBuffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return addr;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public int[] readShortIndicies(int count)
    {
        int[] intArray = new int[count];
        try
        {
            for (int i = 0; i < count; i++)
            {
                intArray[i] = readShort();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return intArray;
    }
}
