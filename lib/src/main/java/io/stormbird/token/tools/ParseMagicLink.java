package io.stormbird.token.tools;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
    public final static BigInteger maxPrice = Convert.toWei(BigDecimal.valueOf(0xFFFFFFFFL),
                                                            Convert.Unit.SZABO).toBigInteger();

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
            return createFromLink(linkData);
        }
        else if (link.length() > 60)
        {
            return createFromLink(link);
        }
        else
        {
            throw new SalesOrderMalformed("Invalid link format");
        }
    }

    private MagicLinkData createFromLink (String linkData) throws SalesOrderMalformed {
        byte[] fullOrder = cryptoInterface.Base64Decode(linkData);
        long szabo;

        MagicLinkData data = new MagicLinkData();
        //read the order
        try
        {
            ByteArrayInputStream bas = new ByteArrayInputStream(fullOrder);
            EthereumReadBuffer ds = new EthereumReadBuffer(bas);
            szabo = ds.toUnsignedLong(ds.readInt());
            data.expiry = ds.toUnsignedLong(ds.readInt());
            data.priceWei = Convert.toWei(BigDecimal.valueOf(szabo), Convert.Unit.SZABO).toBigInteger();
            data.contractAddress = ds.readAddress();
            //ticketCount = ds.available() / 2;
            data.tickets = ds.readCompressedIndices(ds.available() - 65);
            data.ticketCount = data.tickets.length;
            //now read signature
            ds.readSignature(data.signature);
            ds.close();
        } catch (IOException e) {
            throw new SalesOrderMalformed();
        } catch (StringIndexOutOfBoundsException f) {
            throw new SalesOrderMalformed();
        } catch (Exception e) {
            throw new SalesOrderMalformed();
        }

        //now we have to build the message that the contract is expecting the signature for
        data.message = getTradeBytes(data);

        BigInteger microEth = Convert.fromWei(new BigDecimal(data.priceWei), Convert.Unit.SZABO).abs().toBigInteger();
        data.price = microEth.doubleValue() / 1000000.0;

        return data;
    }

    /**
     * ECRecover the owner address from a sales order
     *
     * @return
     */
    public String getOwnerKey(MagicLinkData data) {
        data.ownerAddress = "0x";
        try {
            BigInteger recoveredKey = cryptoInterface.signedMessageToKey(getTradeBytes(data), data.signature);

            //Sign.SignatureData sigData = MarketQueueService.sigFromByteArray(signature);
            //BigInteger recoveredKey = Sign.signedMessageToKey(getTradeBytes(), sigData);

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

    public byte[] getTradeBytes(int[] ticketSendIndexList, String contractAddress, String ethPrice, long expiry)
    {
        BigInteger wei = Convert.toWei(String.valueOf(ethPrice), Convert.Unit.FINNEY).toBigInteger();
        return getTradeBytes(ticketSendIndexList, contractAddress, wei, expiry);
    }

    public String generateUniversalLink(int[] thisTickets, String contractAddr, BigInteger price, long expiry, byte[] signature) throws SalesOrderMalformed
    {
        byte[] leading = generateLeadingLinkBytes(thisTickets, contractAddr, price, expiry);
        return completeUniversalLink(leading, signature);
    }

    public String completeUniversalLink(byte[] message, byte[] signature) throws SalesOrderMalformed
    {
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

    public boolean isValidOrder(MagicLinkData data)
    {
        //check this order is not corrupt
        //first check the owner address - we should already have called getOwnerKey
        boolean isValid = true;

        if (data.ownerAddress == null || data.ownerAddress.length() < 20) isValid = false;
        if (data.contractAddress == null || data.contractAddress.length() < 20) isValid = false;
        if (data.message == null) isValid = false;

        return isValid;
    }
}
