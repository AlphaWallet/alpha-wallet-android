package io.awallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import io.awallet.crypto.alphawallet.repository.TokenRepository;
import io.awallet.crypto.alphawallet.service.MarketQueueService;

import org.spongycastle.util.encoders.Base64;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static io.awallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

/**
 * Created by James on 21/02/2018.
 */

public class SalesOrder implements Parcelable {
    public final long expiry;
    public final double price;
    public final static BigInteger maxPrice = Convert.toWei(BigDecimal.valueOf(0xFFFFFFFFL),
            Convert.Unit.SZABO).toBigInteger();
    public final BigInteger priceWei;
    public final int[] tickets;
    public int ticketStart;
    public final int ticketCount;
    public final String contractAddress;
    public final byte[] signature = new byte[65];
    public final byte[] message;
    public TokenInfo tokenInfo; //convenience pointer to token information
    public String ownerAddress; //convenience ecrecovered owner address;
    public List<BigInteger> balanceInfo = null; // received balance from blockchain check

    public SalesOrder(double price, long expiry, int ticketStart, int ticketCount, String contractAddress, String sig, String msg)
            throws SalesOrderMalformed
    {
        this.message = Base64.decode(msg);
        this.price = price;
        this.expiry = expiry;
        this.ticketStart = ticketStart;
        this.ticketCount = ticketCount;
        this.contractAddress = contractAddress;
        MessageData data = readByteMessage(message, Base64.decode(sig), ticketCount);
        this.priceWei = data.priceWei;
        this.tickets = data.tickets;
        System.arraycopy(data.signature, 0, this.signature, 0, 65);
    }

    public static MessageData readByteMessage(byte[] message, byte[] sig, int ticketCount) throws SalesOrderMalformed
    {
        MessageData data = new MessageData();
        ByteArrayInputStream bas = new ByteArrayInputStream(message);
        try {
            EthereumReadBuffer ds = new EthereumReadBuffer(bas);
            data.priceWei = ds.readBI();
            ds.readBI();
            ds.readAddress();
            data.tickets = new int[ticketCount];
            ds.readUnsignedShort(data.tickets);
            System.arraycopy(sig, 0, data.signature, 0, 65);
            ds.close();
        }
        catch(IOException e) {
            throw new SalesOrderMalformed();
        }
        return data;
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
        final String importTemplate = "https://app.awallet.io/";
        int offset = link.indexOf(importTemplate);
        if (offset > -1)
        {
            offset += importTemplate.length();
            String linkData = link.substring(offset);
            return new SalesOrder(linkData);
        }
        else
        {
            throw new SalesOrderMalformed("Invalid link format");
        }
    }

    /**
     * Generates the first part of a Universal Link transfer message. Contains:
     * 4 Byte Micro Eth value ("Szabo")
     * 4 byte Unsigned expiry value
     * 20 byte address
     * variable length compressed indices (1 byte for 0-127, 2 bytes for 128-32767)
     *
     * @param ticketSendIndexList list of ticket indices
     * @param contractAddress Contract Address
     * @param priceWei Price of bundle in Wei
     * @param expiry Unsigned UNIX timestamp of offer expiry
     * @return First part of Universal Link (requires signature of trade bytes to be added)
     */
    public static byte[] generateLeadingLinkBytes(int[] ticketSendIndexList, String contractAddress, BigInteger priceWei, long expiry) throws SalesOrderMalformed
    {
        try
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            EthereumWriteBuffer wb = new EthereumWriteBuffer(buffer);

            if (priceWei.compareTo(maxPrice) > 0) {
                throw new SalesOrderMalformed("Order's price too high to be used in a link");
            }
            wb.write4ByteMicroEth(priceWei);
            wb.writeUnsigned4(expiry);
            wb.writeAddress(contractAddress);
            wb.writeCompressedIndices(ticketSendIndexList);

            wb.flush();
            wb.close();

            return buffer.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    protected SalesOrder(String linkData) throws SalesOrderMalformed {
        byte[] fullOrder = Base64.decode(linkData);
        long szabo;
        //read the order
        try {
            ByteArrayInputStream bas = new ByteArrayInputStream(fullOrder);
            EthereumReadBuffer ds = new EthereumReadBuffer(bas);
            szabo = ds.toUnsignedLong(ds.readInt());
            expiry = ds.toUnsignedLong(ds.readInt());
            priceWei = Convert.toWei(BigDecimal.valueOf(szabo), Convert.Unit.SZABO).toBigInteger();
            contractAddress = ds.readAddress();
            //ticketCount = ds.available() / 2;
            tickets = ds.readCompressedIndices(ds.available() - 65);
            ticketCount = tickets.length;
            //now read signature
            ds.readSignature(signature);
            ds.close();
        } catch (IOException e) {
            throw new SalesOrderMalformed();
        } catch (StringIndexOutOfBoundsException f) {
            throw new SalesOrderMalformed();
        } catch (Exception e) {
            throw new SalesOrderMalformed();
        }

        //now we have to build the message that the contract is expecting the signature for
        message = getTradeBytes();

        BigInteger milliWei = Convert.fromWei(priceWei.toString(), Convert.Unit.FINNEY).toBigInteger();
        price = milliWei.doubleValue() / 1000.0;
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
        ownerAddress = "0x";
        try {
            Sign.SignatureData sigData = MarketQueueService.sigFromByteArray(signature);
            BigInteger recoveredKey = Sign.signedMessageToKey(getTradeBytes(), sigData);
            ownerAddress += Keys.getAddress(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return ownerAddress;
    }

    private byte[] getTradeBytes()
    {
        return getTradeBytes(tickets, contractAddress, priceWei, expiry);
    }

    public boolean balanceChange(List<BigInteger> balance)
    {
        //compare two balances
        //quick return, if sizes are different there's a change
        if (balanceInfo == null)
        {
            balanceInfo = new ArrayList<>(); //initialise the balance list
            return true;
        }
        if (balance.size() != balanceInfo.size()) return true;

        List<BigInteger> oldBalance = new ArrayList<>(balanceInfo);
        List<BigInteger> newBalance = new ArrayList<>(balance);

        oldBalance.removeAll(balanceInfo);
        newBalance.removeAll(balance);

        return (oldBalance.size() != 0 || newBalance.size() != 0);
    }

    public static byte[] getTradeBytes(int[] ticketSendIndexList, String contractAddress, BigInteger priceWei, long expiry)
    {
        try {
            //form the transaction we need to push to buy
            //trade bytes
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream ds = new DataOutputStream(buffer);

            UnsignedLong expiryUL = UnsignedLong.create(expiry);

            BigInteger addrBI = new BigInteger(Numeric.cleanHexPrefix(contractAddress), 16);
            ds.write(Numeric.toBytesPadded(priceWei, 32));
            ds.write(Numeric.toBytesPadded(expiryUL, 32));
            ds.write(Numeric.toBytesPadded(addrBI, 20));

            byte[] uint16 = new byte[2];
            for (int i : ticketSendIndexList) {
                //write big endian encoding
                uint16[0] = (byte) (i >> 8);
                uint16[1] = (byte) (i & 0xFF);
                ds.write(uint16);
            }

            ds.flush();
            ds.close();

            return buffer.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getTradeBytes(int[] ticketSendIndexList, String contractAddress, String ethPrice, long expiry)
    {
        BigInteger wei = Convert.toWei(String.valueOf(ethPrice), Convert.Unit.FINNEY).toBigInteger();
        return getTradeBytes(ticketSendIndexList, contractAddress, wei, expiry);
    }

    public static String completeUniversalLink(byte[] message, byte[] signature) throws SalesOrderMalformed
    {
        byte[] completeLink = new byte[message.length + signature.length];
        System.arraycopy(message, 0, completeLink, 0, message.length);
        System.arraycopy(signature, 0, completeLink, message.length, signature.length);

        StringBuilder sb = new StringBuilder();

        sb.append("https://app.awallet.io/");
        byte[] b64 = Base64.encode(completeLink);
        sb.append(new String(b64));

        //this trade can be claimed by anyone who pushes the transaction through and has the sig
        return sb.toString();
    }

    public static byte[] signatureToByteArray(Sign.SignatureData signature) throws SalesOrderMalformed
    {
        byte[] sigBytes = new byte[65];
        try {
            System.arraycopy(signature.getR(), 0, sigBytes, 0, 32);     // r
            System.arraycopy(signature.getS(), 0, sigBytes, 32, 32);    // s
            sigBytes[64] = signature.getV(); // v
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

        return sigBytes;
    }

    public static String generateUniversalLink(int[] thisTickets, String contractAddr, BigInteger price, long expiry, byte[] signature) throws SalesOrderMalformed
    {
        byte[] leading = generateLeadingLinkBytes(thisTickets, contractAddr, price, expiry);
        return completeUniversalLink(leading, signature);
    }

    public boolean isValidOrder()
    {
        //check this order is not corrupt
        //first check the owner address - we should already have called getOwnerKey
        boolean isValid = true;

        if (this.ownerAddress == null || this.ownerAddress.length() < 20) isValid = false;
        if (this.contractAddress == null || this.contractAddress.length() < 20) isValid = false;
        if (this.message == null) isValid = false;

        return isValid;
    }
}
