package com.alphawallet.app.entity;

import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by James on 5/02/2018.
 */

public class TradeInstance
{
    public BigInteger expiry;
    public final BigInteger price;
    public final int[] tickets;
    public final BigInteger contractAddress;
    public final BigInteger ticketStart;
    public final String publicKey;
    List<byte[]> signatures = new ArrayList<byte[]>();

    public TradeInstance(BigInteger price, BigInteger expiry, int[] tickets, String contractAddress, BigInteger publicKey, BigInteger ticketStartId) {
        this.price = price;
        this.expiry = expiry;
        this.tickets = tickets;
        byte[] keyBytes = publicKey.toByteArray();
        this.publicKey = padLeft(Numeric.toHexString(keyBytes, 0, keyBytes.length, false), 128);
        this.ticketStart = ticketStartId;
        this.contractAddress = Numeric.toBigInt(contractAddress);
    }

    public TradeInstance(TradeInstance t, byte[] sig) {
        this.price = t.price;
        this.expiry = t.expiry;
        this.tickets = t.tickets;
        this.publicKey = t.publicKey;
        this.ticketStart = t.ticketStart;
        this.contractAddress = t.contractAddress;
    }

    public void addSignature(byte[] sig)
    {
        signatures.add(sig);
    }

    public String getStringSig(int index) {
        String sigStr = null;
        if (index < signatures.size())
        {
            sigStr = new String(signatures.get(index));
        }
        return sigStr;
    }

    public String getExpiryString() {
        long expire = expiry.longValue();
        Date date = new Date(expire*1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm z");
        //sdf.setTimeZone(TimeZone.getTimeZone("GMT-4"));
        return sdf.format(date);
    }

    public byte[] getSignatureBytes(int index)
    {
        byte[] sig = null;
        if (index < signatures.size())
        {
            sig = signatures.get(index);
        }
        return sig;
    }

    public List<byte[]> getSignatures()
    {
        return signatures;
    }

    public void addSignatures(DataOutputStream ds) throws Exception
    {
        //now add the signatures
        for (byte[] sig : signatures)
        {                 
            ds.write(sig);
        }
    }

    public byte[] getTradeBytes() throws Exception
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(buffer);
        ds.write(Numeric.toBytesPadded(price, 32));
        ds.write(Numeric.toBytesPadded(expiry, 32));
        ds.write(Numeric.toBytesPadded(contractAddress, 20));

        byte[] uint16 = new byte[2];
        for (int ticketIndex : tickets)
        {
            //write big endian encoding
            uint16[0] = (byte)(ticketIndex >> 8);
            uint16[1] = (byte)(ticketIndex & 0xFF);
            ds.write(uint16);
        }
        ds.flush();

        return buffer.toByteArray();
    }

    private String padLeft(String source, int length)
    {
        if(source.length() > length) return source;
        char[] out = new char[length];
        int sourceOffset = length - source.length();
        System.arraycopy(source.toCharArray(), 0, out, sourceOffset, source.length());
        Arrays.fill(out, 0, sourceOffset, '0');
        return new String(out);
    }

    public int sigCount()
    {
        return signatures.size();
    }
}
