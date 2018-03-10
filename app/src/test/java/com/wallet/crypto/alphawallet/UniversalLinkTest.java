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
 * Universal link format
 *
 * Android requires the link to be in the form:
 *
 * https://www.awallet.io/import?
 *
 * The format forbids using a prefix other than 'www'.
 * There needs to be text in the specific link too, in this case 'import'.
 *
 * AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKcAqA==;
 * 1b;
 * 2F982B84C635967A9B6306ED5789A7C1919164171E37DCCDF4B59BE547544105;
 * 30818B896B7D240F56C59EBDF209062EE54DA7A3590905739674DCFDCECF3E9B
 *
 * Base64 message: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKcAqA==
 * bytes32: price Wei
 * bytes32: expiry
 * bytes20: contract address
 * Uint16[]: ticket indices
 *
 * byte: 1b
 * bytes32: 2F982B84C635967A9B6306ED5789A7C1919164171E37DCCDF4B59BE547544105
 * bytes32: 30818B896B7D240F56C59EBDF209062EE54DA7A3590905739674DCFDCECF3E9B
 *
 */

public class UniversalLinkTest {
    String link = "https://www.awallet.io/import?AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKcAqA==;1b;2F982B84C635967A9B6306ED5789A7C1919164171E37DCCDF4B59BE547544105;30818B896B7D240F56C59EBDF209062EE54DA7A3590905739674DCFDCECF3E9B";
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
