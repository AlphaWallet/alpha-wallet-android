package com.wallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 21/02/2018.
 */

public class MarketInstance implements Parcelable
{
    public final long expiry;
    public final double price;
    public final int[] tickets;
    public final int ticketStart;
    public final int ticketCount;
    public final String contractAddress;
    public final byte[] signature;
    public final byte[] message;

    public MarketInstance(double price, long expiry, int ticketStart, int ticketCount, String contractAddress, String sig, String msg) {
        this.price = price;
        this.expiry = expiry;
        this.ticketStart = ticketStart;
        this.ticketCount = ticketCount;

        this.tickets = new int[ticketCount];
        for (int i = 0; i < ticketCount; i++)
        {
            this.tickets[i] = ticketStart;
            ticketStart++;
        }

        this.contractAddress = contractAddress;
        this.signature = Base64.decode(sig, Base64.DEFAULT);
        this.message = Base64.decode(msg, Base64.DEFAULT);
    }

    private MarketInstance(Parcel in) {
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
    }

    public static final Creator<MarketInstance> CREATOR = new Creator<MarketInstance>() {
        @Override
        public MarketInstance createFromParcel(Parcel in) {
            return new MarketInstance(in);
        }

        @Override
        public MarketInstance[] newArray(int size) {
            return new MarketInstance[size];
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
    }
}
