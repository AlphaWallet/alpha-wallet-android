package com.wallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.wallet.crypto.alphawallet.repository.TokenRepository;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

import org.spongycastle.util.encoders.Base64;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.wallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

/**
 * Created by James on 21/02/2018.
 */

public class SalesOrder implements Parcelable
{
    public final long expiry;
    public final double price;
    public final BigInteger priceWei;
    public final int[] tickets;
    public int ticketStart;
    public final int ticketCount;
    public final String contractAddress;
    public final byte[] signature = new byte[65];
    public final byte[] message;
    public TokenInfo tokenInfo; //convenience pointer to token information
    public String ownerAddress; //convenience ecrecovered owner address;
    public List<Integer> balanceInfo = null; // received balance from blockchain check

    public SalesOrder(double price, long expiry, int ticketStart, int ticketCount, String contractAddress, String sig, String msg)
            throws SalesOrderMalformed
    {
        this.message = Base64.decode(msg);
        this.price = price;
        this.expiry = expiry;
        this.ticketStart = ticketStart;
        this.ticketCount = ticketCount;
        ByteArrayInputStream bas = new ByteArrayInputStream(message);
        try {
            EthereumReadBuffer ds = new EthereumReadBuffer(bas);
            priceWei = ds.readBI();
            ds.readBI();
            ds.readAddress();
            this.tickets = ds.readUint16Indices(ticketCount);
            this.contractAddress = contractAddress;
            System.arraycopy(Base64.decode(sig), 0, this.signature, 0, 65);
            ds.close();
        }
        catch(IOException e) {
            throw new SalesOrderMalformed();
        }
    }

    /**
     * Universal link's Query String section is formatted like this:
     *
     * AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKcAqA==;
     * 1b;
     * 2F982B84C635967A9B6306ED5789A7C1919164171E37DCCDF4B59BE547544105;
     * 30818B896B7D240F56C59EBDF209062EE54DA7A3590905739674DCFDCECF3E9B
     *
     * Base64 message: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKcAqA==
     *     - bytes32: price Wei
     *     - bytes32: expiry
     *     - bytes20: contract address
     *     - Uint16[]: ticket indices
     *
     * byte: 1b
     * bytes32: 2F982B84C635967A9B6306ED5789A7C1919164171E37DCCDF4B59BE547544105
     * bytes32: 30818B896B7D240F56C59EBDF209062EE54DA7A3590905739674DCFDCECF3E9B
     *
     */
    public static SalesOrder parseUniversalLink(String link) throws SalesOrderMalformed
    {
        final String importTemplate = "/import?";
        int offset = link.indexOf(importTemplate);
        if (offset > 0)
        {
            offset += importTemplate.length();
            String linkData = link.substring(offset);
            return new SalesOrder(linkData);
        }
        else
        {
            return null;
        }
    }

    protected SalesOrder(String linkData) throws SalesOrderMalformed {
        //separate the args
        String[] linkArgs = linkData.split(";");
        byte[] r = Numeric.toBytesPadded(new BigInteger(linkArgs[2], 16), 32);
        byte[] s = Numeric.toBytesPadded(new BigInteger(linkArgs[3], 16), 32);
        try {
            /* if was given in decimal by mistake, some high bytes are chopped off here */
            System.arraycopy(r, 0, signature, 0, 32);     // r
            System.arraycopy(s, 0, signature, 32, 32);    // s
            signature[64] = (byte) (int) Integer.valueOf(linkArgs[1], 16); // v
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new SalesOrderMalformed("Signature shorter than expected 256");
        }
        catch (ArrayStoreException a)
        {
            throw new SalesOrderMalformed("Attempting to write signature too long for storage");
        }
        catch (NullPointerException e)
        {
            throw new SalesOrderMalformed("invalid import link data");
        }

        message = Base64.decode(linkArgs[0]);
        try {
            ByteArrayInputStream bas = new ByteArrayInputStream(message);
            EthereumReadBuffer ds = new EthereumReadBuffer(bas);
            priceWei = ds.readBI();
            expiry = ds.readBI().intValue();
            contractAddress = ds.readAddress();
            ticketCount = ds.available() / 2;
            tickets = ds.readUint16Indices(ticketCount);
            ds.close();
        } catch (IOException e) {
            throw new SalesOrderMalformed();
        } catch (StringIndexOutOfBoundsException f) {
            throw new SalesOrderMalformed();
        } catch (Exception e) {
            throw new SalesOrderMalformed();
        }

        BigInteger milliWei = Convert.fromWei(priceWei.toString(), Convert.Unit.FINNEY).toBigInteger();
        price = milliWei.doubleValue() / 1000.0;
    }

    public byte[] writeMessage()
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(buffer);
        try {
            ds.write(Numeric.toBytesPadded(priceWei, 32));
            ds.write(Numeric.toBytesPadded(BigInteger.valueOf(expiry), 32));
            ds.write(contractAddress.getBytes());

            byte[] uint16 = new byte[2];
            for (int ticketIndex : tickets)
            {
                //write big endian encoding
                uint16[0] = (byte)(ticketIndex >> 8);
                uint16[1] = (byte)(ticketIndex & 0xFF);
                ds.write(uint16);
            }
            ds.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return buffer.toByteArray();
    }

    private SalesOrder(Parcel in) {
        expiry = in.readLong();
        price = in.readDouble();
        ticketStart = in.readInt();
        ticketCount = in.readInt();
        contractAddress = in.readString();
        int ticketLength = in.readInt();
        tickets = new int[ticketLength];
        in.readIntArray(tickets);

        int sigLength = in.readInt();   // must not be higher than 65 bytes
        in.readByteArray(signature);    // in my guess, it's always is 65 bytes so it should fit.

        int messageLength = in.readInt();
        message = new byte[messageLength];
        in.readByteArray(message);
        priceWei = new BigInteger(in.readString());
    }

    public static final Creator<SalesOrder> CREATOR = new Creator<SalesOrder>() {
        @Override
        public SalesOrder createFromParcel(Parcel in) {
            return new SalesOrder(in);
        }

        @Override
        public SalesOrder[] newArray(int size) {
            return new SalesOrder[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public static byte[] generateReverseTradeData(SalesOrder order)
    {
        byte[] data = null;
        try
        {
            BigInteger expiry = BigInteger.valueOf(order.expiry);
            List<BigInteger> ticketIndices = new ArrayList<>();
            for (int ticketIndex : order.tickets) {
                ticketIndices.add(BigInteger.valueOf(ticketIndex));
            }
            //convert to signature representation
            Sign.SignatureData sellerSig = sigFromByteArray(order.signature);

            data = TokenRepository.createTrade(expiry, ticketIndices, (int)sellerSig.getV(), sellerSig.getR(), sellerSig.getS());

            //Can we recreate the seller address?
            System.out.println(order.getOwnerKey());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return data;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(expiry);
        parcel.writeDouble(price);
        parcel.writeInt(ticketStart);
        parcel.writeInt(ticketCount);
        parcel.writeString(contractAddress);
        parcel.writeInt(tickets.length);
        parcel.writeIntArray(tickets);
        parcel.writeInt(signature.length);
        parcel.writeByteArray(signature);
        parcel.writeInt(message.length);
        parcel.writeByteArray(message);
        parcel.writeString(priceWei.toString(10));
    }

    /**
     * ECRecover the owner address from a sales order
     *
     * @return
     */
    public String getOwnerKey() {
        try {
            Sign.SignatureData sigData = MarketQueueService.sigFromByteArray(signature);
            BigInteger recoveredKey = Sign.signedMessageToKey(getTradeBytes(), sigData);
            ownerAddress = "0x" + Keys.getAddress(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return ownerAddress;
    }

    private byte[] getTradeBytes()
    {
        try {
            BigInteger contractAddressBi = Numeric.toBigInt(contractAddress);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(buffer);
            ds.write(Numeric.toBytesPadded(priceWei, 32));
            ds.write(Numeric.toBytesPadded(BigInteger.valueOf(expiry), 32));
            ds.write(Numeric.toBytesPadded(contractAddressBi, 20));

            byte[] uint16 = new byte[2];
            for (int ticketIndex : tickets) {
                //write big endian encoding
                uint16[0] = (byte) (ticketIndex >> 8);
                uint16[1] = (byte) (ticketIndex & 0xFF);
                ds.write(uint16);
            }
            ds.flush();

            return buffer.toByteArray();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public boolean balanceChange(List<Integer> balance)
    {
        //compare two balances

        //quick return, if sizes are different there's a change
        if (balanceInfo == null)
        {
            balanceInfo = new ArrayList<>(); //initialise the balance list
            return true;
        }
        if (balance.size() != balanceInfo.size()) return true;

        List<Integer> oldBalance = new ArrayList<>(balanceInfo);
        List<Integer> newBalance = new ArrayList<>(balance);

        oldBalance.removeAll(balanceInfo);
        newBalance.removeAll(balance);

        return (oldBalance.size() != 0 || newBalance.size() != 0);
    }
}
