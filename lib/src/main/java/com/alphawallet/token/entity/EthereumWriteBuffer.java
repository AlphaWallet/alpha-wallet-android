package com.alphawallet.token.entity;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import com.alphawallet.token.tools.Convert;
import com.alphawallet.token.tools.Numeric;

/**
 * Created by James on 26/03/2018.
 */

public class EthereumWriteBuffer extends DataOutputStream
{
    public EthereumWriteBuffer(OutputStream in)
    {
        super(in);
    }

    public void write32(BigInteger bi) throws IOException
    {
        write(Numeric.toBytesPadded(bi, 32));
    }

    public void writeAddress(BigInteger addr) throws IOException {
        write(Numeric.toBytesPadded(addr, 20));
    }

    public void writeAddress(String addr) throws IOException {
        BigInteger addrBI = new BigInteger(Numeric.cleanHexPrefix(addr), 16);
        writeAddress(addrBI);
    }

    public void writeBytes(String hex, int length) throws IOException {
        byte[] hexBytes = Numeric.hexStringToByteArray(hex);
        if (hexBytes.length < length)
        {
            for (int i = 0; i < (length - hexBytes.length); i++) writeByte(0);
        }
        write(hexBytes);
    }

    public void writeUnsigned4(BigInteger value) throws IOException {
        write(Numeric.toBytesPadded(UnsignedLong.create(value), 4));
    }

    public void writeUnsigned4(long value) throws IOException {
        write(Numeric.toBytesPadded(UnsignedLong.create(value), 4));
    }


    public void writeCompressedIndices(int[] indices) throws IOException
    {
        byte[] uint16 = new byte[2];
        byte[] uint8 = new byte[1];
        final int indexMax = 1<<16;
        for (int i : indices)
        {
            if (i >= indexMax)
            {
                throw new IOException("Index out of representation range: " + i);
            }
            if (i < (1 << 7))
            {
                uint8[0] = (byte) (i & ~(1 << 7));
                write(uint8);
            }
            else
            {
                uint16[0] = (byte) ((i >> 8) | (1<<7));
                uint16[1] = (byte) (i & 0xFF);
                write(uint16);
            }
        }
    }

    public void writeTokenIds(List<BigInteger> tokenIds) throws IOException
    {
        for (BigInteger tokenId : tokenIds)
        {
            write(Numeric.toBytesPadded(tokenId, 32));
        }
    }

    public void writeSignature(byte[] sig) throws IOException
    {
        //assertEquals(sig.length, 65);
        write(sig);
    }

    public void write4ByteMicroEth(BigInteger weiValue) throws IOException
    {
        byte[] max = Numeric.hexStringToByteArray("FFFFFFFF");
        BigInteger maxValue = new BigInteger(1, max);
        //this is value in microeth/szabo
        //convert to wei
        BigInteger microEth = Convert.fromWei(new BigDecimal(weiValue), Convert.Unit.SZABO).abs().toBigInteger();
        if (microEth.compareTo(maxValue) > 0)
        {
            microEth = maxValue;    //should we signal an overflow error here, or just silently round?
                                    //possibly irrelevant, this is a huge amount of eth.
        }

        byte[] uValBytes = UnsignedLong.createBytes(microEth.longValue());
        write(uValBytes);
    }

    /**
     * Write any decimal string value of any length into bytes
     * @param value
     * @param convSize
     */
    public void writeValue(String value, int convSize) throws IOException
    {
        BigInteger val = new BigInteger(value);
        byte[] valueBytes = val.toByteArray();
        byte[] toBeWritten = new byte[convSize];
        if (val.compareTo(BigInteger.ZERO) < 0) //pad with 0xFF if value is negative
        {
            for (int i = 0; i < convSize; i++) toBeWritten[i] = (byte) 0xFF;
        }

        if (valueBytes.length > convSize)
        {
            toBeWritten = new byte[convSize];
            int startTruncate = valueBytes.length - convSize;
            System.arraycopy(val.toByteArray(), startTruncate, toBeWritten, 0, convSize);
        }
        else
        {
            int bytesLength;
            int srcOffset;
            if (valueBytes[0] == 0)
            {
                bytesLength = valueBytes.length - 1;
                srcOffset = 1;
            }
            else
            {
                bytesLength = valueBytes.length;
                srcOffset = 0;
            }

            int destOffset = convSize - bytesLength;
            System.arraycopy(valueBytes, srcOffset, toBeWritten, destOffset, bytesLength);
        }

        write(toBeWritten);
    }
}

