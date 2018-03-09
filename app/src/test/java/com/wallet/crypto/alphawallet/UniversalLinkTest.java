package com.wallet.crypto.alphawallet;

import com.wallet.crypto.alphawallet.entity.SalesOrder;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.math.BigInteger;
import java.util.Date;

/**
 * Created by weiwu on 9/3/18.
 */

/**
 * Universal link format (so that we can reform the ticket range to be imported BEFORE we import - this makes a good UI.
 *
 * bytes32: price Wei
 * bytes32: expiry
 * bytes20: contract address
 * Uint16[]: ticket indices
 * int32: ticket ID start value   <---- Added for UI ticket preview
 * byte: v
 * bytes32: r
 * bytes32: s
 *
 */


public class UniversalLinkTest {
    String link = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKQApQAAUIUqPzGusqG4jDWy4WUbXPs3vTtZgZnJungxzz7TmzwMSSi/VfTEW0dZduwWdwYqvVR4c2dD9TppFl4JPHQ5mJwxHA==";
    int[] indices = new int[] { 0xa4, 0xa5 };
    boolean verifySignature(byte[] message, byte[] signature) {
        return false;
    }
    @Test
    public void UniversalLinkShouldBeParsedCorrectly() {
        SalesOrder order = SalesOrder.parseUniversalLink(link);
        assertEquals(new BigInteger("0", 16), order.priceWei );
        assertEquals(0x0, order.expiry);
        assertEquals("0xbc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4", order.contractAddress);
        assertArrayEquals(indices, order.tickets);
        assertTrue(verifySignature(order.message, order.signature));
    }

    @Test
    public void UniversalLinkShouldBeGeneratedCorrectly() {
        //SalesOrder order = SalesOrder(......)
    }

}
