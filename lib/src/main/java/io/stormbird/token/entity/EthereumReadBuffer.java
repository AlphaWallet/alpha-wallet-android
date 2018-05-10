package io.stormbird.token.entity;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.stormbird.token.tools.Numeric;

/**
 * Created by James on 24/02/2018.
 */

public class EthereumReadBuffer extends DataInputStream
{
    private final byte[] readBuffer;
    private final byte[] readBuffer4;

    public EthereumReadBuffer(InputStream in)
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


    public void readSignature(byte[] signature) throws IOException
    {
        if (signature.length == 65) {
            read(signature); // would it throw already, if the data is too short? - Weiwu
        } else {
            throw new IOException("Data isn't a signature"); // Is this even necessary? - Weiwu
        }
    }

    /*
     * The java 8 recommended way is to read an unsigned Short as Short, and use it as
     * unsigned Short. Here we still use the old method, reading unsigned shorts into int[].
     */
    public void readUnsignedShort(int[] ints) throws IOException
    {
        for (int i = 0; i < ints.length; i++)
        {
            int value = toUnsignedInt(readShort());
            ints[i] = value;
        }
    }

    /*
     * equivalent of Short.toUnsignedInt
     */
    private int toUnsignedInt(short s) {
        return s & 0x0000FFFF;
    }

    /*
     * equivalent of Byte.toUnsignedInt
     */
    private int toUnsignedInt(byte b)
    {
        return b & 0x000000FF;
    } // Int is 32 bits

    /*
     * equivalent of Integer.readUnsignedLong
     */
    public long toUnsignedLong(int i) throws IOException
    {
        return i & 0x00000000ffffffffL; // long is always 64 bits
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
            Integer p = toUnsignedInt(readBuffer[index]); // equivalent of Byte.toUnsignedInt()
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
