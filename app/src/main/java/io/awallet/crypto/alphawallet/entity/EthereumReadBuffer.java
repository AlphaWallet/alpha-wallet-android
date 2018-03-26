package io.awallet.crypto.alphawallet.entity;

import android.support.annotation.NonNull;

import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 24/02/2018.
 */

public class EthereumReadBuffer extends DataInputStream
{
    private final byte[] readBuffer;
    private final byte[] readBuffer4;

    public EthereumReadBuffer(@NonNull InputStream in)
    {
        super(in);
        readBuffer = new byte[32];
        readBuffer4 = new byte[4];
    }

    public BigInteger readBI() throws IOException
    {
        BigInteger retVal;

        read(readBuffer);
        retVal = new BigInteger(readBuffer);

        return retVal;
    }

    public String readAddress() throws IOException {
        byte[] buffer20 = new byte[20];
        read(buffer20);
        return Numeric.toHexString(buffer20);
    }

    @Override
    public int available() throws IOException
    {
        int remains = 0;
        remains = super.available();

        return remains;
    }

    public int[] readUint16Indices(int count) throws IOException
    {
        int[] intArray = new int[count];
        for (int i = 0; i < count; i++)
        {
            int value = byteToUint(readByte()) * 0x100;
            value += byteToUint(readByte());
            intArray[i] = value;
        }
        return intArray;
    }

    public void readSignature(byte[] signature) throws IOException
    {
        if (signature.length == 65) {
            read(signature); // would it throw already, if the data is too short? - Weiwu
        } else {
            throw new IOException("Data isn't a signature"); // Is this even necessary? - Weiwu
        }
    }

    private int byteToUint(byte b)
    {
        return (int) b & 0xFF;
    }

    public BigInteger read4ByteMicroEth() throws IOException
    {
        byte[] buffer = new byte[4];
        read(buffer);
        BigDecimal value = new BigDecimal(new BigInteger(1, buffer));
        //this is value in microeth/szabo
        //convert to wei
        BigInteger wei = Convert.toWei(value, Convert.Unit.SZABO).toBigInteger();
        return wei;
    }

    public long read4ByteExpiry() throws IOException
    {
        byte[] buffer = new byte[4];
        read(buffer);
        BigDecimal value = new BigDecimal(new BigInteger(1, buffer)); //force unsigned
        return value.longValue();
    }

    public int[] readCompressedIndices(int indiciesLength) throws IOException
    {
        byte[] readBuffer = new byte[indiciesLength];
        read(readBuffer);
        int index = 0;
        int state = 0;

        List<Integer> indexList = new ArrayList<>();
        Integer rValue = 0;

        while (index < indiciesLength)
        {
            Integer p = byteToUint(readBuffer[index]);
            switch (state)
            {
                case 0:
                    //check if we require an extension byte read
                    rValue = (p & ~(1 << 7)); //remove top bit.
                    if (((1 << 7) & p) == (1 << 7)) //check if top bit is there
                    {
                        state = 1;
                    }
                    else
                    {
                        indexList.add(rValue);
                    }
                    break;
                case 1:
                    rValue = (rValue << 8) + (p & 0xFF); //Low byte + High byte without top bit (which is the extension designation bit)
                    indexList.add(rValue);
                    state = 0;
                    break;
                default:
                    throw new IOException("Illegal state in readCompressedIndicies");
            }

            index++;
        }

        int[] indexArray = new int[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) indexArray[i] = indexList.get(i);

        return indexArray;
    }
}
