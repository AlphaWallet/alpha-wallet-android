package io.awallet.crypto.alphawallet;

import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.SalesOrderMalformed;

import static io.awallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by weiwu on 9/3/18.
 */

/**
 * Universal link format-9 is used here. format-10 is a special case of format-9
 */

public class UniversalLinkTest
{
    final String[] links = { "https://app.awallet.io/AAGGoFq1tAC8mhAmpLxvC6i75IbR0J2lcys55AECAwQFBgcICfENh9BG3IRgrrkXGuLWKddxeI/PpXzaZ/RdyUxbrKi4MSEHa8NMnKTyjVw7uODNrpcboqSWZfIrHCFoug/YGegb",
            "https://app.awallet.io/AAGGoFq1tAC8mhAmpLxvC6i75IbR0J2lcys55AECAwQFBgcICXeC1PKTiAd0583blGKYxYj1mWcRQ9GUjd1LpqRGtcaFlvRZe3w72BFRH0xgL6zIgSYxVufa7x7MDw3DShU19r8c",
            "https://app.awallet.io/AAGGoFq1tAC8mhAmpLxvC6i75IbR0J2lcys55AECAwQFBgcICV+rXdRsJeazdeXmisb9qLy2M2z0riLFiPbPQ0GpZUkZ3xNblCEdcv0KMm/GGts/hFrHr/MQbNMmYPBig+FwGl8b" };

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
    final String link = "https://app.awallet.io/AAGGoFq1tAC8mhAmpLxvC6i75IbR0J2lcys55AECAwQFBgcICS+YK4TGNZZ6m2MG7VeJp8GRkWQXHjfczfS1m+VHVEEFMIGLiWt9JA9WxZ698gkGLuVNp6NZCQVzlnTc/c7PPpsb";

    //NB tradeBytes is the exact bytes the ERC875 contract builds to check the valid order.
    //This is what we must sign. If we sign the order bytes the contract transaction will fail.
    //above link is incorrectly formed somehow. Signature is wrong.
    final String correct_link = "https://app.awallet.io/AAGGoFq1tAC8mhAmpLxvC6i75IbR0J2lcys55AECAwQFBgcICabLOHgxkgYbO7q7XCddXu1Mr4lJ6TQfstmjZA5uScM+INfL97NCAT5ltYhrYB6pNGYz9kmakQR4gRaq9jcryfoB";
    /* The entire message of that above order is:
    000000000000000000000000000000000000000000000000016345785d8a0000
    000000000000000000000000000000000000000000000000000000005ab5b400
    bc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4
    000100020003000400050006000700080009 */

    //roll a new key
    ECKeyPair testKey = ECKeyPair.create("Test Key".getBytes());

    @Test
    public void UniversalLinkShouldBeParsedCorrectly() throws SalesOrderMalformed, SignatureException {
        SalesOrder order = SalesOrder.parseUniversalLink(correct_link);
        assertEquals(PRICE, order.priceWei);
        assertEquals(EXPIRY, order.expiry);
        assertEquals(CONTRACT_ADDR, order.contractAddress.toLowerCase());
        assertArrayEquals(indices, order.tickets);

        byte[] tradeBytes = SalesOrder.getTradeBytes(order.tickets, CONTRACT_ADDR, order.priceWei, order.expiry);

        assertTrue(verifySignature(tradeBytes, order.signature));
        Sign.SignatureData signature = sigFromByteArray(order.signature);
        assertEquals(OWNER_PUB_KEY, Sign.signedMessageToKey(order.message, signature));
    }

    @Test
    public void UniversalLinksSignerAddressShouldBeRecoverable() throws SalesOrderMalformed, SignatureException {
        for (String link : links) {
            SalesOrder order = SalesOrder.parseUniversalLink(link);
            order.getOwnerKey();
            assertNotNull(order.contractAddress);
            assertNotNull(order.ownerAddress);
            assertEquals(OWNER_ADDR, order.ownerAddress.toLowerCase()); //created from 0x007
        }
    }

    @Test(expected = SalesOrderMalformed.class)
    public void BadLinksShouldThrow() throws SalesOrderMalformed {
        String link = "https://www.awallet.io/import?bad";
        SalesOrder order = SalesOrder.parseUniversalLink(link);
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
            { 2 },
            { 2, 3 },
            { 2, 3, 4, 56, 127, 345, 4563 }
    };

    //1. Test we can generate and recover a series of universal links
    @Test
    public void UniversalLinkShouldBeGeneratedCorrectly() {
        final int NUMBER_OF_TESTS = 100;

        List<OrderData> orders = new ArrayList<>();

        try {

            for (int i = 0; i < NUMBER_OF_TESTS; i++) {
                OrderData data = new OrderData();
                data.tickets = tickets[i % 3];
                data.count = data.tickets.length;
                double dPrice = 0.5 + (Math.random() * 3.0);
                data.price = getPriceInWei(dPrice);
                data.expiry = System.currentTimeMillis() / 1000 + 2000 + (int) (Math.random() * 20000);

                //NB: Trade bytes is what we send to ethereum contract
                byte[] tradeBytes = SalesOrder.getTradeBytes(data.tickets, CONTRACT_ADDR, data.price, data.expiry);
                byte[] signature = getSignature(tradeBytes);

                //finally generate link
                data.link = SalesOrder.generateUniversalLink(data.tickets, CONTRACT_ADDR, data.price, data.expiry, signature);

                orders.add(data);
            }

            String ownerAddress = "0x" + ecRecoverAddress();

            //now try to read all the links
            for (OrderData data : orders) {
                SalesOrder order = SalesOrder.parseUniversalLink(data.link);
                order.getOwnerKey();
                Sign.SignatureData signature = sigFromByteArray(order.signature);
                assertEquals(order.priceWei, data.price);
                assertEquals(order.expiry, data.expiry);
                assertEquals(order.tickets.length, data.count);
                assertEquals(order.ticketCount, data.count);
                assertArrayEquals(order.tickets, data.tickets);
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
        BigInteger weiValue = Convert.toWei(String.valueOf(milliEth), Convert.Unit.FINNEY).toBigInteger();

        return weiValue;
    }

    private byte[] getSignature(byte[] message) throws SalesOrderMalformed {
        Sign.SignatureData sigData = Sign.signMessage(message, testKey);

        byte[] sig = new byte[65];

        try {
            System.arraycopy(sigData.getR(), 0, sig, 0, 32);
            System.arraycopy(sigData.getS(), 0, sig, 32, 32);
            sig[64] = (byte) (int) sigData.getV();
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
