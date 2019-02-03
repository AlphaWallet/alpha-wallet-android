package io.stormbird.token.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import io.stormbird.token.entity.CryptoFunctionsInterface;
import io.stormbird.token.entity.EthereumReadBuffer;
import io.stormbird.token.entity.EthereumWriteBuffer;
import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.entity.MessageData;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.entity.UnsignedLong;

/**
 * Created by James on 21/02/2018.
 */

public class ParseMagicLink
{
    private final static BigInteger maxPrice = Convert.toWei(BigDecimal.valueOf(0xFFFFFFFFL),
            Convert.Unit.SZABO).toBigInteger();
    //link formats
    private static final byte unassigned = 0x00;
    private static final byte normal = 0x01;
    private static final byte spawnable = 0x02;
    private static final byte customizable = 0x03;
    private static final byte currencyLink = 0x04;

    private CryptoFunctionsInterface cryptoInterface;

    public ParseMagicLink()
    {

    }

    public ParseMagicLink(CryptoFunctionsInterface cryptInf)
    {
        cryptoInterface = cryptInf;
    }

    public void setCryptoInterface(CryptoFunctionsInterface cryptInf)
    {
        cryptoInterface = cryptInf;
    }

    public MessageData readByteMessage(byte[] message, byte[] sig, int ticketCount) throws SalesOrderMalformed
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
    public MagicLinkData parseUniversalLink(String link) throws SalesOrderMalformed
    {
        final String importTemplate = "https://app.awallet.io/";
        int offset = link.indexOf(importTemplate);
        if (offset > -1)
        {
            offset += importTemplate.length();
            String linkData = link.substring(offset);
            return getMagicLinkDataFromURL(linkData);
        }
        else if (link.length() > 60)
        {
            return getMagicLinkDataFromURL(link);
        }
        else
        {
            throw new SalesOrderMalformed("Invalid link format");
        }
    }

    private MagicLinkData parseNonSpawnableLinks(String linkData, boolean encoded) throws IOException
    {
        long szabo = 0;
        byte[] orderBytes = cryptoInterface.Base64Decode(linkData);
        //if link is encoded, remove the encoding byte
        if(encoded) orderBytes = Arrays.copyOfRange(orderBytes, 1, orderBytes.length - 1);
        MagicLinkData data = new MagicLinkData();
        ByteArrayInputStream bas = new ByteArrayInputStream(orderBytes);
        EthereumReadBuffer ds = new EthereumReadBuffer(bas);
        szabo = ds.toUnsignedLong(ds.readInt());
        data.expiry = ds.toUnsignedLong(ds.readInt());
        data.priceWei = Convert.toWei(BigDecimal.valueOf(szabo), Convert.Unit.SZABO).toBigInteger();
        data.contractAddress = ds.readAddress();
        data.tickets = ds.readCompressedIndices(ds.available() - 65);
        data.ticketCount = data.tickets.length;
        //now read signature
        ds.readSignature(data.signature);
        ds.close();
        //now we have to build the message that the contract is expecting the signature for
        data.message = getTradeBytes(data);
        BigInteger microEth = Convert.fromWei(new BigDecimal(data.priceWei), Convert.Unit.SZABO).abs().toBigInteger();
        data.price = microEth.doubleValue() / 1000000.0;
        return data;
    }

    private MagicLinkData parseSpawnableLinks(String linkData) throws IOException
    {
        long szabo = 0;
        byte[] orderBytes = cryptoInterface.Base64Decode(linkData);
        //remove encoding byte
        orderBytes = Arrays.copyOfRange(orderBytes, 1, orderBytes.length - 1);
        MagicLinkData data = new MagicLinkData();
        ByteArrayInputStream bas = new ByteArrayInputStream(orderBytes);
        EthereumReadBuffer ds = new EthereumReadBuffer(bas);
        szabo = ds.toUnsignedLong(ds.readInt());
        data.expiry = ds.toUnsignedLong(ds.readInt());
        data.priceWei = Convert.toWei(BigDecimal.valueOf(szabo), Convert.Unit.SZABO).toBigInteger();
        data.contractAddress = ds.readAddress();
        data.tokenIds = ds.readTokenIdsFromSpawnableLink(orderBytes);
        data.ticketCount = data.tokenIds.size();
        //now read signature
        ds.readSignature(data.signature);
        ds.close();
        //now we have to build the message that the contract is expecting the signature for
        data.message = getTradeBytes(data);
        BigInteger microEth = Convert.fromWei(new BigDecimal(data.priceWei), Convert.Unit.SZABO).abs().toBigInteger();
        data.price = microEth.doubleValue() / 1000000.0;

        return data;
    }

    private byte[] getMessageFromCurrencyDropLink(
            byte[] prefix,
            byte[] nonce,
            byte[] amount,
            byte[] expiry,
            byte[] contractAddress
    )
    {
        ByteBuffer message = ByteBuffer.allocate(40);
        message.put(prefix);
        message.put(nonce);
        message.put(amount);
        message.put(expiry);
        message.put(contractAddress);
        return message.array();
    }

    //Note: currency links handle the unit in szabo directly, no need to parse to wei or vice versa
    private MagicLinkData parseCurrencyLinks(String linkData) throws IOException
    {
        byte[] orderBytes = cryptoInterface.Base64Decode(linkData);
        //remove encoding byte
        orderBytes = Arrays.copyOfRange(orderBytes, 1, orderBytes.length - 1);
        MagicLinkData data = new MagicLinkData();
        ByteArrayInputStream bas = new ByteArrayInputStream(orderBytes);
        EthereumReadBuffer ds = new EthereumReadBuffer(bas);
        byte[] nonceBytes = Arrays.copyOfRange(orderBytes, 8, 11);
        byte[] amountBytes = Arrays.copyOfRange(orderBytes, 12, 15);
        byte[] contractAddressBytes = Arrays.copyOfRange(orderBytes, 20, 39);
        byte[] expiryBytes = Arrays.copyOfRange(orderBytes, 16, 19);
        data.prefix = Arrays.copyOfRange(orderBytes, 0, 7);
        data.nonce = new BigInteger(nonceBytes);
        data.amount = new BigInteger(amountBytes);
        data.contractAddress = new BigInteger(contractAddressBytes).toString(16);
        data.expiry = new BigInteger(expiryBytes).longValue();
        byte v = orderBytes[104];
        byte[] r = Arrays.copyOfRange(orderBytes, 72, 103);
        byte[] s = Arrays.copyOfRange(orderBytes, 40, 71);
        data.signature = getSignatureFromComponents(v, r, s);
        //now read signature
        ds.readSignature(data.signature);
        ds.close();
        //now we have to build the message that the contract is expecting the signature for
        data.message = getMessageFromCurrencyDropLink(
                data.prefix,
                nonceBytes,
                amountBytes,
                expiryBytes,
                contractAddressBytes
        );
        return data;
    }

    //TODO make sure this isn't duplicate
    private byte[] getSignatureFromComponents(byte v, byte[] r, byte[] s)
    {
        byte[] signature = new byte[65];
        System.arraycopy(s, 0, signature, 0, 32);
        System.arraycopy(r, 0, signature, 32, 32);
        signature[64] = v;
        return signature;
    }

    private MagicLinkData getMagicLinkDataFromURL(String linkData) throws SalesOrderMalformed {
        byte[] fullOrder = cryptoInterface.Base64Decode(linkData);
        MagicLinkData data = new MagicLinkData();
        try
        {
            ByteArrayInputStream bas = new ByteArrayInputStream(fullOrder);
            EthereumReadBuffer ds = new EthereumReadBuffer(bas);
            data.contractType = ds.readByte();
            switch (data.contractType)
            {
                case unassigned:
                    return parseNonSpawnableLinks(linkData, false);
                case normal:
                    return parseNonSpawnableLinks(linkData, true);
                case spawnable:
                    return parseSpawnableLinks(linkData);
                case customizable:
                    //Not yet implemented so default to spawnable
                    return parseSpawnableLinks(linkData);
                case currencyLink:
                    return parseCurrencyLinks(linkData);
                default:
                    return parseNonSpawnableLinks(linkData, false);
            }
        } catch (Exception e) {
            throw new SalesOrderMalformed();
        }
    }

    /**
     * ECRecover the owner address from a sales order
     *
     * @return string address of the owner
     */
    public String getOwnerKey(MagicLinkData data) {
        data.ownerAddress = "0x";
        try {
            BigInteger recoveredKey = cryptoInterface.signedMessageToKey(getTradeBytes(data), data.signature);
            data.ownerAddress += cryptoInterface.getAddressFromKey(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return data.ownerAddress;
    }

    private byte[] getTradeBytes(MagicLinkData data)
    {
        return getTradeBytes(data.tickets, data.contractAddress, data.priceWei, data.expiry);
    }

    public byte[] getTradeBytes(int[] ticketSendIndexList, String contractAddress, BigInteger priceWei, long expiry)
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

    private static byte[] generateLeadingLinkBytes(
            byte type,
            int[] ticketSendIndexList,
            String contractAddress,
            BigInteger priceWei,
            long expiry
    ) throws SalesOrderMalformed
    {
        try
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            EthereumWriteBuffer wb = new EthereumWriteBuffer(buffer);

            if (type > 0)
            {
                wb.writeByte(type);
            }

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

    public static byte[] generateLeadingLinkBytes(int[] ticketSendIndexList, String contractAddress, BigInteger priceWei, long expiry) throws SalesOrderMalformed
    {
        return generateLeadingLinkBytes(unassigned, ticketSendIndexList, contractAddress, priceWei, expiry);
    }

    public String generateUniversalLink(int[] thisTickets, String contractAddr, BigInteger price, long expiry, byte[] signature) throws SalesOrderMalformed
    {
        byte[] leading = generateLeadingLinkBytes(thisTickets, contractAddr, price, expiry);
        return completeUniversalLink(leading, signature);
    }

    public String completeUniversalLink(byte[] message, byte[] signature) {
        byte[] completeLink = new byte[message.length + signature.length];
        System.arraycopy(message, 0, completeLink, 0, message.length);
        System.arraycopy(signature, 0, completeLink, message.length, signature.length);
        StringBuilder sb = new StringBuilder();
        sb.append("https://app.awallet.io/");
        byte[] b64 = cryptoInterface.Base64Encode(completeLink);
        sb.append(new String(b64));
        //this trade can be claimed by anyone who pushes the transaction through and has the sig
        return sb.toString();
    }

}
