package io.awallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.awallet.crypto.alphawallet.repository.TokenRepository;
import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.entity.MessageData;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;

import static io.awallet.crypto.alphawallet.entity.CryptoFunctions.sigFromByteArray;

public class MagicLinkParcel implements Parcelable
{
    public MagicLinkData magicLink;

    public MagicLinkParcel(MagicLinkData data)
    {
        magicLink = data;
    }

    public MagicLinkParcel(double price, long expiry, int ticketStart, int ticketCount, String contractAddress, String sig, String msg, ParseMagicLink parser)
            throws SalesOrderMalformed
    {
        magicLink.message = Base64.decode(msg, Base64.URL_SAFE);
        magicLink.price = price;
        magicLink.expiry = expiry;
        magicLink.ticketStart = ticketStart;
        magicLink.ticketCount = ticketCount;
        magicLink.contractAddress = contractAddress;
        MessageData data = parser.readByteMessage(magicLink.message, Base64.decode(sig, Base64.URL_SAFE), ticketCount);
        magicLink.priceWei = data.priceWei;
        magicLink.tickets = data.tickets;
        System.arraycopy(data.signature, 0, magicLink.signature, 0, 65);
    }

    private MagicLinkParcel(Parcel in)
    {
        magicLink.expiry = in.readLong();
        magicLink.price = in.readDouble();
        magicLink.ticketStart = in.readInt();
        magicLink.ticketCount = in.readInt();
        magicLink.contractAddress = in.readString();
        int ticketLength = in.readInt();
        magicLink.tickets = new int[ticketLength];
        in.readIntArray(magicLink.tickets);

        int sigLength = in.readInt();   // must not be higher than 65 bytes
        in.readByteArray(magicLink.signature);    // in my guess, it's always is 65 bytes so it should fit.

        int messageLength = in.readInt();
        magicLink.message = new byte[messageLength];
        in.readByteArray(magicLink.message);
        magicLink.priceWei = new BigInteger(in.readString());
    }

    public static final Creator<MagicLinkParcel> CREATOR = new Creator<MagicLinkParcel>() {
        @Override
        public MagicLinkParcel createFromParcel(Parcel in) {
            return new MagicLinkParcel(in);
        }

        @Override
        public MagicLinkParcel[] newArray(int size) {
            return new MagicLinkParcel[size];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(magicLink.expiry);
        parcel.writeDouble(magicLink.price);
        parcel.writeInt(magicLink.ticketStart);
        parcel.writeInt(magicLink.ticketCount);
        parcel.writeString(magicLink.contractAddress);
        parcel.writeInt(magicLink.tickets.length);
        parcel.writeIntArray(magicLink.tickets);
        parcel.writeInt(magicLink.signature.length);
        parcel.writeByteArray(magicLink.signature);
        parcel.writeInt(magicLink.message.length);
        parcel.writeByteArray(magicLink.message);
        parcel.writeString(magicLink.priceWei.toString(10));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static byte[] generateReverseTradeData(MagicLinkData order)
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
}
