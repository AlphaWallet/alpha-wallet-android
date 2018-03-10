package com.wallet.crypto.alphawallet.entity;

import android.support.annotation.NonNull;

import org.web3j.crypto.Sign;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            addr = "0x" + bytesToHex(buffer20);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return addr;
    }

    @Override
    public void close()
    {
        try
        {
            super.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int available()
    {
        int remains = 0;
        try
        {
            remains = super.available();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return remains;
    }

    public int readInt32()
    {
        int value = 0;
        try
        {
            value = readInt();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return value;
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

    public int[] readShortIndices(int count)
    {
        int[] intArray = new int[count];
        try
        {
            for (int i = 0; i < count; i++)
            {
                int value = readByte() * 0x100;
                value += (int) (readByte()&0xFF);
                intArray[i] = value;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return intArray;
    }

    public byte[] readSignature()
    {
        byte[] sig = new byte[65];
        try
        {
            read(sig);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return sig;
    }
}
