package com.wallet.crypto.alphawallet.entity;

import org.web3j.utils.Numeric;

import java.io.DataOutputStream;
import java.math.BigInteger;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by James on 5/02/2018.
 */

public class TradeInstance
{
    public final BigInteger expiry;
    public final BigInteger price;
    public final short[] tickets;
    public final BigInteger contractAddress;
    List<byte[]> signatures = new ArrayList<byte[]>();

    public TradeInstance(BigInteger price, BigInteger expiry, short[] tickets, String contractAddress) {
        this.price = price;
        this.expiry = expiry;
        this.tickets = tickets;
        this.contractAddress = Numeric.toBigInt(contractAddress);//Numeric.cleanHexPrefix(ticket.getAddress());
    }

    public TradeInstance(TradeInstance t, byte[] sig) {
        this.price = t.price;
        this.expiry = t.expiry;
        this.tickets = t.tickets;
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
        String formattedDate = sdf.format(date);
        return formattedDate;
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

    public int sigCount()
    {
        return signatures.size();
    }
}
