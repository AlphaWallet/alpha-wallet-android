package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokenRepository;

import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.MessageData;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;

import static com.alphawallet.token.tools.ParseMagicLink.currencyLink;
import static com.alphawallet.token.tools.ParseMagicLink.spawnable;

import timber.log.Timber;

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
        magicLink.indices = data.tickets;
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
        magicLink.indices = new int[ticketLength];
        in.readIntArray(magicLink.indices);

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
        parcel.writeInt(magicLink.indices.length);
        parcel.writeIntArray(magicLink.indices);
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

    public static byte[] generateReverseTradeData(MagicLinkData order, Token token, String recipient)
    {
        byte[] data = null;
        try
        {
            List<BigInteger> tokenElements = new ArrayList<>();
            BigInteger expiry = BigInteger.valueOf(order.expiry);
            //convert to signature representation
            Sign.SignatureData sellerSig = CryptoFunctions.sigFromByteArray(order.signature);
            int v = (new BigInteger(sellerSig.getV())).intValue();

            switch (order.contractType)
            {
                case spawnable:
                    data = TokenRepository.createSpawnPassTo(token, expiry, order.tokenIds, v, sellerSig.getR(), sellerSig.getS(), recipient);
                    break;
                case currencyLink:
                    // for testing only, we would be using an intermediate server
                    data = TokenRepository.createDropCurrency(order, v, sellerSig.getR(), sellerSig.getS(), recipient);
                    break;
                default:
                    for (int ticketIndex : order.indices) {
                        tokenElements.add(BigInteger.valueOf(ticketIndex));
                    }
                    data = TokenRepository.createTrade(token, expiry, tokenElements, v, sellerSig.getR(), sellerSig.getS());
                    break;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);;
        }

        return data;
    }
}
