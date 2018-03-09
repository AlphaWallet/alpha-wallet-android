package com.wallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import org.ethereum.geth.BigInt;
import org.web3j.utils.Numeric;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 21/02/2018.
 */

public class SalesOrder implements Parcelable
{
    public final long expiry;
    public final double price;
    public final BigInteger priceWei;
    public final int[] tickets;
    public final int ticketStart;
    public final int ticketCount;
    public final String contractAddress;
    public final byte[] signature;
    public final byte[] message;

    public SalesOrder(double price, long expiry, int ticketStart, int ticketCount, String contractAddress, String sig, String msg)
    {
        this.message = Base64.decode(msg, Base64.DEFAULT);
        this.price = price;
        this.expiry = expiry;
        this.ticketStart = ticketStart;
        this.ticketCount = ticketCount;
        ByteArrayInputStream bas = new ByteArrayInputStream(message);
        EthereumReadBuffer ds = new EthereumReadBuffer(bas);
        priceWei = ds.readBI();
        ds.readBI();
        ds.readAddress();
        this.tickets = ds.readShortIndicies(ticketCount);
        this.contractAddress = contractAddress;
        this.signature = Base64.decode(sig, Base64.DEFAULT);
    }

    public SalesOrder(byte[] message, int v, BigInteger r, BigInteger s) {
    }

    public static SalesOrder parseUniversalLink(String link) {
            BigInteger r = BigInteger.ONE;
            BigInteger s = BigInteger.ZERO;
            int v = 27;
            byte[] message = new byte[32];
            /* TODO: parse the link */
            return new SalesOrder(message, v, r, s);
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

        int sigLength = in.readInt();
        signature = new byte[sigLength]; // in theory we shouldn't need to do this, always is 65 bytes
        in.readByteArray(signature);

        int messageLength = in.readInt();
        message = new byte[messageLength]; // in theory we shouldn't need to do this, always is 65 bytes
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
}
