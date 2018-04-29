package io.awallet.crypto.alphawallet.entity;

import android.support.annotation.NonNull;

import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import static junit.framework.Assert.assertEquals;

/**
 * Created by James on 26/03/2018.
 */

public class EthereumWriteBuffer extends DataOutputStream
{
    public EthereumWriteBuffer(@NonNull OutputStream in)
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

    public void writeSignature(byte[] sig) throws IOException
    {
        assertEquals(sig.length, 65);
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
                                    //Update: This is screened out in the UI now
        }

        byte[] uValBytes = UnsignedLong.createBytes(microEth.longValue());
        write(uValBytes);
    }
}

