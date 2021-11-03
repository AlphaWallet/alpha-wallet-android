package com.alphawallet.token.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alphawallet.token.entity.*;

import static com.alphawallet.token.entity.MagicLinkInfo.mainnetMagicLinkDomain;

/**
 * Created by James on 21/02/2018.
 */

public class ParseMagicLink
{
    private final static BigInteger maxPrice = Convert.toWei(BigDecimal.valueOf(0xFFFFFFFFL),
            Convert.Unit.SZABO).toBigInteger();

    //link formats
    public static final byte unassigned = 0x00;
    public static final byte normal = 0x01;
    public static final byte spawnable = 0x02;
    public static final byte customizable = 0x03;
    public static final byte currencyLink = 0x04;

    private static final String CURRENCY_LINK_PREFIX = "XDAIDROP";
    private CryptoFunctionsInterface cryptoInterface;

    private Map<Long, ChainSpec> extraChains;

    public ParseMagicLink(CryptoFunctionsInterface cryptInf, List<ChainSpec> chains)
    {
        cryptoInterface = cryptInf;
        if (chains != null)
        {
            extraChains = new HashMap<>();
            for (ChainSpec cs : chains) extraChains.put(cs.chainId, cs);
        }
    }

    public void addChain(ChainSpec chain)
    {
        if (extraChains == null) extraChains = new HashMap<>();
        extraChains.put(chain.chainId, chain);
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
        long chainId = MagicLinkInfo.identifyChainId(link);
        String magicLinkUrlPrefix = MagicLinkInfo.getMagicLinkDomainFromNetworkId(chainId);
        if (chainId == 0 && extraChains != null)
        {
            chainId = identifyChain(link);
            if (chainId > 0) magicLinkUrlPrefix = extraChains.get(chainId).urlPrefix;
        }

        if (magicLinkUrlPrefix == null)
        {
            throw new SalesOrderMalformed("Invalid link format");
        }

        int offset = link.indexOf(magicLinkUrlPrefix);
        if (offset > -1)
        {
            offset += magicLinkUrlPrefix.length() + 1;
            String linkData = link.substring(offset);
            return getMagicLinkDataFromURL(linkData, chainId);
        }
        else
        {
            throw new SalesOrderMalformed("Invalid link format");
        }
    }

    private long identifyChain(String link)
    {
        int dSlash = link.indexOf("://");
        long chainId = 0;
        //split out the chainId from the magiclink
        int index = link.indexOf(mainnetMagicLinkDomain);

        if (index > 0 && dSlash > 0)
        {
            String domain = link.substring(dSlash+3, index + mainnetMagicLinkDomain.length());
            for (ChainSpec cs : extraChains.values())
            {
                int prefix = link.indexOf(cs.urlPrefix);
                if (prefix > 0)
                {
                    chainId = cs.chainId;
                    break;
                }
            }
        }

        return chainId;
    }

    private MagicLinkData getDataFromLinks(MagicLinkData data, EthereumReadBuffer ds) throws IOException
    {
        long szabo = ds.toUnsignedLong(ds.readInt());
        data.expiry = ds.toUnsignedLong(ds.readInt());
        data.priceWei = Convert.toWei(BigDecimal.valueOf(szabo), Convert.Unit.SZABO).toBigInteger();
        data.contractAddress = ds.readAddress();
        switch (data.contractType)
        {
            case spawnable:
                data.tokenIds = ds.readTokenIdsFromSpawnableLink(ds.available() - 65);
                data.ticketCount = data.tokenIds.size();
                break;
            default:
                data.indices = ds.readCompressedIndices(ds.available() - 65);
                data.ticketCount = data.indices.length;
                break;
        }

        //now read signature
        ds.readSignature(data.signature);
        ds.close();
        //now we have to build the message that the contract is expecting the signature for
        data.message = getTradeBytes(data);
        BigInteger microEth = Convert.fromWei(new BigDecimal(data.priceWei), Convert.Unit.SZABO).abs().toBigInteger();
        data.price = microEth.doubleValue() / 1000000.0;
        return data;
    }

    //Note: currency links handle the unit in szabo directly, no need to parse to wei or vice versa
    private MagicLinkData parseCurrencyLinks(MagicLinkData data, EthereumReadBuffer ds) throws IOException
    {
        data.prefix = ds.readBytes(8);
        data.nonce = ds.readBI(4);
        data.amount = ds.readBI(4);
        data.expiry = ds.toUnsignedLong(ds.readInt());
        data.contractAddress = ds.readAddress();
        data.priceWei = BigInteger.ZERO;
        data.price = 0;
        ds.readSignature(data.signature);
        ds.close();
        //now we have to build the message that the contract is expecting the signature for
        data.message = getTradeBytes(data);
        return data;
    }

    private MagicLinkData getMagicLinkDataFromURL(String linkData, long chainId) throws SalesOrderMalformed
    {
        MagicLinkData data = new MagicLinkData();
        data.chainId = chainId;

        try
        {
            byte[] fullOrder = cryptoInterface.Base64Decode(linkData);
            ByteArrayInputStream bas = new ByteArrayInputStream(fullOrder);
            EthereumReadBuffer ds = new EthereumReadBuffer(bas);
            data.contractType = ds.readByte();

            switch (data.contractType)
            {
                case unassigned:
                    ds.reset();
                    //drop through
                case normal:
                case spawnable:
                case customizable:
                    return getDataFromLinks(data, ds);
                case currencyLink:
                    return parseCurrencyLinks(data, ds);
                default:
                    return getDataFromLinks(data, ds);
            }
        } catch (Exception e) {
            data.chainId = 0;
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
            BigInteger recoveredKey = cryptoInterface.signedMessageToKey(data.message, data.signature);
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
        switch (data.contractType)
        {
            case unassigned:
            case normal:
            case customizable:
                return getTradeBytes(data.indices, data.contractAddress, data.priceWei, data.expiry);
            case spawnable:
                return getSpawnableBytes(data.tokenIds, data.contractAddress, data.priceWei, data.expiry);
            case currencyLink:
                return getCurrencyBytes(data.contractAddress, data.amount, data.expiry, data.nonce.longValue());//data.formCurrencyDropLinkMessage();
            default:
                return getTradeBytes(data.indices, data.contractAddress, data.priceWei, data.expiry);
        }
    }

    public byte[] getSpawnableBytes(List<BigInteger> tokenIds, String contractAddress, BigInteger priceWei, long expiry)
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

            for (BigInteger tokenId : tokenIds)
            {
                ds.write(Numeric.toBytesPadded(tokenId, 32));
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
            List<BigInteger> tokenIds,
            String contractAddress,
            BigInteger priceWei,
            long expiry
    ) throws SalesOrderMalformed
    {
        try
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            EthereumWriteBuffer wb = new EthereumWriteBuffer(buffer);

            wb.writeByte(type);

            if (priceWei.compareTo(maxPrice) > 0) {
                throw new SalesOrderMalformed("Order's price too high to be used in a link");
            }
            wb.write4ByteMicroEth(priceWei);
            wb.writeUnsigned4(expiry);
            wb.writeAddress(contractAddress);
            switch (type)
            {
                case spawnable:
                    wb.writeTokenIds(tokenIds);
                    break;
                default:
                    wb.writeCompressedIndices(ticketSendIndexList);
                    break;
            }

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

    public byte[] getCurrencyBytes(String contractAddress, BigInteger szaboAmount, long expiry, long nonce)
    {
        try
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            EthereumWriteBuffer wb = new EthereumWriteBuffer(buffer);

            wb.write(CURRENCY_LINK_PREFIX.getBytes());
            wb.writeUnsigned4(nonce);
            wb.writeUnsigned4(szaboAmount);
            wb.writeUnsigned4(expiry);
            wb.writeAddress(contractAddress);
            wb.flush();
            wb.close();
            return buffer.toByteArray();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] generateCurrencyLink(byte[] currencyBytes)
    {
        try
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            EthereumWriteBuffer wb = new EthereumWriteBuffer(buffer);
            wb.writeByte(currencyLink);
            wb.write(currencyBytes);
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
        return generateLeadingLinkBytes(normal, ticketSendIndexList, null, contractAddress, priceWei, expiry);
    }

    public static byte[] generateSpawnableLeadingLinkBytes(List<BigInteger> tokenIds, String contractAddress, BigInteger priceWei, long expiry) throws SalesOrderMalformed
    {
        return generateLeadingLinkBytes(spawnable, null, tokenIds, contractAddress, priceWei, expiry);
    }

    public String generateUniversalLink(int[] thisTickets, String contractAddr, BigInteger price, long expiry, byte[] signature, long chainId) throws SalesOrderMalformed
    {
        byte[] leading = generateLeadingLinkBytes(thisTickets, contractAddr, price, expiry);
        return completeUniversalLink(chainId, leading, signature);
    }

    public String completeUniversalLink(long chainId, byte[] message, byte[] signature)
    {
        byte[] completeLink = new byte[message.length + signature.length];
        System.arraycopy(message, 0, completeLink, 0, message.length);
        System.arraycopy(signature, 0, completeLink, message.length, signature.length);
        String magiclinkPrefix;
        if (extraChains != null && extraChains.containsKey(chainId))
        {
            magiclinkPrefix = extraChains.get(chainId).urlPrefix;
        }
        else
        {
            magiclinkPrefix = MagicLinkInfo.generatePrefix(chainId);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(magiclinkPrefix);
        byte[] b64 = cryptoInterface.Base64Encode(completeLink);
        sb.append(new String(b64));
        //this trade can be claimed by anyone who pushes the transaction through and has the sig
        return sb.toString();
    }
}
