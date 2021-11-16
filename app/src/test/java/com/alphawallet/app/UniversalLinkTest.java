package com.alphawallet.app;


import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;


/**
 * Created by weiwu on 9/3/18.
 */

/**
 * Universal link format-9 is used here. format-10 is a special case of format-9
 */

public class UniversalLinkTest
{
    private static final ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), null);

    final String[] links = { "https://aw.app/AAAAAFroO8yg2x-t8XoYKvHWEk8mRcRZuarNIgwNDg9OYA205_-QZURILYlNp6astOo-RkQMSSefIzMWHKdjcGsc3kAaHfHYi7rrLTgmUfAMaQjFB_u8G0EbB8HewJwDAA==",
            "https://aw.app/AB6EgFroX2xm8IymiSAXpF2m-3kqjpRvy-PYZRQVFhcYAlMtOEau6TvoUT-lN5HoxjxlErC2T0LJ-1u4DmORCdoVs-UNTIL33W_OJ6jGJy2ocqEyWBmV-RiYPIzQlHq0mwE=",
            "https://aw.app/ABLEsFsIA6hOusrp6ZAfDlACatAh6lurgkAr9zc4OTo7SZscuiiYYTfr1VhZ2Kv6NhZqf4dHGhZC5bkclppyAXpnk6SL1teCB_DB-6VKoJZGJj5jZ1Axc1RQ5B2uWojAOgA=" };

    final String OWNER_ADDR     = "0x007bee82bdd9e866b2bd114780a47f2261c684e3";
    final BigInteger OWNER_PUB_KEY =  new BigInteger("47EAE0D3EEFBC60BD914F8C361C658A11746D04D9CB00DF14F2B6C8BE5C23014CFC3E36BDED38BD151A29576996CC41DDC7E038EE8DAE6CE02AEDE6B3E232CDA", 16);

    /**
     * these values give the key format, ie
     * 4 bytes - price in MicroEth (Szabo)
     * 4 bytes - expiry (Unix time stamp - unsigned, gives higher range)
     * 20 bytes - contract addr
     * variable length indicies, multiple of single byte with MSB specifying extension to
     *
     */
    final BigInteger PRICE      = Convert.toWei("0.1", Convert.Unit.ETHER).toBigInteger(); //0x000186A0 SZABO
    final long EXPIRY           = 0x5AB5B400;
    final String CONTRACT_ADDR  = "0xbc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4";
    final int[] indices         = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

    /* To generate the URL path on a Linux/OS X computer:
    $ echo -n https://www.awallet.io/;\
    echo 000186A0 5AB5B400 bc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4 010203040506070809 \
         bfd3cb4ede61f0ccb9e7b6f589c2aa18a17024886b6be486e4cf5ffa46b887d8\
         0xdaa357cbed5df0041082a57efc39018b205ad1bfb33a745b28fa7d53a306401a \
         1b | xxd -r -p | base64 -w 0
     */
    final String link = "https://aw.app/AAAAAFr-ylTBVepdO7lE0GIfuVnycUI0dQHIahcYeCym0Gr-SNCJ-y69sl54rkw5UNWxlKfpdgmyz3iWEbguAQa215Zzg4kiJ8mPLT8Yz3tSbDk7o_SpmrrRrnmfSQE=";

    //NB tradeBytes is the exact bytes the ERC875 contract builds to check the valid order.
    //This is what we must sign. If we sign the order bytes the contract transaction will fail.
    //above link is incorrectly formed somehow. Signature is wrong.
    final String correct_link = "https://aw.app/AAAD6FroYRBOusrp6ZAfDlACatAh6lurgkAr924oOHKrWrHlBwhDtjCJW8mdFWhcAB2aD_VXigLtQcr4UHROYOjloqnrWnqUXBbCHhG2PPQ2w72ggu5yN4rxrRCRAA==";
    /* The entire message of that above order is:
    000000000000000000000000000000000000000000000000016345785d8a0000
    000000000000000000000000000000000000000000000000000000005ab5b400
    bc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4
    000100020003000400050006000700080009 */

    //roll a new key
    ECKeyPair testKey = ECKeyPair.create("Test Key".getBytes());

    @Test
    public void UniversalLinksSignerAddressShouldBeRecoverable() throws SalesOrderMalformed {
        for (String link : links) {
            MagicLinkData order = parser.parseUniversalLink(link);
            parser.getOwnerKey(order);
            assertNotNull(order.contractAddress);
            assertNotNull(order.ownerAddress);
            assertEquals(OWNER_ADDR, order.ownerAddress.toLowerCase()); //created from 0x007
        }
    }

    @Test(expected = SalesOrderMalformed.class)
    public void BadLinksShouldThrow() throws SalesOrderMalformed {
        String link = "https://www.awallet.io/import?bad";
        MagicLinkData order = parser.parseUniversalLink(link);
    }

    private class OrderData
    {
        int count;
        BigInteger price;
        long expiry;
        int [] tickets;

        String link;
    }

    int[][] tickets = new int[][]{
            { 0 },
            { 2 },
            { 2, 3 },
            { 2, 3, 4, 56, 127, 345, 4563 },
            { 0, 1, 1099, 23 }
    };

    //1. Test we can generate and recover a series of universal links
    @Test
    public void UniversalLinkShouldBeGeneratedCorrectly() {
        final int NUMBER_OF_TESTS = 500;

        List<OrderData> orders = new ArrayList<>();

        try {

            for (int i = 0; i < NUMBER_OF_TESTS; i++) {
                OrderData data = new OrderData();
                data.tickets = tickets[i % tickets.length];
                data.count = data.tickets.length;
                double dPrice = 0.5 + (Math.random() * 3.0);
                data.price = getPriceInWei(dPrice);
                data.expiry = System.currentTimeMillis() / 1000 + 2000 + (int) (Math.random() * 20000);

                //NB: Trade bytes is what we send to ethereum contract
                byte[] tradeBytes = parser.getTradeBytes(data.tickets, CONTRACT_ADDR, data.price, data.expiry);
                byte[] signature = getSignature(tradeBytes);

                //finally generate link
                data.link = parser.generateUniversalLink(data.tickets, CONTRACT_ADDR, data.price, data.expiry, signature, 1);

                orders.add(data);
            }

            String ownerAddress = "0x" + ecRecoverAddress();

            //now try to read all the links
            for (OrderData data : orders) {
                MagicLinkData order = parser.parseUniversalLink(data.link);
                parser.getOwnerKey(order);
                Sign.SignatureData signature = sigFromByteArray(order.signature);
                assertEquals(order.priceWei, data.price);
                assertEquals(order.expiry, data.expiry);
                assertEquals(order.indices.length, data.count);
                assertEquals(order.ticketCount, data.count);
                assertArrayEquals(order.indices, data.tickets);
                assertNotNull(order.contractAddress);
                assertNotNull(order.ownerAddress);
                assertEquals(ownerAddress, order.ownerAddress.toLowerCase());
            }
        }
        catch (SalesOrderMalformed e) {
            assertTrue(e.getMessage(),false);
        }
        catch (Exception e) {
            assertTrue(e.getMessage(), false);
        }
    }

    private BigInteger getPriceInWei(double ethValue)
    {
        //now convert to milliWei
        int milliEth = (int)(ethValue*1000.0f);

        //now convert to ETH

        return Convert.toWei(String.valueOf(milliEth), Convert.Unit.FINNEY).toBigInteger();
    }

    private byte[] getSignature(byte[] message) throws SalesOrderMalformed {
        Sign.SignatureData sigData = Sign.signMessage(message, testKey);

        byte[] sig = new byte[65];

        try {
            System.arraycopy(sigData.getR(), 0, sig, 0, 32);
            System.arraycopy(sigData.getS(), 0, sig, 32, 32);
            System.arraycopy(sigData.getV(), 0, sig, 64, 1);
        } catch (IndexOutOfBoundsException e) {
            throw new SalesOrderMalformed("Signature shorter than expected 256");
        }

        return sig;
    }

    private String ecRecoverAddress(byte[] data, Sign.SignatureData signature) //get the hex string address from the sig and data
    {
        String address = "";
        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(data, signature); //get embedded address
            address = Keys.getAddress(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return address;
    }

    private boolean verifySignature(byte[] message, byte[] signature) {
        boolean pass = false;
        try {
            Sign.SignatureData sig = sigFromByteArray(signature);
            String address = ecRecoverAddress(message, sig);

            if (Numeric.cleanHexPrefix(address).equalsIgnoreCase(Numeric.cleanHexPrefix(OWNER_ADDR)))
            {
                pass = true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return pass;
    }

    private String ecRecoverAddress() throws Exception
    {
        String testSigMsg = "obtain public key";
        Sign.SignatureData testSig = Sign.signMessage(testSigMsg.getBytes(), testKey);
        BigInteger recoveredKey = Sign.signedMessageToKey(testSigMsg.getBytes(), testSig);

        return Keys.getAddress(recoveredKey);
    }
}
