package com.wallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import com.wallet.crypto.alphawallet.repository.TokenRepository;

import org.ethereum.geth.BigInt;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
        this.tickets = ds.readShortIndices(ticketCount);
        this.contractAddress = contractAddress;
        this.signature = Base64.decode(sig, Base64.DEFAULT);
        ds.close();
    }

    public SalesOrder(String universalLink) {
        byte[] message = Base64.decode(universalLink, Base64.DEFAULT);
        ByteArrayInputStream bas = new ByteArrayInputStream(message);
        EthereumReadBuffer ds = new EthereumReadBuffer(bas);
        priceWei = ds.readBI();
        expiry = ds.readBI().intValue();
        contractAddress = ds.readAddress();
        //calculate how many tickets there must be:
        final int sigPlusIdStart = 4 + 65;
        ticketCount = (ds.available() - sigPlusIdStart) / 2;
        //now read uint16's until we hit the delimiter
        tickets = ds.readShortIndices(ticketCount);
        ticketStart = ds.readInt32();
        signature = ds.readSignature();
        ds.close();

        //now write the message
        this.message = writeMessage();

        BigInteger milliWei = Convert.fromWei(priceWei.toString(), Convert.Unit.FINNEY).toBigInteger();
        price = milliWei.doubleValue() / 1000.0;
    }

    public static SalesOrder parseUniversalLink(String link)
    {
        return new SalesOrder(link);
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
}
